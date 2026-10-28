<#
.SYNOPSIS
  Edital de teste E2E: grupos, fases, etapas, publicacões, desempate, cronograma.
  Exercita CREATE / UPDATE / DELETE e reporta 4xx/5xx.

.PARAMETER CriarNovo
  Cria edital + 2 cargos via modulo-dados (porta 8001) e usa o id gerado.
#>
param(
    [int] $EditalId = 0,
    [string] $Base = "http://localhost:8002/data/v1/idecan",
    [string] $DadosBase = "http://localhost:8001/data",
    [int] $ClienteId = 1,
    [switch] $CriarNovo,
    [switch] $Cleanup
)

$ErrorActionPreference = "Continue"
$script:Passou = 0
$script:Falhou = 0
$script:Erros = [System.Collections.Generic.List[string]]::new()
$script:Ctx = @{}

function Ok($nome) { $script:Passou++; Write-Host "  OK  $nome" -ForegroundColor Green }
function Fail($nome, $detalhe) {
    $script:Falhou++
    $msg = "$nome :: $detalhe"
    $script:Erros.Add($msg)
    Write-Host "  FAIL $msg" -ForegroundColor Red
}

function New-EditalDoZero {
    param([string] $DadosBase, [int] $ClienteId)
    $ts = Get-Date -Format "yyyyMMdd-HHmmss"
    $nome = "E2E FASES ETAPAS CRONOGRAMA $ts"
    $entidade = "ORGAO E2E TESTE"
    Write-Host "`n=== Criando edital do zero (modulo-dados) ===" -ForegroundColor Cyan
    $raw = curl.exe -s -w "`nHTTP:%{http_code}" -X POST "$DadosBase/editais/step1" `
        -F "clienteId=$ClienteId" -F "staId=3" `
        -F "ediEntidade=$entidade" -F "ediNomeConcurso=$nome"
    $lines = @($raw -split "`n")
    $code = (($lines | Where-Object { $_ -like "HTTP:*" } | Select-Object -Last 1) -replace "HTTP:", "")
    $body = (($lines | Where-Object { $_ -notlike "HTTP:*" }) -join "`n").Trim()
    if ($code -notin @("200", "201") -or -not $body) {
        Fail "POST editais/step1" "HTTP $code body=$body"
        exit 1
    }
    $ediId = [int]$body
    Ok "POST editais/step1 -> edital $ediId [$code]"

    $inicio = (Get-Date).AddDays(7).ToString("yyyy-MM-ddTHH:mm:ss")
    $fim = (Get-Date).AddDays(37).ToString("yyyy-MM-ddTHH:mm:ss")
    $raw2 = curl.exe -s -w "`nHTTP:%{http_code}" -X PUT "$DadosBase/editais/$ediId/step2" `
        -F "clienteId=$ClienteId" `
        -F "ediTexto=Edital E2E automatico de fases, etapas e cronograma." `
        -F "ediDtInicioInscricao=$inicio" -F "ediDtFimInscricao=$fim" `
        -F "ediDtInicioSolicitacaoAtendimentoEspecial=$inicio" `
        -F "ediDtFimSolicitacaoAtendimentoEspecial=$fim" `
        -F "ediDtInicioSolicitacaoIsencao=$inicio" `
        -F "ediDtFimSolicitacaoIsencao=$fim"
    $code2 = ((($raw2 -split "`n") | Where-Object { $_ -like "HTTP:*" } | Select-Object -Last 1) -replace "HTTP:", "")
    if ($code2 -notin @("200", "204")) { Fail "PUT editais/step2" "HTTP $code2" } else { Ok "PUT editais/step2 [$code2]" }

    # configs (alternativa ao step3, que pode 500 em alguns ambientes)
    try {
        $cfg = @{ isento = $true; ediAceitaCartao = $true; variasInscricoes = $false } | ConvertTo-Json -Compress
        $r = Invoke-WebRequest -Uri "$DadosBase/editais/$ediId/configuracoes?clienteId=$ClienteId" `
            -Method PATCH -ContentType "application/json" -Body $cfg -UseBasicParsing -TimeoutSec 30
        Ok "PATCH editais/configuracoes [$($r.StatusCode)]"
    } catch {
        Fail "PATCH editais/configuracoes" $_.Exception.Message
    }

    foreach ($desc in @("Agente Administrativo E2E $ts", "Assistente Administrativo E2E $ts")) {
        $cargoBody = @{
            clienteId = $ClienteId; ediId = $ediId; carDescricao = $desc
            carVlInscricao = 80.00; carVlSalarioBase = 3000.00
            carEscolaridade = "Ensino Medio Completo"; carComentario = "Cargo E2E"
            carPermitirInscricao = 1; carPermitirPagamento = 1; cargoDataHora = $null
        } | ConvertTo-Json -Compress
        try {
            $r = Invoke-WebRequest -Uri "$DadosBase/concurso/cargo" -Method POST `
                -ContentType "application/json" -Body $cargoBody -UseBasicParsing -TimeoutSec 30
            Ok "POST concurso/cargo $desc [$($r.StatusCode)] id=$($r.Content)"
        } catch {
            Fail "POST concurso/cargo" "$desc :: $($_.Exception.Message)"
            exit 1
        }
    }
    return $ediId
}

if ($CriarNovo -or $EditalId -le 0) {
    $EditalId = New-EditalDoZero -DadosBase $DadosBase -ClienteId $ClienteId
}

function Invoke-Api {
    param(
        [string] $Method,
        [string] $Path,
        [object] $Body = $null,
        [int[]] $Expect = @(200, 201, 204),
        [string] $Nome
    )
    $uri = if ($Path.StartsWith("http")) { $Path } else { "$Base$Path" }
    $params = @{
        Uri             = $uri
        Method          = $Method
        TimeoutSec      = 60
        UseBasicParsing = $true
    }
    if ($null -ne $Body) {
        $params.ContentType = "application/json; charset=utf-8"
        $params.Body = ($Body | ConvertTo-Json -Depth 10 -Compress)
    }
    try {
        $resp = Invoke-WebRequest @params
        $code = [int]$resp.StatusCode
        if ($Expect -notcontains $code) {
            Fail $Nome "HTTP $code (esperado $($Expect -join ',')) body=$($resp.Content.Substring(0, [Math]::Min(300, $resp.Content.Length)))"
            return $null
        }
        Ok "$Nome [$code]"
        if ([string]::IsNullOrWhiteSpace($resp.Content)) { return $null }
        try { return $resp.Content | ConvertFrom-Json } catch { return $resp.Content }
    } catch {
        $code = 0
        $detail = $_.Exception.Message
        if ($_.Exception.Response) {
            $code = [int]$_.Exception.Response.StatusCode
            try {
                $reader = [IO.StreamReader]::new($_.Exception.Response.GetResponseStream())
                $detail = $reader.ReadToEnd()
                $reader.Close()
            } catch {}
        }
        if ($Expect -contains $code) {
            Ok "$Nome [$code]"
            return $null
        }
        $snip = if ($detail.Length -gt 400) { $detail.Substring(0, 400) } else { $detail }
        Fail $Nome "HTTP $code $snip"
        return $null
    }
}

Write-Host "`n=== E2E edital $EditalId ===" -ForegroundColor Cyan
Write-Host "Base: $Base`n"

# --- Tipos ---
$tipos = Invoke-Api GET "/tipos/etapa" -Nome "GET tipos/etapa"
$pubs = Invoke-Api GET "/tipos/publicacao" -Nome "GET tipos/publicacao"
$tipoObj = $tipos | Where-Object { $_.valor -eq "prova_objetiva" } | Select-Object -First 1
$tipoDisc = $tipos | Where-Object { $_.valor -eq "prova_discursiva" } | Select-Object -First 1
$tipoRf = $tipos | Where-Object { $_.valor -eq "resultado" } | Select-Object -First 1
if (-not $tipoObj) { Fail "tipos" "prova_objetiva ausente"; exit 1 }

$pubIds = @()
if ($pubs) {
    $pubIds = @($pubs | Where-Object { $_.valor -in @("resultado_preliminar", "resultado_oficial") } | ForEach-Object { $_.id })
}

# --- Cargos ---
$cargos = Invoke-Api GET "/grupos-cargo/cargos-sem-grupo?editalId=$EditalId" -Nome "GET cargos-sem-grupo"
if (-not $cargos -or $cargos.Count -lt 2) {
    Fail "pre-requisito" "Edital $EditalId precisa de pelo menos 2 cargos sem grupo"
    exit 1
}
$cargoA = $cargos[0].id
$cargoB = $cargos[1].id
$script:Ctx.cargoA = $cargoA
$script:Ctx.cargoB = $cargoB

# --- Grupo CREATE / UPDATE ---
$sufixo = Get-Date -Format "HHmmss"
$grupo = Invoke-Api POST "/grupos-cargo?editalId=$EditalId" -Body @{ nome = "E2E Medio $sufixo" } -Expect @(201) -Nome "POST grupo"
if (-not $grupo) { exit 1 }
$grupoId = $grupo.id
$script:Ctx.grupoId = $grupoId

Invoke-Api PUT "/grupos-cargo/$grupoId" -Body @{ nome = "E2E Nivel Medio $sufixo" } -Nome "PUT grupo renomear" | Out-Null
Invoke-Api PUT "/grupos-cargo/$grupoId/cargos/$cargoA" -Nome "PUT cargo no grupo" | Out-Null
Invoke-Api PUT "/grupos-cargo/$grupoId/cargos/$cargoB" -Nome "PUT 2o cargo no grupo" | Out-Null
Invoke-Api GET "/grupos-cargo?editalId=$EditalId" -Nome "GET grupos do edital" | Out-Null

# --- Gerar fases (objetiva + discursiva) ---
$hoje = (Get-Date).ToString("yyyy-MM-dd")
$fim = (Get-Date).AddDays(30).ToString("yyyy-MM-dd")
$gerarBody = @{
    fases = @(
        @{
            nome            = "Prova objetiva"
            descricao       = $null
            dataAbertura    = "${hoje}T08:00:00-03:00"
            dataFechamento  = "${fim}T18:00:00-03:00"
            etapas          = @(
                @{ tipoId = $tipoObj.id; nome = $null; publicacoes = $pubIds }
            )
        },
        @{
            nome            = "Prova discursiva"
            descricao       = $null
            dataAbertura    = "${hoje}T08:00:00-03:00"
            dataFechamento  = "${fim}T18:00:00-03:00"
            etapas          = @(
                @{ tipoId = $tipoDisc.id; nome = $null; publicacoes = $pubIds }
            )
        }
    )
}
$geradas = Invoke-Api POST "/grupos-cargo/$grupoId/gerar-fases" -Body $gerarBody -Expect @(201) -Nome "POST gerar-fases"
if (-not $geradas -or $geradas.Count -lt 1) { Fail "gerar-fases" "sem fases"; exit 1 }

$faseObj = $geradas[0].fase
$faseDisc = if ($geradas.Count -gt 1) { $geradas[1].fase } else { $null }
$script:Ctx.faseObjId = $faseObj.id
$etapaObj = $geradas[0].etapas | Where-Object { $_.tipoId -eq $tipoObj.id } | Select-Object -First 1
$etapaRf = $geradas[0].etapas | Where-Object { $_.tipoId -eq $tipoRf.id } | Select-Object -First 1
$script:Ctx.etapaObjId = $etapaObj.id

# --- Fases CRUD ---
Invoke-Api GET "/fases?grupoCargoId=$grupoId" -Nome "GET fases do grupo" | Out-Null
Invoke-Api GET "/fases/$($faseObj.id)" -Nome "GET fase por id" | Out-Null
$faseUpd = Invoke-Api PUT "/fases/$($faseObj.id)" -Body @{
    nome           = "Prova objetiva E2E"
    descricao      = "Atualizada no teste"
    ordem          = $faseObj.ordem
    dataAbertura   = "${hoje}T08:00:00-03:00"
    dataFechamento = "${fim}T18:00:00-03:00"
} -Nome "PUT fase"

# --- Etapas CRUD ---
$etapas = Invoke-Api GET "/etapas?faseId=$($faseObj.id)" -Nome "GET etapas da fase"
if ($etapaObj) {
    # modalidade so vale para S05 (hetero/biopsico) — objetiva deve ir null
    Invoke-Api PUT "/etapas/$($etapaObj.id)" -Body @{
        nome                   = "Prova objetiva"
        tipoId                 = $tipoObj.id
        descricao              = "Etapa E2E"
        dataAbertura           = "${hoje}T08:00:00-03:00"
        dataFechamento         = "${fim}T18:00:00-03:00"
        statusId               = 1
        reprovavel             = $true
        obrigatoriaParaFase    = $true
        todosCandidatos        = $true
        aceitaDocumentos       = $false
        aceitaRecurso          = $true
        preliminar             = $true
        elegibilidadeEtapaId   = $null
        elegibilidadeStatusId  = $null
        responsavelId          = $null
        publicadaEm            = $null
        notaZeroElimina        = $null
        notaMinimaPercentual   = $null
        modalidade             = $null
    } -Nome "PUT etapa"
}

# criar etapa extra (recurso) se tipo existir
$tipoRecurso = $tipos | Where-Object { $_.valor -eq "recurso" } | Select-Object -First 1
$etapaExtraId = $null
if ($tipoRecurso) {
    $criada = Invoke-Api POST "/etapas?faseId=$($faseObj.id)" -Body @{
        nome                   = "Recurso E2E"
        tipoId                 = $tipoRecurso.id
        descricao              = $null
        dataAbertura           = "${hoje}T08:00:00-03:00"
        dataFechamento         = "${fim}T18:00:00-03:00"
        statusId               = $null
        reprovavel             = $false
        obrigatoriaParaFase    = $false
        todosCandidatos        = $true
        aceitaDocumentos       = $false
        aceitaRecurso          = $false
        preliminar             = $false
        elegibilidadeEtapaId   = $null
        elegibilidadeStatusId  = $null
        responsavelId          = $null
        publicadaEm            = $null
        notaZeroElimina        = $null
        notaMinimaPercentual   = $null
        modalidade             = $null
    } -Expect @(201) -Nome "POST etapa recurso"
    if ($criada) { $etapaExtraId = $criada.id }
}

# --- Publicacões ---
$publicacoes = Invoke-Api GET "/publicacoes?faseId=$($faseObj.id)" -Nome "GET publicacões"
if ($publicacoes -and $publicacoes.Count -gt 0) {
    $pub = $publicacoes[0]
    Invoke-Api PUT "/publicacoes/$($pub.id)" -Body @{
        dataPrevista         = "${hoje}T08:00:00-03:00"
        visualizacaoInicio   = "${hoje}T08:00:00-03:00"
        visualizacaoFim      = "${fim}T18:00:00-03:00"
        recursoInicio        = $null
        recursoFim           = $null
    } -Nome "PUT publicacao datas"
}

# --- Criterios de desempate CRUD ---
# Limpa criterios previos da fase (pode ser compartilhada entre grupos)
$existentes = Invoke-Api GET "/fases/$($faseObj.id)/criterios-desempate" -Nome "GET criterios-desempate (antes)"
foreach ($c in @($existentes)) {
    if ($c.id) {
        Invoke-Api DELETE "/criterios-desempate/$($c.id)" -Expect @(200, 204) -Nome "DELETE desempate antigo $($c.id)" | Out-Null
    }
}

$c1 = Invoke-Api POST "/fases/$($faseObj.id)/criterios-desempate" -Body @{
    label          = "Idade igual ou superior a 60 anos"
    tipo           = "idade"
    direcao        = "desc"
    condicao       = @{ operador = ">="; valor = 60; unidade = "anos" }
    dataReferencia = "ultimo_dia_inscricao"
    fonte          = $null
    baseLegal      = "idoso"
} -Expect @(201) -Nome "POST desempate idade"

$c2 = Invoke-Api POST "/fases/$($faseObj.id)/criterios-desempate" -Body @{
    label          = "Maior nota na prova objetiva"
    tipo           = "nota"
    direcao        = "desc"
    condicao       = @{ operador = ">"; valor = 0.5; unidade = "percentual" }
    dataReferencia = $null
    fonte          = "prova_objetiva"
    baseLegal      = $null
} -Expect @(201) -Nome "POST desempate nota"

$listaCrit = Invoke-Api GET "/fases/$($faseObj.id)/criterios-desempate" -Nome "GET criterios-desempate"

if ($c2) {
    Invoke-Api PUT "/criterios-desempate/$($c2.id)" -Body @{
        label          = "Maior nota na prova objetiva"
        tipo           = "nota"
        direcao        = "desc"
        condicao       = @{ operador = ">"; valor = 1.0; unidade = "percentual" }
        dataReferencia = $null
        fonte          = "prova_objetiva"
        baseLegal      = $null
    } -Nome "PUT desempate" | Out-Null
}

if ($listaCrit -and $listaCrit.Count -gt 1) {
    $idsOrdem = @($listaCrit | ForEach-Object { $_.id })
    # inverte para exercitar reordenacao com TODOS os ids
    [Array]::Reverse($idsOrdem)
    Invoke-Api PUT "/fases/$($faseObj.id)/criterios-desempate/ordem" -Body @{
        ids = $idsOrdem
    } -Nome "PUT desempate ordem" | Out-Null
}

# --- Fases do concurso / estrutura ---
Invoke-Api GET "/editais/$EditalId/fases-do-concurso" -Nome "GET fases-do-concurso" | Out-Null
Invoke-Api GET "/editais/$EditalId/estrutura-por-grupo" -Nome "GET estrutura-por-grupo" | Out-Null
Invoke-Api GET "/editais/$EditalId/config-publicacoes" -Nome "GET config-publicacoes" | Out-Null

# --- Cronograma interno ---
$crono = Invoke-Api POST "/editais/$EditalId/cronograma" -Expect @(200, 201) -Nome "POST abrir cronograma"
$cronoId = $null
if ($crono -and $crono.id) { $cronoId = $crono.id; $script:Ctx.cronoId = $cronoId }
if ($cronoId) {
    Invoke-Api GET "/cronogramas/$cronoId" -Nome "GET cronograma" | Out-Null
    Invoke-Api GET "/cronograma/modelo" -Nome "GET cronograma modelo" | Out-Null
}

# --- Cronograma publico (Anexo II) ---
$itensPub = @(
    @{ descricao = "Publicacao do edital"; inicio = $hoje; termino = $hoje; destaque = $true },
    @{ descricao = "PerIodo de inscricao"; inicio = $hoje; termino = $fim; destaque = $false },
    @{ descricao = "Prova objetiva"; inicio = $fim; termino = $fim; destaque = $true }
)
Invoke-Api PUT "/editais/$EditalId/cronograma-publico" -Body @{ itens = $itensPub } -Nome "PUT cronograma-publico" | Out-Null
Invoke-Api GET "/editais/$EditalId/cronograma-publico" -Nome "GET cronograma-publico" | Out-Null

# --- Disciplinas (se objetiva) ---
if ($etapaObj) {
    $discResp = Invoke-Api POST "/etapas/$($etapaObj.id)/disciplinas" -Body @{
        nome          = "Lingua Portuguesa"
        questaoInicio = 1
        questaoFim    = 10
        cargoIds      = @($cargoA)
    } -Expect @(200, 201) -Nome "POST disciplina"
    $listaDisc = Invoke-Api GET "/etapas/$($etapaObj.id)/disciplinas" -Nome "GET disciplinas"
    $faixaId = $null
    if ($discResp -and $discResp.disciplinas -and $discResp.disciplinas.Count -gt 0) {
        $faixaId = $discResp.disciplinas[0].faixaId
    } elseif ($listaDisc -and $listaDisc.disciplinas -and $listaDisc.disciplinas.Count -gt 0) {
        $faixaId = $listaDisc.disciplinas[0].faixaId
    }
    if ($faixaId) {
        Invoke-Api PUT "/disciplinas/$faixaId/peso" -Body @{ peso = 2 } -Expect @(200, 204) -Nome "PUT peso disciplina" | Out-Null
        Invoke-Api DELETE "/disciplinas/$faixaId" -Expect @(200, 204) -Nome "DELETE disciplina" | Out-Null
    } else {
        Fail "disciplina faixa" "POST/GET nao retornou faixaId"
    }
}

# --- DELETE (ordem: extras → criterios → limpeza opcional) ---
if ($etapaExtraId) {
    Invoke-Api DELETE "/etapas/$etapaExtraId" -Expect @(200, 204) -Nome "DELETE etapa recurso" | Out-Null
}
if ($c2) {
    Invoke-Api DELETE "/criterios-desempate/$($c2.id)" -Expect @(200, 204) -Nome "DELETE desempate" | Out-Null
}

if ($Cleanup) {
    Write-Host "`n--- Cleanup ---" -ForegroundColor Yellow
    $fases = Invoke-Api GET "/fases?grupoCargoId=$grupoId" -Nome "GET fases (cleanup)"
    foreach ($f in @($fases)) {
        $ets = Invoke-Api GET "/etapas?faseId=$($f.id)" -Nome "GET etapas cleanup"
        foreach ($e in @($ets)) {
            # RF nao pode excluir - esperar 4xx
            $expect = if ($tipoRf -and $e.tipoId -eq $tipoRf.id) { @(400, 409) } else { @(200, 204, 400, 409) }
            Invoke-Api DELETE "/etapas/$($e.id)" -Expect $expect -Nome "DELETE etapa $($e.id)" | Out-Null
        }
        Invoke-Api DELETE "/fases/$($f.id)" -Expect @(200, 204, 400, 409) -Nome "DELETE fase $($f.id)" | Out-Null
    }
    Invoke-Api DELETE "/grupos-cargo/$grupoId/cargos/$cargoA" -Expect @(200, 204) -Nome "DELETE cargo A" | Out-Null
    Invoke-Api DELETE "/grupos-cargo/$grupoId/cargos/$cargoB" -Expect @(200, 204) -Nome "DELETE cargo B" | Out-Null
    Invoke-Api DELETE "/grupos-cargo/$grupoId" -Expect @(200, 204) -Nome "DELETE grupo" | Out-Null
}

Write-Host "`n=== Resumo ===" -ForegroundColor Cyan
Write-Host "Passou: $script:Passou  |  Falhou: $script:Falhou"
Write-Host "Contexto: $($script:Ctx | ConvertTo-Json -Compress)"
if ($script:Erros.Count -gt 0) {
    Write-Host "`nFalhas:" -ForegroundColor Red
    $script:Erros | ForEach-Object { Write-Host " - $_" }
    exit 1
}
Write-Host "`nE2E OK - edital $EditalId pronto para uso no front (grupo E2E Nivel Medio)." -ForegroundColor Green
exit 0

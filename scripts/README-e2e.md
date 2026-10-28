# E2E — fases, etapas e cronograma

Script: `e2e-edital-fase-cronograma.ps1`

## Pré-requisitos

- `modulo-fase` em `quarkus:dev` (`http://localhost:8002`)
- Para **criar edital do zero**: `modulo-dados` em `http://localhost:8001`

## Rodar

```powershell
# Cria edital + 2 cargos e testa tudo (recomendado)
.\scripts\e2e-edital-fase-cronograma.ps1 -CriarNovo

# Ou usa um edital já existente com cargos sem grupo
.\scripts\e2e-edital-fase-cronograma.ps1 -EditalId 305

# Limpa grupo/fases ao final
.\scripts\e2e-edital-fase-cronograma.ps1 -EditalId 305 -Cleanup
```

Último edital criado do zero e validado: **306** (`ORGAO E2E TESTE` / `E2E FASES ETAPAS CRONOGRAMA …`) — **42/42 OK**.

## O que cobre (CREATE / UPDATE / DELETE)

| Área | Endpoints |
|------|-----------|
| Edital (com `-CriarNovo`) | POST step1, PUT step2, PATCH configs, POST cargos |
| Grupos | POST/PUT/GET/DELETE grupos-cargo + cargos |
| Fases | POST gerar-fases, GET/PUT fase |
| Etapas | GET/PUT/POST/DELETE etapa |
| Publicações | GET/PUT datas |
| Desempate | POST/GET/PUT/DELETE + ordem |
| Disciplinas | POST/GET/PUT peso/DELETE |
| Cronograma | POST abrir, GET, modelo |
| Cronograma público | PUT/GET itens Anexo II |
| Leitura | fases-do-concurso, estrutura-por-grupo, config-publicacoes |

## Front

Abrir o edital gerado → **Fases e Etapas** → grupo **E2E Nivel Medio …**.

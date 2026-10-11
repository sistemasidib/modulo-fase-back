package br.bioregistro.fase.application.controllers;

import br.bioregistro.fase.application.use_cases.ConcursosEmMemoria;
import br.bioregistro.fase.application.use_cases.CriteriosDesempateEmMemoria;
import br.bioregistro.fase.application.use_cases.CronogramasEmMemoria;
import br.bioregistro.fase.application.use_cases.DisciplinasEmMemoria;
import br.bioregistro.fase.application.use_cases.EtapasEmMemoria;
import br.bioregistro.fase.application.use_cases.FasesEmMemoria;
import br.bioregistro.fase.application.use_cases.GruposEmMemoria;
import br.bioregistro.fase.application.use_cases.PublicacoesEmMemoria;
import br.bioregistro.fase.application.use_cases.TiposEmMemoria;

import java.time.LocalDate;
import java.time.ZoneId;

import static br.bioregistro.fase.domain.model.Instituicao.IDECAN;
import static br.bioregistro.fase.domain.model.Instituicao.IDIB;

/**
 * Dados em memória de um teste; {@link RepositoriosEmMemoria} entrega estes repositórios no lugar
 * dos adapters de SQL Server, e o relógio fica "hoje" em 10/10/2026, para
 * exercitar os endpoints de ponta a ponta (HTTP → controller → use case → domínio) sem banco.
 * Chamar em {@code @BeforeEach}: cada teste começa com dados limpos.
 *
 * <p>Dados iniciais: editais 10 e 11 no IDECAN (cargos 101, 102, 103 no 10) e edital 20 no IDIB.
 */
public final class ApiEmMemoria {

    public static final ZoneId BR = ZoneId.of("America/Sao_Paulo");
    public static final LocalDate HOJE = LocalDate.of(2026, 10, 10);

    public final FasesEmMemoria fases = new FasesEmMemoria()
            .comEdital(IDECAN, 10).comEdital(IDECAN, 11).comEdital(IDIB, 20);
    public final GruposEmMemoria grupos = new GruposEmMemoria(fases).comCargos(10, 101, 102, 103);
    public final EtapasEmMemoria etapas = new EtapasEmMemoria();
    public final PublicacoesEmMemoria publicacoes = new PublicacoesEmMemoria();
    public final TiposEmMemoria tipos = new TiposEmMemoria();
    public final ConcursosEmMemoria concursos = new ConcursosEmMemoria()
            .comEdital(IDECAN, 10, "PM-XX 2027").comEdital(IDECAN, 11, "PM-XX 2027").comEdital(IDIB, 20, "Outro");
    public final CronogramasEmMemoria cronogramas = new CronogramasEmMemoria();
    public final DisciplinasEmMemoria disciplinas = new DisciplinasEmMemoria();
    public final CriteriosDesempateEmMemoria criteriosDesempate = new CriteriosDesempateEmMemoria();

    private ApiEmMemoria() {
    }

    public static ApiEmMemoria instalar() {
        ApiEmMemoria api = new ApiEmMemoria();
        RepositoriosEmMemoria.atual = api;
        return api;
    }
}

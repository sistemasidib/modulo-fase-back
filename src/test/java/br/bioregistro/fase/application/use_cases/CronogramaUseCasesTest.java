package br.bioregistro.fase.application.use_cases;

import br.bioregistro.fase.application.dtos.ConcursoDtos.ConcursoRequest;
import br.bioregistro.fase.application.dtos.CronogramaDtos.AcaoRequest;
import br.bioregistro.fase.application.dtos.CronogramaDtos.CronogramaResponse;
import br.bioregistro.fase.application.dtos.CronogramaDtos.ParticularidadeRequest;
import br.bioregistro.fase.application.dtos.CronogramaDtos.SalvarCronogramaRequest;
import br.bioregistro.fase.domain.exception.ConflitoException;
import br.bioregistro.fase.domain.exception.NaoEncontradoException;
import br.bioregistro.fase.domain.exception.RegraDeNegocioException;
import br.bioregistro.fase.domain.model.Instituicao;
import br.bioregistro.fase.domain.model.cronograma.Acao;
import br.bioregistro.fase.domain.model.cronograma.Edicao;
import br.bioregistro.fase.domain.model.cronograma.Parametros;
import br.bioregistro.fase.domain.model.cronograma.Previa;
import br.bioregistro.fase.domain.model.cronograma.StatusEtapa;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Critérios CB01, CB02, CB07, CB10, CB13–CB16 da spec 003-B, sem banco. */
class CronogramaUseCasesTest {

    private static final Instituicao IDECAN = Instituicao.IDECAN;
    private static final Instituicao IDIB = Instituicao.IDIB;
    private static final ZoneId BR = ZoneId.of("America/Sao_Paulo");
    private static final Clock HOJE = Clock.fixed(LocalDate.of(2026, 10, 10).atStartOfDay(BR).toInstant(), BR);

    /** Edital 03/12/2026: RM-01 = 48 dias, nenhuma violação. */
    private static final Edicao OK = new Edicao(Set.of(), LocalDate.of(2026, 12, 3), Map.of(), Set.of(), Set.of());

    private final FasesEmMemoria fases = new FasesEmMemoria();
    private final ConcursosEmMemoria concursos = new ConcursosEmMemoria()
            .comEdital(IDECAN, 10, "PM-XX 2027").comEdital(IDECAN, 11, "PM-XX 2027").comEdital(IDIB, 20, "Outro");
    private final CronogramasEmMemoria cronogramas = new CronogramasEmMemoria();

    private final ConsultarConcursos consultarConcursos = new ConsultarConcursos(concursos, cronogramas);
    private final ManterConcurso manterConcurso = new ManterConcurso(concursos, cronogramas, fases, consultarConcursos);
    private final ConsultarCronograma consultar = new ConsultarCronograma(cronogramas, HOJE);
    private final EditarCronograma editar = new EditarCronograma(cronogramas, concursos, consultar);

    CronogramaUseCasesTest() {
        fases.comEdital(IDECAN, 10).comEdital(IDECAN, 11).comEdital(IDIB, 20);
    }

    private int concurso(String nome) {
        return manterConcurso.criar(IDECAN, new ConcursoRequest(nome)).id();
    }

    private static Edicao edital(LocalDate data) {
        return new Edicao(Set.of(), data, Map.of(), Set.of(), Set.of());
    }

    private static SalvarCronogramaRequest salvar(int versao, Edicao edicao, String justificativa) {
        return new SalvarCronogramaRequest(versao, edicao, List.of(), justificativa);
    }

    @Test
    void cb01_editalEmUmConcursoSo() {
        int a = concurso("PM-XX 2027");
        int b = concurso("Outro concurso");
        manterConcurso.incluirEdital(IDECAN, a, 10);
        assertEquals(1, consultarConcursos.buscar(IDECAN, a).editais().size());

        ConflitoException erro = assertThrows(ConflitoException.class, () -> manterConcurso.incluirEdital(IDECAN, b, 10));
        assertTrue(erro.getMessage().contains("PM-XX 2027"));
        assertThrows(NaoEncontradoException.class, () -> manterConcurso.incluirEdital(IDECAN, a, 999));
        // CA03: edital do IDIB não existe no banco do IDECAN
        assertThrows(NaoEncontradoException.class, () -> manterConcurso.incluirEdital(IDECAN, a, 20));
    }

    @Test
    void sugereConcursoPeloNomeDoEdital() {
        int a = concurso("PM-XX 2027");
        manterConcurso.incluirEdital(IDECAN, a, 10);
        var soltos = consultarConcursos.editaisSemConcurso(IDECAN);
        assertEquals(1, soltos.size());
        assertEquals(11, soltos.get(0).id());
        assertEquals(a, soltos.get(0).concursoSugeridoId());
    }

    @Test
    void concursoComCronogramaNaoPodeSerExcluido() {
        int a = concurso("PM-XX 2027");
        editar.abrir(IDECAN, a, "Yuri");
        assertThrows(ConflitoException.class, () -> manterConcurso.excluir(IDECAN, a));
    }

    @Test
    void cb15_abrirDuasVezesDevolveOMesmo() {
        int a = concurso("PM-XX 2027");
        var primeiro = editar.abrir(IDECAN, a, "Yuri");
        var segundo = editar.abrir(IDECAN, a, "Yuri");
        assertTrue(primeiro.criado());
        assertFalse(segundo.criado());
        assertEquals(primeiro.id(), segundo.id());
        Acao geracao = consultar.historico(IDECAN, primeiro.id()).get(0);
        assertEquals(Acao.GERACAO, geracao.origem());
        assertTrue(geracao.detalhe().contains("26 etapas"));
    }

    @Test
    void cb07_mudarOModeloNaoMudaCronogramaSalvo() {
        int a = concurso("PM-XX 2027");
        int id = editar.abrir(IDECAN, a, "Yuri").id();
        cronogramas.padrao = new Parametros(Map.of(Parametros.GABARITO_APOS_PROVA, 10));

        CronogramaResponse salvo = consultar.buscar(IDECAN, id);
        var e16 = salvo.calculado().etapa("E16").orElseThrow();
        var e15 = salvo.calculado().etapa("E15").orElseThrow();
        assertEquals(15, e16.inicioRelativo() - e15.inicioRelativo());

        int novo = editar.abrir(IDECAN, concurso("Novo"), "Yuri").id();
        var n16 = consultar.buscar(IDECAN, novo).calculado().etapa("E16").orElseThrow();
        var n15 = consultar.buscar(IDECAN, novo).calculado().etapa("E15").orElseThrow();
        assertEquals(10, n16.inicioRelativo() - n15.inicioRelativo());
    }

    @Test
    void cb10_violacaoExigeJustificativa() {
        int id = editar.abrir(IDECAN, concurso("PM-XX 2027"), "Yuri").id();
        Edicao viola = edital(LocalDate.of(2026, 12, 1)); // RM-01 = 43 dias

        assertThrows(RegraDeNegocioException.class, () -> editar.salvar(IDECAN, id, salvar(0, viola, "  "), "Yuri"));
        CronogramaResponse salvo = editar.salvar(IDECAN, id, salvar(0, viola, "Órgão pediu a data"), "Yuri");

        assertEquals("MATERIALIZADO", salvo.situacao());
        assertEquals("Órgão pediu a data", salvo.justificativa());
        Acao salvamento = consultar.historico(IDECAN, id).get(0);
        assertEquals(Acao.SALVAMENTO, salvamento.origem());
        assertEquals("Órgão pediu a data", salvamento.justificativa());
    }

    @Test
    void semViolacaoSalvaSemJustificativa() {
        int id = editar.abrir(IDECAN, concurso("PM-XX 2027"), "Yuri").id();
        CronogramaResponse salvo = editar.salvar(IDECAN, id, salvar(0, edital(LocalDate.of(2026, 12, 3)), null), "Yuri");
        assertFalse(salvo.calculado().checagens().viola());
        assertEquals(1, salvo.versao());
    }

    @Test
    void cb11_editalNovoNoPassadoEhRecusado() {
        int id = editar.abrir(IDECAN, concurso("PM-XX 2027"), "Yuri").id();
        assertThrows(RegraDeNegocioException.class,
                () -> editar.salvar(IDECAN, id, salvar(0, edital(LocalDate.of(2026, 10, 9)), "x"), "Yuri"));
    }

    @Test
    void cb13_versaoDesatualizadaEhConflito() {
        int id = editar.abrir(IDECAN, concurso("PM-XX 2027"), "Yuri").id();
        editar.salvar(IDECAN, id, salvar(0, OK, null), "Ana");
        assertThrows(ConflitoException.class, () -> editar.salvar(IDECAN, id, salvar(0, OK, null), "Yuri"));
    }

    @Test
    void cb14_cascataViraUmaAcaoComNItens() {
        int id = editar.abrir(IDECAN, concurso("PM-XX 2027"), "Yuri").id();
        List<Previa.Item> itens = java.util.stream.IntStream.range(0, 20)
                .mapToObj(i -> new Previa.Item("E" + (20 + i), "data", "a", "b")).toList();
        AcaoRequest cascata = new AcaoRequest("Alterou com cascata · E04", "duração: 31 → 25", Acao.RECALCULO, itens);
        editar.salvar(IDECAN, id, new SalvarCronogramaRequest(0, OK, List.of(cascata), null), "Yuri");

        List<Acao> historico = consultar.historico(IDECAN, id);
        Acao gravada = historico.stream().filter(a -> a.acao().startsWith("Alterou")).findFirst().orElseThrow();
        assertEquals(20, gravada.itens().size());
        assertEquals("Yuri", gravada.autor());
    }

    @Test
    void cb16_statusSegueOFluxoEOGatilhoDoRecurso() {
        int id = editar.abrir(IDECAN, concurso("PM-XX 2027"), "Yuri").id();
        editar.salvar(IDECAN, id, salvar(0, edital(LocalDate.of(2026, 12, 3)), null), "Yuri");

        // prazo do recurso do gabarito (E17) ainda não abriu em 10/10/2026
        assertThrows(RegraDeNegocioException.class, () -> editar.registrarPrimeiroRecurso(IDECAN, id, "E17", "Yuri"));
        assertThrows(RegraDeNegocioException.class,
                () -> editar.marcarStatus(IDECAN, id, "E17", StatusEtapa.EM_ANALISE, "Yuri"));

        CronogramaResponse feito = editar.marcarStatus(IDECAN, id, "E04", StatusEtapa.FEITO, "Yuri");
        var e04 = feito.status().stream().filter(s -> s.codigo().equals("E04")).findFirst().orElseThrow();
        assertEquals(StatusEtapa.FEITO, e04.manual());
        assertNull(e04.automatico());
        assertTrue(consultar.historico(IDECAN, id).get(0).acao().contains("E04"));
    }

    @Test
    void particularidadeEntraESaiDoHistorico() {
        int id = editar.abrir(IDECAN, concurso("PM-XX 2027"), "Yuri").id();
        CronogramaResponse com = editar.adicionarParticularidade(IDECAN, id,
                new ParticularidadeRequest("Faltosos do processo interno não são eliminados", "Prova objetiva", "E15"),
                "Yuri");
        assertEquals(1, com.particularidades().size());
        CronogramaResponse sem = editar.removerParticularidade(IDECAN, id, com.particularidades().get(0).id(), "Yuri");
        assertTrue(sem.particularidades().isEmpty());
        assertTrue(consultar.historico(IDECAN, id).get(0).acao().startsWith("Removeu particularidade"));
        assertThrows(RegraDeNegocioException.class, () -> editar.adicionarParticularidade(IDECAN, id,
                new ParticularidadeRequest("x", "Assunto inválido", null), "Yuri"));
    }

    @Test
    void simularNaoGrava() {
        int id = editar.abrir(IDECAN, concurso("PM-XX 2027"), "Yuri").id();
        var calculado = consultar.simular(IDECAN, id, new Edicao(Set.of("S12"), null, Map.of(), Set.of(), Set.of()));
        assertEquals(29, calculado.indicadores().etapasAtivas());
        assertEquals(0, cronogramas.salvos.get(id).versao());
        assertTrue(cronogramas.salvos.get(id).edicao().modulos().isEmpty());
    }
}

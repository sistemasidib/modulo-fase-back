package br.bioregistro.fase.application.use_cases;

import br.bioregistro.fase.application.dtos.ConcursoDtos.ConcursoRequest;
import br.bioregistro.fase.application.dtos.CronogramaDtos.AcaoRequest;
import br.bioregistro.fase.application.dtos.CronogramaDtos.CronogramaResponse;
import br.bioregistro.fase.application.dtos.CronogramaDtos.ParticularidadeRequest;
import br.bioregistro.fase.application.dtos.CronogramaDtos.SalvarCronogramaRequest;
import br.bioregistro.fase.application.dtos.PublicacaoDatasRequest;
import br.bioregistro.fase.application.dtos.PublicacaoResponse;
import br.bioregistro.fase.domain.exception.ConflitoException;
import br.bioregistro.fase.domain.exception.NaoEncontradoException;
import br.bioregistro.fase.domain.exception.RegraDeNegocioException;
import br.bioregistro.fase.domain.model.Fase;
import br.bioregistro.fase.domain.model.Instituicao;
import br.bioregistro.fase.domain.model.cronograma.Acao;
import br.bioregistro.fase.domain.model.cronograma.CronogramaCalculado;
import br.bioregistro.fase.domain.model.cronograma.Edicao;
import br.bioregistro.fase.domain.model.cronograma.ModuloDeExecucao;
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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Critérios CB01, CB02, CB07, CB10, CB13–CB16 da spec 003-B e, da spec 005, o cronograma
 * compartilhado com sobreposição por edital e a ponte para a estrutura. Sem banco.
 */
class CronogramaUseCasesTest {

    private static final Instituicao IDECAN = Instituicao.IDECAN;
    private static final Instituicao IDIB = Instituicao.IDIB;
    private static final ZoneId BR = ZoneId.of("America/Sao_Paulo");
    private static final Clock HOJE = Clock.fixed(LocalDate.of(2026, 10, 10).atStartOfDay(BR).toInstant(), BR);

    /** Edital 03/12/2026: RM-01 = 34 dias, nenhuma violação. */
    private static final Edicao OK = new Edicao(Set.of(), LocalDate.of(2026, 12, 3), Map.of(), Set.of(), Set.of());

    private final FasesEmMemoria fases = new FasesEmMemoria();
    private final EtapasEmMemoria etapas = new EtapasEmMemoria();
    private final TiposEmMemoria tipos = new TiposEmMemoria();
    private final PublicacoesEmMemoria publicacoes = new PublicacoesEmMemoria();
    private final ConcursosEmMemoria concursos = new ConcursosEmMemoria()
            .comEdital(IDECAN, 10, "PM-XX 2027").comEdital(IDECAN, 11, "PM-XX 2027").comEdital(IDIB, 20, "Outro");
    private final CronogramasEmMemoria cronogramas = new CronogramasEmMemoria(concursos);
    private final VinculosEmMemoria vinculos = new VinculosEmMemoria(cronogramas, publicacoes);

    private final ConsultarConcursos consultarConcursos = new ConsultarConcursos(concursos, cronogramas);
    private final ManterConcurso manterConcurso = new ManterConcurso(concursos, cronogramas, fases, consultarConcursos);
    private final ConsultarCronograma consultar = new ConsultarCronograma(cronogramas, HOJE);
    private final PonteCronograma ponte = new PonteCronograma(cronogramas, consultar,
            new EstruturaDoCronograma(cronogramas, vinculos, fases, etapas, tipos, publicacoes));
    private final EditarCronograma editar = new EditarCronograma(cronogramas, concursos, fases, consultar, ponte);
    private final PublicacoesDoCronograma doCronograma = new PublicacoesDoCronograma(vinculos,
            new EncerramentoDoLegado((LocalDate) null, HOJE, fases, cronogramas));

    CronogramaUseCasesTest() {
        fases.comEdital(IDECAN, 10).comEdital(IDECAN, 11).comEdital(IDIB, 20);
    }

    private int concurso(String nome) {
        return manterConcurso.criar(IDECAN, new ConcursoRequest(nome)).id();
    }

    /** Concurso com os editais 10 e 11 e o cronograma aberto (os dois o seguem). */
    private int concursoComDoisEditais() {
        int a = concurso("PM-XX 2027");
        manterConcurso.incluirEdital(IDECAN, a, 10);
        manterConcurso.incluirEdital(IDECAN, a, 11);
        return editar.abrir(IDECAN, a, "Yuri").id();
    }

    private static Edicao edital(LocalDate data) {
        return new Edicao(Set.of(), data, Map.of(), Set.of(), Set.of());
    }

    private static SalvarCronogramaRequest salvar(int versao, Edicao edicao, String justificativa) {
        return new SalvarCronogramaRequest(versao, edicao, List.of(), justificativa);
    }

    private CronogramaResponse doEdital(int id, int editalId) {
        return consultar.buscar(IDECAN, id, editalId);
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

    /** Spec 005, RN13b: o concurso só agrupa; os editais continuam seguindo o cronograma. */
    @Test
    void concursoComCronogramaPodeSerExcluido() {
        int id = concursoComDoisEditais();
        int a = cronogramas.salvos.get(id).concursoId();
        manterConcurso.excluir(IDECAN, a);

        assertNull(cronogramas.salvos.get(id).concursoId());
        assertEquals(id, cronogramas.doEdital(IDECAN, 10).orElseThrow().cronogramaId());
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
    void abrirLigaOsEditaisDoConcurso() {
        int id = concursoComDoisEditais();
        assertEquals(id, cronogramas.doEdital(IDECAN, 10).orElseThrow().cronogramaId());
        assertEquals(id, cronogramas.doEdital(IDECAN, 11).orElseThrow().cronogramaId());
        var concurso = consultarConcursos.buscar(IDECAN, cronogramas.salvos.get(id).concursoId());
        assertEquals(id, concurso.cronogramaId());
        assertEquals(2, concurso.cronogramasDosEditais().size());
        assertEquals(2, consultar.buscar(IDECAN, id).editais().size());
    }

    @Test
    void cb07_mudarOModeloNaoMudaCronogramaSalvo() {
        int a = concurso("PM-XX 2027");
        int id = editar.abrir(IDECAN, a, "Yuri").id();
        cronogramas.padrao = new Parametros(Map.of(Parametros.GABARITO_APOS_PROVA, 10));

        CronogramaResponse salvo = consultar.buscar(IDECAN, id);
        var e16 = salvo.calculado().etapa("E16").orElseThrow();
        var e15 = salvo.calculado().etapa("E15").orElseThrow();
        assertEquals(1, e16.inicioRelativo() - e15.inicioRelativo());

        int novo = editar.abrir(IDECAN, concurso("Novo"), "Yuri").id();
        var n16 = consultar.buscar(IDECAN, novo).calculado().etapa("E16").orElseThrow();
        var n15 = consultar.buscar(IDECAN, novo).calculado().etapa("E15").orElseThrow();
        assertEquals(10, n16.inicioRelativo() - n15.inicioRelativo());
    }

    @Test
    void cb10_violacaoExigeJustificativa() {
        int id = editar.abrir(IDECAN, concurso("PM-XX 2027"), "Yuri").id();
        Edicao viola = new Edicao(Set.of(), LocalDate.of(2026, 12, 1), Map.of("E15", new Edicao.Ajuste(20, null)),
                Set.of(), Set.of(Parametros.INTERVALO_PROVA)); // RM-01 = 20 dias

        assertThrows(RegraDeNegocioException.class,
                () -> editar.salvar(IDECAN, id, null, salvar(0, viola, "  "), "Yuri"));
        CronogramaResponse salvo = editar.salvar(IDECAN, id, null, salvar(0, viola, "Órgão pediu a data"), "Yuri");

        assertEquals("MATERIALIZADO", salvo.situacao());
        assertEquals("Órgão pediu a data", salvo.justificativa());
        Acao salvamento = consultar.historico(IDECAN, id).get(0);
        assertEquals(Acao.SALVAMENTO, salvamento.origem());
        assertEquals("Órgão pediu a data", salvamento.justificativa());
    }

    @Test
    void semViolacaoSalvaSemJustificativa() {
        int id = editar.abrir(IDECAN, concurso("PM-XX 2027"), "Yuri").id();
        CronogramaResponse salvo = editar.salvar(IDECAN, id, null, salvar(0, edital(LocalDate.of(2026, 12, 3)), null),
                "Yuri");
        assertFalse(salvo.calculado().checagens().viola());
        assertEquals(1, salvo.versao());
    }

    /** Spec 005, RN08: edital no passado (publicado, retificado) salva e só gera alerta. */
    @Test
    void editalNoPassadoSoAlerta() {
        int id = editar.abrir(IDECAN, concurso("PM-XX 2027"), "Yuri").id();
        CronogramaResponse salvo = editar.salvar(IDECAN, id, null,
                salvar(0, edital(LocalDate.of(2026, 10, 9)), null), "Yuri");
        CronogramaCalculado.Checagens checagens = salvo.calculado().checagens();
        assertFalse(checagens.viola());
        assertFalse(checagens.alertas().stream().filter(a -> a.codigo().equals("EDITAL_NO_PRAZO"))
                .findFirst().orElseThrow().cumprida());
    }

    @Test
    void cb13_versaoDesatualizadaEhConflito() {
        int id = editar.abrir(IDECAN, concurso("PM-XX 2027"), "Yuri").id();
        editar.salvar(IDECAN, id, null, salvar(0, OK, null), "Ana");
        assertThrows(ConflitoException.class, () -> editar.salvar(IDECAN, id, null, salvar(0, OK, null), "Yuri"));
    }

    @Test
    void cb14_cascataViraUmaAcaoComNItens() {
        int id = editar.abrir(IDECAN, concurso("PM-XX 2027"), "Yuri").id();
        List<Previa.Item> itens = java.util.stream.IntStream.range(0, 20)
                .mapToObj(i -> new Previa.Item("E" + (20 + i), "data", "a", "b")).toList();
        AcaoRequest cascata = new AcaoRequest("Alterou com cascata · E04", "duração: 31 → 25", Acao.RECALCULO, itens);
        editar.salvar(IDECAN, id, null, new SalvarCronogramaRequest(0, OK, List.of(cascata), null), "Yuri");

        List<Acao> historico = consultar.historico(IDECAN, id);
        Acao gravada = historico.stream().filter(a -> a.acao().startsWith("Alterou")).findFirst().orElseThrow();
        assertEquals(20, gravada.itens().size());
        assertEquals("Yuri", gravada.autor());
    }

    @Test
    void cb16_statusSegueOFluxoEOGatilhoDoRecurso() {
        int id = editar.abrir(IDECAN, concurso("PM-XX 2027"), "Yuri").id();
        editar.salvar(IDECAN, id, null, salvar(0, edital(LocalDate.of(2026, 12, 3)), null), "Yuri");

        // prazo do recurso do gabarito (E17) ainda não abriu em 10/10/2026
        assertThrows(RegraDeNegocioException.class,
                () -> editar.registrarPrimeiroRecurso(IDECAN, id, null, "E17", "Yuri"));
        assertThrows(RegraDeNegocioException.class,
                () -> editar.marcarStatus(IDECAN, id, null, "E17", StatusEtapa.EM_ANALISE, "Yuri"));

        CronogramaResponse feito = editar.marcarStatus(IDECAN, id, null, "E04", StatusEtapa.FEITO, "Yuri");
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

    // ── spec 005: compartilhado + sobreposição por edital ─────────────────────

    @Test
    void mudarUmEditalNaoMudaOsOutros() {
        int id = concursoComDoisEditais();
        editar.salvar(IDECAN, id, null, salvar(0, OK, null), "Yuri");

        CronogramaResponse doDez = doEdital(id, 10);
        editar.salvar(IDECAN, id, 10, salvar(doDez.versao(), edital(LocalDate.of(2026, 12, 10)), null), "Ana");

        assertEquals(LocalDate.of(2026, 12, 10), doEdital(id, 10).edicao().dataEdital());
        assertEquals(Set.of("E01"), doEdital(id, 10).sobreposto());
        assertEquals(OK.dataEdital(), doEdital(id, 11).edicao().dataEdital());
        assertEquals(OK.dataEdital(), consultar.buscar(IDECAN, id).edicao().dataEdital());
        assertEquals(10, consultar.historico(IDECAN, id).get(0).editalId());
    }

    @Test
    void mudarOCompartilhadoValeParaQuemNaoSobrepos() {
        int id = concursoComDoisEditais();
        editar.salvar(IDECAN, id, null, salvar(0, OK, null), "Yuri");
        editar.salvar(IDECAN, id, 10, salvar(doEdital(id, 10).versao(), edital(LocalDate.of(2026, 12, 10)), null),
                "Ana");

        Edicao comDiscursiva = new Edicao(Set.of("S12"), LocalDate.of(2026, 12, 17), Map.of(), Set.of(), Set.of());
        editar.salvar(IDECAN, id, null, salvar(1, comDiscursiva, null), "Yuri");

        assertEquals(LocalDate.of(2026, 12, 17), doEdital(id, 11).edicao().dataEdital());
        assertEquals(LocalDate.of(2026, 12, 10), doEdital(id, 10).edicao().dataEdital());
        assertEquals(Set.of("S12"), doEdital(id, 10).edicao().modulos());
    }

    @Test
    void statusDoEditalNaoMexeNoCompartilhado() {
        int id = concursoComDoisEditais();
        editar.salvar(IDECAN, id, null, salvar(0, OK, null), "Yuri");
        editar.marcarStatus(IDECAN, id, 10, "E04", StatusEtapa.FEITO, "Ana");

        assertEquals(StatusEtapa.FEITO, cronogramas.salvos.get(id).doEdital(
                cronogramas.doEdital(IDECAN, 10).orElseThrow()).status("E04"));
        assertEquals(StatusEtapa.A_FAZER, cronogramas.salvos.get(id).status("E04"));
    }

    @Test
    void itensEExcecoesSoNoCompartilhado() {
        int id = concursoComDoisEditais();
        Edicao comExcecao = new Edicao(Set.of(), null, Map.of(), Set.of(), Set.of(Parametros.ISENCAO));
        assertThrows(RegraDeNegocioException.class,
                () -> editar.salvar(IDECAN, id, 10, salvar(0, comExcecao, null), "Ana"));
    }

    @Test
    void restaurarVoltaAoCompartilhado() {
        int id = concursoComDoisEditais();
        editar.salvar(IDECAN, id, null, salvar(0, OK, null), "Yuri");
        editar.salvar(IDECAN, id, 10, salvar(doEdital(id, 10).versao(), edital(LocalDate.of(2026, 12, 10)), null),
                "Ana");

        CronogramaResponse restaurado = editar.restaurar(IDECAN, id, 10, "Ana");
        assertEquals(OK.dataEdital(), restaurado.edicao().dataEdital());
        assertTrue(restaurado.sobreposto().isEmpty());
    }

    @Test
    void separarDaAoEditalUmCronogramaProprio() {
        int id = concursoComDoisEditais();
        editar.salvar(IDECAN, id, null, salvar(0, OK, null), "Yuri");
        editar.salvar(IDECAN, id, 10, salvar(doEdital(id, 10).versao(), edital(LocalDate.of(2026, 12, 10)), null),
                "Ana");

        int proprio = editar.separar(IDECAN, 10, "Ana").id();
        assertNotEquals(id, proprio);
        assertNull(cronogramas.salvos.get(proprio).concursoId());
        assertEquals(LocalDate.of(2026, 12, 10), consultar.buscar(IDECAN, proprio, 10).edicao().dataEdital());

        editar.salvar(IDECAN, id, null, salvar(1, edital(LocalDate.of(2026, 12, 17)), null), "Yuri");
        assertEquals(LocalDate.of(2026, 12, 10), consultar.buscar(IDECAN, proprio, 10).edicao().dataEdital());
        assertThrows(NaoEncontradoException.class, () -> consultar.buscar(IDECAN, id, 10));
    }

    @Test
    void seguirOCronogramaDeOutroEdital() {
        int id = concursoComDoisEditais();
        int proprio = editar.separar(IDECAN, 10, "Ana").id();
        editar.seguir(IDECAN, 11, proprio, "Ana");
        assertEquals(proprio, cronogramas.doEdital(IDECAN, 11).orElseThrow().cronogramaId());
        assertTrue(cronogramas.editais(IDECAN, id).isEmpty());
        assertThrows(NaoEncontradoException.class, () -> editar.seguir(IDECAN, 11, 999, "Ana"));
    }

    @Test
    void abrirDoEditalLigaAoCronogramaDoConcurso() {
        int a = concurso("PM-XX 2027");
        manterConcurso.incluirEdital(IDECAN, a, 10);
        var aberto = editar.abrirDoEdital(IDECAN, a, 10, "Yuri");
        assertTrue(aberto.criado());
        assertEquals(aberto.id(), consultarConcursos.buscar(IDECAN, a).cronogramaId());
        assertEquals(aberto.id(), cronogramas.doEdital(IDECAN, 10).orElseThrow().cronogramaId());

        var denovo = editar.abrirDoEdital(IDECAN, a, 10, "Yuri");
        assertFalse(denovo.criado());
        assertEquals(aberto.id(), denovo.id());
    }

    @Test
    void cronogramaSoParaEditalDoConcurso() {
        int a = concurso("PM-XX 2027");
        manterConcurso.incluirEdital(IDECAN, a, 10);
        assertThrows(RegraDeNegocioException.class, () -> editar.abrirDoEdital(IDECAN, a, 11, "Yuri"));
        assertThrows(NaoEncontradoException.class, () -> editar.abrirDoEdital(IDECAN, 999, 10, "Yuri"));
    }

    /** Spec 005, RN17: retirar edital é normal e não mexe no cronograma; fora do concurso = 404. */
    @Test
    void retirarEditalNaoMexeNoCronograma() {
        int id = concursoComDoisEditais();
        int a = cronogramas.salvos.get(id).concursoId();

        assertEquals(1, manterConcurso.retirarEdital(IDECAN, a, 10).editais().size());
        assertEquals(id, cronogramas.doEdital(IDECAN, 10).orElseThrow().cronogramaId());
        assertThrows(NaoEncontradoException.class, () -> manterConcurso.retirarEdital(IDECAN, a, 10));
    }

    @Test
    void simularNaoGrava() {
        int id = editar.abrir(IDECAN, concurso("PM-XX 2027"), "Yuri").id();
        var calculado = consultar.simular(IDECAN, id, new Edicao(Set.of("S12"), null, Map.of(), Set.of(), Set.of()));
        assertEquals(29, calculado.indicadores().etapasAtivas());
        assertEquals(0, cronogramas.salvos.get(id).versao());
        assertTrue(cronogramas.salvos.get(id).edicao().modulos().isEmpty());
    }

    // ── spec 005: ponte para a estrutura ──────────────────────────────────────

    private Fase faseDoModulo(int editalId, ModuloDeExecucao modulo) {
        int vinculo = cronogramas.doEdital(IDECAN, editalId).orElseThrow().id();
        int faseId = vinculos.faseDoModulo(IDECAN, vinculo, modulo).orElseThrow();
        return fases.buscar(IDECAN, faseId).orElseThrow();
    }

    @Test
    void rascunhoNaoCriaFase() {
        int id = concursoComDoisEditais();
        int vinculo = cronogramas.doEdital(IDECAN, 10).orElseThrow().id();
        assertTrue(vinculos.faseDoModulo(IDECAN, vinculo, ModuloDeExecucao.NUCLEO).isEmpty());
        assertTrue(fases.listarPorEdital(IDECAN, 10).isEmpty());
        assertFalse(cronogramas.linhasDoEdital(IDECAN, vinculo).isEmpty());
        assertEquals(id, cronogramas.doEdital(IDECAN, 10).orElseThrow().cronogramaId());
    }

    @Test
    void ponteCriaAProvaObjetivaComAsPublicacoesDoCronograma() {
        int id = concursoComDoisEditais();
        Edicao comProva = new Edicao(Set.of(), LocalDate.of(2026, 12, 3), Map.of(), Set.of(), Set.of(), List.of(),
                LocalDate.of(2027, 2, 20)); // sábado
        CronogramaResponse salvo = editar.salvar(IDECAN, id, null, salvar(0, comProva, null), "Yuri");
        assertEquals(LocalDate.of(2027, 2, 20), salvo.calculado().etapa("E15").orElseThrow().inicio());

        for (int edital : List.of(10, 11)) {
            Fase fase = faseDoModulo(edital, ModuloDeExecucao.NUCLEO);
            assertEquals("Prova objetiva", fase.nome());
            assertEquals(1, etapas.listarPorFase(IDECAN, fase.id()).size());
            assertEquals(5, vinculos.publicacoesDaFase(IDECAN, fase.id()).size());
        }

        Fase fase = faseDoModulo(10, ModuloDeExecucao.NUCLEO);
        List<PublicacaoResponse> lista = new ListarPublicacoes(fases, publicacoes, doCronograma).executar(IDECAN, fase.id());
        PublicacaoResponse gabarito = lista.stream().filter(p -> p.tipo().equals("gabarito_preliminar"))
                .findFirst().orElseThrow();
        assertEquals(LocalDate.of(2027, 2, 21), gabarito.dataPrevista().toLocalDate());
        assertEquals(LocalDate.of(2027, 2, 21), gabarito.recursoInicio().toLocalDate());
        assertNull(gabarito.publicadaEm());

        editar.marcarStatus(IDECAN, id, 10, "E16", StatusEtapa.PUBLICADO, "Ana");
        PublicacaoResponse publicado = new ListarPublicacoes(fases, publicacoes, doCronograma)
                .executar(IDECAN, fase.id()).stream().filter(p -> p.tipo().equals("gabarito_preliminar"))
                .findFirst().orElseThrow();
        assertEquals(LocalDate.of(2027, 2, 21), publicado.publicadaEm().toLocalDate());
    }

    @Test
    void sincronizarDeNovoNaoDuplica() {
        int id = concursoComDoisEditais();
        editar.salvar(IDECAN, id, null, salvar(0, OK, null), "Yuri");
        int fasesAntes = fases.listarPorEdital(IDECAN, 10).size();
        int publicacoesAntes = publicacoes.listarPorFase(IDECAN, faseDoModulo(10, ModuloDeExecucao.NUCLEO).id()).size();

        ponte.sincronizarEdital(IDECAN, 10);
        ponte.sincronizarEdital(IDECAN, 10);
        assertEquals(fasesAntes, fases.listarPorEdital(IDECAN, 10).size());
        assertEquals(publicacoesAntes,
                publicacoes.listarPorFase(IDECAN, faseDoModulo(10, ModuloDeExecucao.NUCLEO).id()).size());
    }

    @Test
    void ligarADiscursivaSoNumEditalCriaAFaseSoNele() {
        int id = concursoComDoisEditais();
        editar.salvar(IDECAN, id, null, salvar(0, OK, null), "Yuri");
        Edicao comDiscursiva = new Edicao(Set.of("S12"), OK.dataEdital(), Map.of(), Set.of(), Set.of());
        editar.salvar(IDECAN, id, 10, salvar(doEdital(id, 10).versao(), comDiscursiva, null), "Ana");

        assertEquals("Prova discursiva", faseDoModulo(10, ModuloDeExecucao.S12).nome());
        int vinculo11 = cronogramas.doEdital(IDECAN, 11).orElseThrow().id();
        assertTrue(vinculos.faseDoModulo(IDECAN, vinculo11, ModuloDeExecucao.S12).isEmpty());
    }

    @Test
    void publicacaoDoCronogramaNaoAceitaDataManual() {
        int id = concursoComDoisEditais();
        editar.salvar(IDECAN, id, null, salvar(0, OK, null), "Yuri");
        int faseId = faseDoModulo(10, ModuloDeExecucao.NUCLEO).id();
        int publicacaoId = publicacoes.listarPorFase(IDECAN, faseId).get(0).id();

        PublicacaoDatasRequest datas = new PublicacaoDatasRequest(null, null, null, null, null);
        assertThrows(ConflitoException.class,
                () -> new AtualizarPublicacao(publicacoes, doCronograma).executar(IDECAN, publicacaoId, datas));
        assertThrows(ConflitoException.class,
                () -> new PublicarPublicacao(publicacoes, doCronograma).executar(IDECAN, publicacaoId, datas));
        assertThrows(ConflitoException.class,
                () -> new ExcluirPublicacao(publicacoes, doCronograma).executar(IDECAN, publicacaoId));
    }
}

package br.bioregistro.fase.application.use_cases;

import br.bioregistro.fase.application.dtos.CriterioCorrecaoDtos.CriterioCorrecaoRequest;
import br.bioregistro.fase.application.dtos.EstruturaPorGrupoDtos.GrupoEstruturaResponse;
import br.bioregistro.fase.application.dtos.EtapaRequest;
import br.bioregistro.fase.application.dtos.EtapaResponse;
import br.bioregistro.fase.application.dtos.GrupoCargoRequest;
import br.bioregistro.fase.domain.model.ConfiguracaoDePublicacao;
import br.bioregistro.fase.domain.model.FormatoDePublicacao;
import br.bioregistro.fase.domain.model.Modalidade;
import br.bioregistro.fase.domain.model.Publicacao;
import br.bioregistro.fase.domain.model.TipoPublicacao;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import static br.bioregistro.fase.application.use_cases.Cenario.etapa;
import static br.bioregistro.fase.application.use_cases.TiposEmMemoria.BIOPSICOSSOCIAL;
import static br.bioregistro.fase.application.use_cases.TiposEmMemoria.GABARITO_PRELIMINAR;
import static br.bioregistro.fase.application.use_cases.TiposEmMemoria.HETEROIDENTIFICACAO;
import static br.bioregistro.fase.application.use_cases.TiposEmMemoria.PROVA_DISCURSIVA;
import static br.bioregistro.fase.application.use_cases.TiposEmMemoria.PROVA_OBJETIVA;
import static br.bioregistro.fase.application.use_cases.TiposEmMemoria.RESULTADO_OFICIAL;
import static br.bioregistro.fase.application.use_cases.TiposEmMemoria.RESULTADO_PRELIMINAR;
import static br.bioregistro.fase.domain.model.Instituicao.IDECAN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Spec006UseCasesTest {

    private final Cenario c = new Cenario().comEdital(IDECAN, 10);

    Spec006UseCasesTest() {
        c.grupos.comCargos(10, 101, 102);
    }

    @Test
    void estruturaPorGrupoMostraFaseEmCadaGrupo() {
        int gA = new CriarGrupoCargo(c.fases, c.grupos).executar(IDECAN, 10, new GrupoCargoRequest("A")).id();
        int gB = new CriarGrupoCargo(c.fases, c.grupos).executar(IDECAN, 10, new GrupoCargoRequest("B")).id();
        new AdicionarCargoAoGrupo(c.grupos).executar(IDECAN, gA, 101);
        new AdicionarCargoAoGrupo(c.grupos).executar(IDECAN, gB, 102);
        int faseId = c.criarFase().executar(IDECAN, 10, Cenario.fase("Prova objetiva", 1)).id();
        c.grupos.vincularFase(IDECAN, faseId, gA);
        c.grupos.vincularFase(IDECAN, faseId, gB);

        List<GrupoEstruturaResponse> estrutura = new EstruturaPorGrupo(c.fases, c.etapas, c.grupos, c.criterios)
                .executar(IDECAN, 10);

        assertEquals(2, estrutura.size());
        assertTrue(estrutura.stream().allMatch(g -> g.fases().stream()
                .anyMatch(f -> f.fase().nome().equals("Prova objetiva"))));
        assertEquals(2, estrutura.getFirst().fases().getFirst().gruposAfetados());
    }

    @Test
    void discursivaNasceComNotaZeroElimina() {
        int faseId = c.criarFase().executar(IDECAN, 10, Cenario.fase("Discursiva", 1)).id();
        EtapaResponse criada = c.criarEtapa().executar(IDECAN, faseId, etapa("Discursiva", PROVA_DISCURSIVA, null));
        assertEquals(true, criada.notaZeroElimina());
    }

    @Test
    void notaMinimaMostraPontosSobreSomaDosCriterios() {
        int faseId = c.criarFase().executar(IDECAN, 10, Cenario.fase("Discursiva", 1)).id();
        int etapaId = c.criarEtapa().executar(IDECAN, faseId, etapa("Discursiva", PROVA_DISCURSIVA, null)).id();
        c.criteriosDeCorrecao().criar(IDECAN, etapaId, new CriterioCorrecaoRequest("Formal", "Concordância", null,
                new BigDecimal("30")));
        c.criteriosDeCorrecao().criar(IDECAN, etapaId, new CriterioCorrecaoRequest("Textual", "Coesão", null,
                new BigDecimal("20")));
        EtapaResponse atualizada = c.atualizarEtapa().executar(IDECAN, etapaId,
                new EtapaRequest("Discursiva", PROVA_DISCURSIVA, null, null, null, null, null, null, null, null,
                        null, null, null, null, null, null, true, new BigDecimal("40"), null, null));
        assertEquals(0, new BigDecimal("50").compareTo(atualizada.pontuacaoMaxima()));
        assertEquals(0, new BigDecimal("20.00").compareTo(atualizada.notaMinimaEmPontos()));
    }

    @Test
    void modalidadesS05SaoIndependentes() {
        int faseId = c.criarFase().executar(IDECAN, 10, Cenario.fase("S05", 1)).id();
        EtapaResponse hetero = c.criarEtapa().executar(IDECAN, faseId,
                new EtapaRequest("Hetero", HETEROIDENTIFICACAO, null, null, null, null, null, null, null, null,
                        null, null, null, null, null, null, null, null, "ONLINE", null));
        EtapaResponse biopsico = c.criarEtapa().executar(IDECAN, faseId,
                etapa("Biopsico", BIOPSICOSSOCIAL, null));
        assertEquals(Modalidade.REMOTA.name(), hetero.modalidade());
        assertEquals(Modalidade.PRESENCIAL.name(), biopsico.modalidade());
        assertTrue(c.publicacoes.listarPorEtapa(IDECAN, hetero.id()).stream()
                .anyMatch(p -> TipoPublicacao.LINK_ACESSO.equals(p.tipo().valor())));
        assertTrue(c.publicacoes.listarPorEtapa(IDECAN, biopsico.id()).stream()
                .anyMatch(p -> TipoPublicacao.LOCAL_PROVA.equals(p.tipo().valor())));
    }

    @Test
    void trocaModalidadeComPublicacaoPublicadaNaoMudaEAvisa() {
        int faseId = c.criarFase().executar(IDECAN, 10, Cenario.fase("S05", 1)).id();
        EtapaResponse hetero = c.criarEtapa().executar(IDECAN, faseId,
                etapa("Hetero", HETEROIDENTIFICACAO, null));
        Publicacao locais = c.publicacoes.listarPorEtapa(IDECAN, hetero.id()).stream()
                .filter(p -> TipoPublicacao.LOCAL_PROVA.equals(p.tipo().valor()))
                .findFirst().orElseThrow();
        var agora = java.time.OffsetDateTime.now();
        c.publicacoes.salvar(IDECAN, locais.comDatas(agora, agora, agora.plusDays(1), null, null).publicar(agora));

        EtapaResponse atualizada = c.atualizarEtapa().executar(IDECAN, hetero.id(),
                new EtapaRequest("Hetero", HETEROIDENTIFICACAO, null, null, null, null, null, null, null, null,
                        null, null, null, null, null, null, null, null, "ONLINE", null));
        assertTrue(atualizada.aviso() != null && !atualizada.aviso().isBlank());
        assertTrue(c.publicacoes.listarPorEtapa(IDECAN, hetero.id()).stream()
                .anyMatch(p -> TipoPublicacao.LOCAL_PROVA.equals(p.tipo().valor())));
    }

    @Test
    void overrideDeConfigNaoAfetaOutroEdital() {
        c.comEdital(IDECAN, 20);
        ConfiguracoesDePublicacao uc = new ConfiguracoesDePublicacao(c.configsPublicacao, c.tipos, c.fases);
        c.configsPublicacao.comPadrao(new ConfiguracaoDePublicacao(2, "prova_objetiva", 0, 3, FormatoDePublicacao.LISTA_PDF));
        uc.salvarEdital(IDECAN, 10, new ConfiguracaoDePublicacao(2, "prova_objetiva", 0, 7, FormatoDePublicacao.TABELA_LINK));
        assertEquals(7, uc.efetiva(IDECAN, 10, 2).diasNoAr());
        assertEquals(3, uc.efetiva(IDECAN, 20, 2).diasNoAr());
    }

    @Test
    void listarEfetivasUsaPadraoEOverride() {
        ConfiguracoesDePublicacao uc = new ConfiguracoesDePublicacao(c.configsPublicacao, c.tipos, c.fases);
        c.configsPublicacao.comPadrao(new ConfiguracaoDePublicacao(GABARITO_PRELIMINAR.id(), "prova_objetiva", 1, 3,
                FormatoDePublicacao.LISTA_PDF));
        c.configsPublicacao.comPadrao(new ConfiguracaoDePublicacao(RESULTADO_PRELIMINAR.id(), null, 0, 5,
                FormatoDePublicacao.LISTA_PDF));
        uc.salvarEdital(IDECAN, 10, new ConfiguracaoDePublicacao(GABARITO_PRELIMINAR.id(), "prova_objetiva", 1, 9,
                FormatoDePublicacao.TABELA_LINK));
        List<ConfiguracaoDePublicacao> lista = uc.listarEfetivas(IDECAN, 10);
        assertEquals(2, lista.size());
        assertEquals(9, lista.stream().filter(x -> x.tipoPublicacaoId().equals(GABARITO_PRELIMINAR.id()))
                .findFirst().orElseThrow().diasNoAr());
        assertEquals(5, lista.stream().filter(x -> x.tipoPublicacaoId().equals(RESULTADO_PRELIMINAR.id()))
                .findFirst().orElseThrow().diasNoAr());
    }

    @Test
    void datasDaEtapaDerivamPeriodosDosEspacosVazios() {
        c.configsPublicacao.comPadrao(new ConfiguracaoDePublicacao(GABARITO_PRELIMINAR.id(), "prova_objetiva", 7, 14,
                FormatoDePublicacao.LISTA_PDF));
        c.configsPublicacao.comPadrao(new ConfiguracaoDePublicacao(RESULTADO_PRELIMINAR.id(), null, 0, 5,
                FormatoDePublicacao.LISTA_PDF));
        c.configsPublicacao.comPadrao(new ConfiguracaoDePublicacao(RESULTADO_OFICIAL.id(), null, 5, 30,
                FormatoDePublicacao.LISTA_PDF));

        int faseId = c.criarFase().executar(IDECAN, 10, Cenario.fase("Obj", 1)).id();
        OffsetDateTime fimProva = OffsetDateTime.parse("2026-11-10T23:59:00-03:00");
        EtapaResponse criada = c.criarEtapa().executar(IDECAN, faseId,
                new EtapaRequest("Obj", PROVA_OBJETIVA, null,
                        OffsetDateTime.parse("2026-11-10T08:00:00-03:00"), fimProva,
                        null, null, null, null, null, null, null, null, null, null, null, null, null, null, null));

        Publicacao gabarito = c.publicacoes.listarPorEtapa(IDECAN, criada.id()).stream()
                .filter(p -> "gabarito_preliminar".equals(p.tipo().valor())).findFirst().orElseThrow();
        assertEquals(LocalDate.of(2026, 11, 17), gabarito.visualizacaoInicio().toLocalDate());
        assertEquals(LocalDate.of(2026, 11, 30), gabarito.visualizacaoFim().toLocalDate());

        Publicacao prelim = c.publicacoes.listarPorEtapa(IDECAN, criada.id()).stream()
                .filter(p -> "resultado_preliminar".equals(p.tipo().valor())).findFirst().orElseThrow();
        assertEquals(LocalDate.of(2026, 11, 10), prelim.visualizacaoInicio().toLocalDate());
        assertEquals(LocalDate.of(2026, 11, 14), prelim.visualizacaoFim().toLocalDate());

        Publicacao oficial = c.publicacoes.listarPorEtapa(IDECAN, criada.id()).stream()
                .filter(p -> "resultado_oficial".equals(p.tipo().valor())).findFirst().orElseThrow();
        assertEquals(LocalDate.of(2026, 11, 15), oficial.visualizacaoInicio().toLocalDate());
        assertEquals(LocalDate.of(2026, 12, 14), oficial.visualizacaoFim().toLocalDate());
    }

    @Test
    void mudarAncoraRecalculaEspacoNaoPublicado() {
        c.configsPublicacao.comPadrao(new ConfiguracaoDePublicacao(GABARITO_PRELIMINAR.id(), "prova_objetiva", 1, 3,
                FormatoDePublicacao.LISTA_PDF));
        int faseId = c.criarFase().executar(IDECAN, 10, Cenario.fase("Obj", 1)).id();
        EtapaResponse criada = c.criarEtapa().executar(IDECAN, faseId, etapa("Obj", PROVA_OBJETIVA, null));
        Publicacao gabarito = c.publicacoes.listarPorEtapa(IDECAN, criada.id()).stream()
                .filter(p -> "gabarito_preliminar".equals(p.tipo().valor())).findFirst().orElseThrow();
        OffsetDateTime manualIni = OffsetDateTime.parse("2026-12-01T00:00:00-03:00");
        OffsetDateTime manualFim = OffsetDateTime.parse("2026-12-05T23:59:00-03:00");
        c.publicacoes.salvar(IDECAN, gabarito.comDatas(manualIni, manualIni, manualFim,
                manualIni, manualFim));

        c.atualizarEtapa().executar(IDECAN, criada.id(),
                new EtapaRequest("Obj", PROVA_OBJETIVA, null,
                        OffsetDateTime.parse("2026-11-10T08:00:00-03:00"),
                        OffsetDateTime.parse("2026-11-10T23:59:00-03:00"),
                        null, null, null, null, null, null, null, null, null, null, null, null, null, null, null));

        Publicacao apos = c.publicacoes.listarPorEtapa(IDECAN, criada.id()).stream()
                .filter(p -> "gabarito_preliminar".equals(p.tipo().valor())).findFirst().orElseThrow();
        // D+1 a partir de 10/11 → 11/11 (não preserva override manual se não publicado)
        assertEquals(LocalDate.of(2026, 11, 11), apos.visualizacaoInicio().toLocalDate());
    }

    @Test
    void objetivaNaoAceitaCamposDaDiscursiva() {
        int faseId = c.criarFase().executar(IDECAN, 10, Cenario.fase("Obj", 1)).id();
        EtapaResponse criada = c.criarEtapa().executar(IDECAN, faseId, etapa("Obj", PROVA_OBJETIVA, null));
        assertNull(criada.notaZeroElimina());
        assertNull(criada.modalidade());
    }
}

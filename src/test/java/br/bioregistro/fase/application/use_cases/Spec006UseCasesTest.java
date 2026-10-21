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
import java.util.List;

import static br.bioregistro.fase.application.use_cases.Cenario.etapa;
import static br.bioregistro.fase.application.use_cases.TiposEmMemoria.BIOPSICOSSOCIAL;
import static br.bioregistro.fase.application.use_cases.TiposEmMemoria.HETEROIDENTIFICACAO;
import static br.bioregistro.fase.application.use_cases.TiposEmMemoria.PROVA_DISCURSIVA;
import static br.bioregistro.fase.application.use_cases.TiposEmMemoria.PROVA_OBJETIVA;
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
                        null, null, null, null, null, null, true, new BigDecimal("40"), null));
        assertEquals(0, new BigDecimal("50").compareTo(atualizada.pontuacaoMaxima()));
        assertEquals(0, new BigDecimal("20.00").compareTo(atualizada.notaMinimaEmPontos()));
    }

    @Test
    void modalidadesS05SaoIndependentes() {
        int faseId = c.criarFase().executar(IDECAN, 10, Cenario.fase("S05", 1)).id();
        EtapaResponse hetero = c.criarEtapa().executar(IDECAN, faseId,
                new EtapaRequest("Hetero", HETEROIDENTIFICACAO, null, null, null, null, null, null, null, null,
                        null, null, null, null, null, null, null, null, "ONLINE"));
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
                        null, null, null, null, null, null, null, null, "ONLINE"));
        assertTrue(atualizada.aviso() != null && !atualizada.aviso().isBlank());
        assertTrue(c.publicacoes.listarPorEtapa(IDECAN, hetero.id()).stream()
                .anyMatch(p -> TipoPublicacao.LOCAL_PROVA.equals(p.tipo().valor())));
    }

    @Test
    void overrideDeConfigNaoAfetaOutroEdital() {
        c.comEdital(IDECAN, 20);
        ConfiguracoesDePublicacao uc = new ConfiguracoesDePublicacao(c.configsPublicacao, c.tipos, c.fases);
        c.configsPublicacao.comPadrao(new ConfiguracaoDePublicacao(2, "E16", 0, 3, FormatoDePublicacao.LISTA_PDF));
        uc.salvarEdital(IDECAN, 10, new ConfiguracaoDePublicacao(2, "E16", 0, 7, FormatoDePublicacao.TABELA_LINK));
        assertEquals(7, uc.efetiva(IDECAN, 10, 2).diasNoAr());
        assertEquals(3, uc.efetiva(IDECAN, 20, 2).diasNoAr());
    }

    @Test
    void objetivaNaoAceitaCamposDaDiscursiva() {
        int faseId = c.criarFase().executar(IDECAN, 10, Cenario.fase("Obj", 1)).id();
        EtapaResponse criada = c.criarEtapa().executar(IDECAN, faseId, etapa("Obj", PROVA_OBJETIVA, null));
        assertNull(criada.notaZeroElimina());
        assertNull(criada.modalidade());
    }
}

package br.bioregistro.fase.application.use_cases;

import br.bioregistro.fase.application.dtos.EtapaResponse;
import br.bioregistro.fase.domain.exception.ConflitoException;
import br.bioregistro.fase.domain.exception.NaoEncontradoException;
import br.bioregistro.fase.domain.model.Publicacao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static br.bioregistro.fase.application.use_cases.Cenario.etapa;
import static br.bioregistro.fase.application.use_cases.Cenario.fase;
import static br.bioregistro.fase.application.use_cases.TiposEmMemoria.PROVA_OBJETIVA;
import static br.bioregistro.fase.application.use_cases.TiposEmMemoria.TAF;
import static br.bioregistro.fase.domain.model.Instituicao.IDECAN;
import static br.bioregistro.fase.domain.model.Instituicao.IDIB;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EtapaUseCasesTest {

    private final Cenario c = new Cenario().comEdital(IDECAN, 10);
    private int faseId;

    @BeforeEach
    void criarFase() {
        faseId = c.criarFase().executar(IDECAN, 10, fase("Objetiva", 1)).id();
    }

    private List<String> espacosDaEtapa(int etapaId) {
        return c.publicacoes.listarPorEtapa(IDECAN, etapaId).stream().map(p -> p.tipo().valor()).toList();
    }

    @Test
    void criaEtapaSemDatasComFlagsFalsasPorPadrao() {
        EtapaResponse criada = c.criarEtapa().executar(IDECAN, faseId, etapa("Prova", PROVA_OBJETIVA, null));

        assertEquals(faseId, criada.faseId());
        assertFalse(criada.reprovavel());
        assertNull(criada.dataAbertura());
    }

    @Test
    void provaObjetivaNasceComSeusEspacosDePublicacao() {
        EtapaResponse criada = c.criarEtapa().executar(IDECAN, faseId, etapa("Prova", PROVA_OBJETIVA, null));

        assertEquals(List.of("local_prova", "gabarito_preliminar", "gabarito_definitivo",
                "resultado_preliminar", "resultado_oficial"), espacosDaEtapa(criada.id()));
    }

    @Test
    void tafNasceComOsEspacosConfiguradosParaOTipo() {
        EtapaResponse criada = c.criarEtapa().executar(IDECAN, faseId, etapa("TAF", TAF, null));

        assertEquals(List.of("resultado_preliminar", "resultado_oficial"), espacosDaEtapa(criada.id()));
    }

    @Test
    void tipoDeEtapaPrecisaExistir() {
        assertThrows(NaoEncontradoException.class,
                () -> c.criarEtapa().executar(IDECAN, faseId, etapa("Prova", 999, null)));
    }

    @Test
    void naoCriaEtapaEmFaseInexistente() {
        assertThrows(NaoEncontradoException.class,
                () -> c.criarEtapa().executar(IDECAN, 999, etapa("Prova", PROVA_OBJETIVA, null)));
    }

    @Test
    void faseDeOutraInstituicaoNaoEEncontrada() {
        assertThrows(NaoEncontradoException.class,
                () -> c.criarEtapa().executar(IDIB, faseId, etapa("Prova", PROVA_OBJETIVA, null)));
    }

    @Test
    void preRequisitoPrecisaExistir() {
        assertThrows(NaoEncontradoException.class,
                () -> c.criarEtapa().executar(IDECAN, faseId, etapa("TAF", TAF, 42)));
    }

    @Test
    void atualizaMantendoAFase() {
        EtapaResponse prova = c.criarEtapa().executar(IDECAN, faseId, etapa("Prova", PROVA_OBJETIVA, null));
        EtapaResponse taf = c.criarEtapa().executar(IDECAN, faseId, etapa("TAF", TAF, null));

        EtapaResponse atualizada = c.atualizarEtapa().executar(IDECAN, taf.id(), etapa("TAF final", TAF, prova.id()));
        assertEquals("TAF final", atualizada.nome());
        assertEquals(faseId, atualizada.faseId());
        assertEquals(prova.id(), atualizada.elegibilidadeEtapaId());
    }

    @Test
    void listaBuscaEExcluiJuntoComEspacosVazios() {
        EtapaResponse criada = c.criarEtapa().executar(IDECAN, faseId, etapa("Prova", PROVA_OBJETIVA, null));

        assertEquals(1, new ListarEtapas(c.fases, c.etapas).executar(IDECAN, faseId).size());
        assertEquals("Prova", new BuscarEtapa(c.etapas).executar(IDECAN, criada.id()).nome());

        c.excluirEtapa().executar(IDECAN, criada.id());
        assertTrue(c.etapas.buscar(IDECAN, criada.id()).isEmpty());
        assertTrue(espacosDaEtapa(criada.id()).isEmpty());
        assertThrows(NaoEncontradoException.class, () -> c.excluirEtapa().executar(IDECAN, criada.id()));
    }

    @Test
    void naoExcluiEtapaComPublicacaoPreenchida() {
        EtapaResponse criada = c.criarEtapa().executar(IDECAN, faseId, etapa("Prova", PROVA_OBJETIVA, null));
        Publicacao local = c.publicacoes.listarPorEtapa(IDECAN, criada.id()).get(0);
        c.publicacoes.salvar(IDECAN, local.comDatas(Cenario.ABERTURA, null, null, null, null));

        assertThrows(ConflitoException.class, () -> c.excluirEtapa().executar(IDECAN, criada.id()));
        assertFalse(c.etapas.buscar(IDECAN, criada.id()).isEmpty());
    }
}

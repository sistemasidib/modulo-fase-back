package br.bioregistro.fase.application.use_cases;

import br.bioregistro.fase.application.dtos.GerarFasesRequest;
import br.bioregistro.fase.application.dtos.GerarFasesRequest.EtapaGerada;
import br.bioregistro.fase.application.dtos.GerarFasesRequest.FaseGerada;
import br.bioregistro.fase.application.dtos.GrupoCargoRequest;
import br.bioregistro.fase.domain.exception.ConflitoException;
import br.bioregistro.fase.domain.exception.NaoEncontradoException;
import br.bioregistro.fase.domain.model.Publicacao;
import org.junit.jupiter.api.Test;

import java.util.List;

import static br.bioregistro.fase.application.use_cases.Cenario.ABERTURA;
import static br.bioregistro.fase.application.use_cases.Cenario.FECHAMENTO;
import static br.bioregistro.fase.application.use_cases.TiposEmMemoria.GABARITO_DEFINITIVO;
import static br.bioregistro.fase.application.use_cases.TiposEmMemoria.GABARITO_PRELIMINAR;
import static br.bioregistro.fase.application.use_cases.TiposEmMemoria.LOCAL_PROVA;
import static br.bioregistro.fase.application.use_cases.TiposEmMemoria.PROVA_OBJETIVA;
import static br.bioregistro.fase.application.use_cases.TiposEmMemoria.RESULTADO_OFICIAL;
import static br.bioregistro.fase.application.use_cases.TiposEmMemoria.RESULTADO_PRELIMINAR;
import static br.bioregistro.fase.application.use_cases.TiposEmMemoria.TAF;
import static br.bioregistro.fase.domain.model.Instituicao.IDECAN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PublicacoesDaFaseDoConcursoTest {

    private final Cenario c = new Cenario().comEdital(IDECAN, 10);
    private final PublicacoesDaFaseDoConcurso publicacoes =
            new PublicacoesDaFaseDoConcurso(c.vinculos, c.fases, c.etapas, c.publicacoes, c.espacos);

    PublicacoesDaFaseDoConcursoTest() {
        c.grupos.comCargos(10, 101, 102);
    }

    private int grupoComFase(String nome, int cargoId, List<Integer> escolhidas) {
        int grupoId = new CriarGrupoCargo(c.fases, c.grupos).executar(IDECAN, 10, new GrupoCargoRequest(nome)).id();
        new AdicionarCargoAoGrupo(c.grupos).executar(IDECAN, grupoId, cargoId);
        c.gerarFases().executar(IDECAN, grupoId, new GerarFasesRequest(List.of(new FaseGerada(
                "Objetiva", null, ABERTURA, FECHAMENTO, List.of(new EtapaGerada(PROVA_OBJETIVA, null, escolhidas))))));
        return grupoId;
    }

    private List<String> daEtapaDoGrupo(int grupoId) {
        int faseId = c.fases.listarPorGrupo(IDECAN, grupoId).get(0).id();
        int etapaId = c.etapas.listarPorFase(IDECAN, faseId).get(0).id();
        return c.publicacoes.listarPorEtapa(IDECAN, etapaId).stream().map(p -> p.tipo().valor()).toList();
    }

    @Test
    void etapaNasceComAsEscolhidasEOResultadoSempre() {
        int medio = grupoComFase("Médio", 101, List.of(LOCAL_PROVA.id()));

        assertEquals(List.of("local_prova", "resultado_preliminar", "resultado_oficial"), daEtapaDoGrupo(medio));
        assertEquals(List.of(LOCAL_PROVA.id(), RESULTADO_PRELIMINAR.id(), RESULTADO_OFICIAL.id()),
                publicacoes.listar(IDECAN, 10, PROVA_OBJETIVA));
    }

    @Test
    void semEscolhaUsaOsPadraoDoTipo() {
        int medio = grupoComFase("Médio", 101, null);

        assertEquals(5, daEtapaDoGrupo(medio).size());
    }

    @Test
    void tipoDePublicacaoPrecisaExistir() {
        assertThrows(NaoEncontradoException.class, () -> grupoComFase("Médio", 101, List.of(999)));
    }

    @Test
    void mudarValeParaTodosOsGrupos() {
        int medio = grupoComFase("Médio", 101, null);
        int superior = grupoComFase("Superior", 102, null);

        List<Integer> resultado = publicacoes.definir(IDECAN, 10, PROVA_OBJETIVA,
                List.of(GABARITO_PRELIMINAR.id(), GABARITO_DEFINITIVO.id()));

        List<String> esperadas = List.of("gabarito_preliminar", "gabarito_definitivo", "resultado_preliminar", "resultado_oficial");
        assertEquals(List.of(GABARITO_PRELIMINAR.id(), GABARITO_DEFINITIVO.id(), RESULTADO_PRELIMINAR.id(),
                RESULTADO_OFICIAL.id()), resultado);
        assertEquals(esperadas, daEtapaDoGrupo(medio));
        assertEquals(esperadas, daEtapaDoGrupo(superior));
    }

    @Test
    void oResultadoNuncaSaiDaEtapa() {
        int medio = grupoComFase("Médio", 101, null);

        publicacoes.definir(IDECAN, 10, PROVA_OBJETIVA, List.of());

        assertEquals(List.of("resultado_preliminar", "resultado_oficial"), daEtapaDoGrupo(medio));
    }

    @Test
    void naoTiraTipoComPublicacaoPreenchida() {
        int medio = grupoComFase("Médio", 101, null);
        int faseId = c.fases.listarPorGrupo(IDECAN, medio).get(0).id();
        Publicacao gabarito = c.publicacoes.listarPorFase(IDECAN, faseId).stream()
                .filter(p -> p.tipo().equals(GABARITO_PRELIMINAR)).findFirst().orElseThrow();
        c.publicacoes.salvar(IDECAN, gabarito.comDatas(ABERTURA, null, null, null, null));

        assertThrows(ConflitoException.class,
                () -> publicacoes.definir(IDECAN, 10, PROVA_OBJETIVA, List.of(LOCAL_PROVA.id())));
    }

    @Test
    void faseDoConcursoPrecisaExistirNoEdital() {
        grupoComFase("Médio", 101, null);

        assertThrows(NaoEncontradoException.class, () -> publicacoes.listar(IDECAN, 10, TAF));
        assertThrows(NaoEncontradoException.class, () -> publicacoes.listar(IDECAN, 99, PROVA_OBJETIVA));
    }
}

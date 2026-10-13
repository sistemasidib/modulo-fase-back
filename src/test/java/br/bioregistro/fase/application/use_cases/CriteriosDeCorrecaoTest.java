package br.bioregistro.fase.application.use_cases;

import br.bioregistro.fase.application.dtos.CriterioCorrecaoDtos.CriterioCorrecaoRequest;
import br.bioregistro.fase.application.dtos.CriterioCorrecaoDtos.CriterioCorrecaoResponse;
import br.bioregistro.fase.domain.exception.NaoEncontradoException;
import br.bioregistro.fase.domain.exception.RegraDeNegocioException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static br.bioregistro.fase.application.use_cases.Cenario.etapa;
import static br.bioregistro.fase.application.use_cases.Cenario.fase;
import static br.bioregistro.fase.application.use_cases.TiposEmMemoria.PROVA_DISCURSIVA;
import static br.bioregistro.fase.application.use_cases.TiposEmMemoria.PROVA_OBJETIVA;
import static br.bioregistro.fase.domain.model.Instituicao.IDECAN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CriteriosDeCorrecaoTest {

    private final Cenario c = new Cenario().comEdital(IDECAN, 10);
    private final CriteriosDeCorrecao criterios = c.criteriosDeCorrecao();
    private int faseId;
    private int discursiva;

    @BeforeEach
    void criarProva() {
        faseId = c.criarFase().executar(IDECAN, 10, fase("Discursiva", 1)).id();
        discursiva = c.criarEtapa().executar(IDECAN, faseId, etapa("Discursiva", PROVA_DISCURSIVA, null)).id();
    }

    private static CriterioCorrecaoRequest pedido(String modulo, String criterio, String desconto, String maximo) {
        return new CriterioCorrecaoRequest(modulo, criterio, desconto == null ? null : new BigDecimal(desconto),
                new BigDecimal(maximo));
    }

    private List<String> nomes(int etapaId) {
        return criterios.listar(IDECAN, etapaId).stream().map(CriterioCorrecaoResponse::criterio).toList();
    }

    @Test
    void criaNoFimDaListaComDuasCasas() {
        criterios.criar(IDECAN, discursiva, pedido(" Formal ", "Colocação pronominal", "0.25", "2.5"));
        CriterioCorrecaoResponse segundo =
                criterios.criar(IDECAN, discursiva, pedido("Técnico", "Conhecimento do tema", null, "1.5"));

        assertEquals(2, segundo.ordem());
        assertNull(segundo.descontoPorErro());
        CriterioCorrecaoResponse primeiro = criterios.listar(IDECAN, discursiva).get(0);
        assertEquals("Formal", primeiro.modulo());
        assertEquals(new BigDecimal("0.25"), primeiro.descontoPorErro());
        assertEquals(new BigDecimal("2.50"), primeiro.pontuacaoMaxima());
    }

    @Test
    void provaObjetivaNaoTemCriteriosDeCorrecao() {
        int objetiva = c.criarEtapa().executar(IDECAN, faseId, etapa("Objetiva", PROVA_OBJETIVA, null)).id();

        assertThrows(RegraDeNegocioException.class, () -> criterios.listar(IDECAN, objetiva));
        assertThrows(RegraDeNegocioException.class,
                () -> criterios.criar(IDECAN, objetiva, pedido("Formal", "Pontuação", null, "1")));
        assertThrows(NaoEncontradoException.class, () -> criterios.listar(IDECAN, 999));
    }

    @Test
    void validaPontuacaoEDesconto() {
        assertThrows(RegraDeNegocioException.class,
                () -> criterios.criar(IDECAN, discursiva, pedido("Formal", "Pontuação", null, "0")));
        assertThrows(RegraDeNegocioException.class,
                () -> criterios.criar(IDECAN, discursiva, pedido("Formal", "Pontuação", "3", "2.5")));
        assertThrows(RegraDeNegocioException.class,
                () -> criterios.criar(IDECAN, discursiva, pedido("Formal", "Pontuação", "0.125", "2.5")));
        assertThrows(RegraDeNegocioException.class,
                () -> criterios.criar(IDECAN, discursiva, pedido(" ", "Pontuação", null, "2.5")));
        assertThrows(RegraDeNegocioException.class,
                () -> criterios.criar(IDECAN, discursiva, new CriterioCorrecaoRequest("Formal", "Pontuação", null, null)));
    }

    @Test
    void excluirRenumeraEReordenarExigeTodos() {
        int a = criterios.criar(IDECAN, discursiva, pedido("Formal", "A", null, "1")).id();
        int b = criterios.criar(IDECAN, discursiva, pedido("Formal", "B", null, "1")).id();
        int d = criterios.criar(IDECAN, discursiva, pedido("Textual", "C", null, "1")).id();

        criterios.reordenar(IDECAN, discursiva, List.of(d, a, b));
        assertEquals(List.of("C", "A", "B"), nomes(discursiva));
        assertThrows(RegraDeNegocioException.class, () -> criterios.reordenar(IDECAN, discursiva, List.of(d, a)));

        criterios.excluir(IDECAN, a);
        assertEquals(List.of(1, 2), criterios.listar(IDECAN, discursiva).stream().map(CriterioCorrecaoResponse::ordem).toList());
    }

    @Test
    void copiarSubstituiOsDoDestino() {
        int outra = c.criarEtapa().executar(IDECAN, faseId, etapa("Discursiva 2", PROVA_DISCURSIVA, null)).id();
        criterios.criar(IDECAN, outra, pedido("Velho", "Some", null, "1"));
        criterios.criar(IDECAN, discursiva, pedido("Formal", "A", "0.25", "2.5"));
        criterios.criar(IDECAN, discursiva, pedido("Textual", "B", null, "1.5"));

        criterios.copiar(IDECAN, discursiva, List.of(outra, discursiva));

        assertEquals(List.of("A", "B"), nomes(outra));
        assertEquals(List.of("A", "B"), nomes(discursiva));
        assertThrows(RegraDeNegocioException.class, () -> criterios.copiar(IDECAN, discursiva, List.of(discursiva)));
    }

    @Test
    void excluirEtapaLevaOsCriterios() {
        criterios.criar(IDECAN, discursiva, pedido("Formal", "A", null, "1"));

        c.excluirEtapa().executar(IDECAN, discursiva);

        assertTrue(c.criteriosCorrecao.daEtapa(IDECAN, discursiva).isEmpty());
    }
}

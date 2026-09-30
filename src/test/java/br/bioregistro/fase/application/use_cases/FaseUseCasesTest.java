package br.bioregistro.fase.application.use_cases;

import br.bioregistro.fase.application.dtos.FaseResponse;
import br.bioregistro.fase.domain.exception.ConflitoException;
import br.bioregistro.fase.domain.exception.NaoEncontradoException;
import br.bioregistro.fase.domain.model.Publicacao;
import org.junit.jupiter.api.Test;

import java.util.List;

import static br.bioregistro.fase.application.use_cases.Cenario.fase;
import static br.bioregistro.fase.domain.model.Instituicao.IDECAN;
import static br.bioregistro.fase.domain.model.Instituicao.IDIB;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FaseUseCasesTest {

    private final Cenario c = new Cenario().comEdital(IDECAN, 10).comEdital(IDIB, 10);

    @Test
    void criaEBuscaFase() {
        FaseResponse criada = c.criarFase().executar(IDECAN, 10, fase("Objetiva", 1));

        FaseResponse buscada = new BuscarFase(c.fases).executar(IDECAN, criada.id());
        assertEquals("Objetiva", buscada.nome());
        assertEquals(10, buscada.editalId());
    }

    @Test
    void faseNasceComResultadoPreliminarEOficial() {
        FaseResponse criada = c.criarFase().executar(IDECAN, 10, fase("Objetiva", 1));

        List<String> tipos = c.publicacoes.listarPorFase(IDECAN, criada.id()).stream()
                .map(p -> p.tipo().valor()).toList();
        assertEquals(List.of("resultado_preliminar", "resultado_oficial"), tipos);
    }

    @Test
    void naoCriaFaseEmEditalInexistente() {
        assertThrows(NaoEncontradoException.class, () -> c.criarFase().executar(IDECAN, 99, fase("Objetiva", 1)));
    }

    @Test
    void naoRepeteOrdemNoMesmoEdital() {
        c.criarFase().executar(IDECAN, 10, fase("Objetiva", 1));
        assertThrows(ConflitoException.class, () -> c.criarFase().executar(IDECAN, 10, fase("Discursiva", 1)));
    }

    @Test
    void mesmaOrdemEmInstituicoesDiferentesNaoConflita() {
        c.criarFase().executar(IDECAN, 10, fase("Objetiva", 1));
        c.criarFase().executar(IDIB, 10, fase("Objetiva", 1));
        assertEquals(1, new ListarFases(c.fases).executar(IDIB, 10).size());
    }

    @Test
    void listaEmOrdemCrescente() {
        c.criarFase().executar(IDECAN, 10, fase("Segunda", 2));
        c.criarFase().executar(IDECAN, 10, fase("Primeira", 1));

        List<FaseResponse> fases = new ListarFases(c.fases).executar(IDECAN, 10);
        assertEquals(List.of("Primeira", "Segunda"), fases.stream().map(FaseResponse::nome).toList());
    }

    @Test
    void atualizaMantendoEditalEPermitindoAPropriaOrdem() {
        FaseResponse criada = c.criarFase().executar(IDECAN, 10, fase("Objetiva", 1));

        FaseResponse atualizada = new AtualizarFase(c.fases).executar(IDECAN, criada.id(), fase("Objetiva 2", 1));
        assertEquals("Objetiva 2", atualizada.nome());
        assertEquals(10, atualizada.editalId());
    }

    @Test
    void atualizarParaOrdemDeOutraFaseConflita() {
        c.criarFase().executar(IDECAN, 10, fase("Primeira", 1));
        FaseResponse segunda = c.criarFase().executar(IDECAN, 10, fase("Segunda", 2));

        assertThrows(ConflitoException.class,
                () -> new AtualizarFase(c.fases).executar(IDECAN, segunda.id(), fase("Segunda", 1)));
    }

    @Test
    void excluiFaseESeusEspacosVazios() {
        FaseResponse criada = c.criarFase().executar(IDECAN, 10, fase("Objetiva", 1));

        c.excluirFase().executar(IDECAN, criada.id());
        assertTrue(c.fases.buscar(IDECAN, criada.id()).isEmpty());
        assertTrue(c.publicacoes.listarPorFase(IDECAN, criada.id()).isEmpty());
    }

    @Test
    void naoExcluiFaseComPublicacaoPreenchida() {
        FaseResponse criada = c.criarFase().executar(IDECAN, 10, fase("Objetiva", 1));
        Publicacao resultado = c.publicacoes.listarPorFase(IDECAN, criada.id()).get(0);
        c.publicacoes.salvar(IDECAN, resultado.comDatas(Cenario.ABERTURA, null, null, null, null));

        assertThrows(ConflitoException.class, () -> c.excluirFase().executar(IDECAN, criada.id()));
    }

    @Test
    void operacoesEmFaseInexistenteDao404() {
        assertThrows(NaoEncontradoException.class, () -> new BuscarFase(c.fases).executar(IDECAN, 1));
        assertThrows(NaoEncontradoException.class,
                () -> new AtualizarFase(c.fases).executar(IDECAN, 1, fase("X", 1)));
        assertThrows(NaoEncontradoException.class, () -> c.excluirFase().executar(IDECAN, 1));
    }
}

package br.bioregistro.fase.application.use_cases;

import br.bioregistro.fase.application.dtos.CargoResponse;
import br.bioregistro.fase.application.dtos.FaseGeradaResponse;
import br.bioregistro.fase.application.dtos.FaseResponse;
import br.bioregistro.fase.application.dtos.GerarFasesRequest;
import br.bioregistro.fase.application.dtos.GerarFasesRequest.EtapaGerada;
import br.bioregistro.fase.application.dtos.GerarFasesRequest.FaseGerada;
import br.bioregistro.fase.application.dtos.GrupoCargoRequest;
import br.bioregistro.fase.application.dtos.GrupoCargoResponse;
import br.bioregistro.fase.domain.exception.ConflitoException;
import br.bioregistro.fase.domain.exception.NaoEncontradoException;
import br.bioregistro.fase.domain.exception.RegraDeNegocioException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static br.bioregistro.fase.application.use_cases.Cenario.ABERTURA;
import static br.bioregistro.fase.application.use_cases.Cenario.FECHAMENTO;
import static br.bioregistro.fase.application.use_cases.Cenario.fase;
import static br.bioregistro.fase.application.use_cases.TiposEmMemoria.PROVA_OBJETIVA;
import static br.bioregistro.fase.application.use_cases.TiposEmMemoria.TAF;
import static br.bioregistro.fase.domain.model.Instituicao.IDECAN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GrupoCargoUseCasesTest {

    private final Cenario c = new Cenario().comEdital(IDECAN, 10);

    GrupoCargoUseCasesTest() {
        c.grupos.comCargos(10, 101, 102, 103);
    }

    private int grupo(String nome) {
        return new CriarGrupoCargo(c.fases, c.grupos).executar(IDECAN, 10, new GrupoCargoRequest(nome)).id();
    }

    private void adicionar(int grupoId, int cargoId) {
        new AdicionarCargoAoGrupo(c.grupos).executar(IDECAN, grupoId, cargoId);
    }

    private static GerarFasesRequest pedido(String faseNome, Integer... tipos) {
        List<EtapaGerada> etapas = java.util.Arrays.stream(tipos).map(t -> new EtapaGerada(t, null)).toList();
        return new GerarFasesRequest(List.of(new FaseGerada(faseNome, null, ABERTURA, FECHAMENTO, etapas)));
    }

    @Test
    void nomeDoGrupoNaoSeRepeteNoEdital() {
        grupo("Nível médio");
        assertThrows(ConflitoException.class, () -> grupo("Nível médio"));
    }

    @Test
    void usuarioEscolheOsCargosDoGrupo() {
        int medio = grupo("Nível médio");
        adicionar(medio, 101);
        adicionar(medio, 103);

        List<GrupoCargoResponse> grupos = new ListarGruposCargo(c.fases, c.grupos).executar(IDECAN, 10);
        assertEquals(List.of(101, 103), grupos.get(0).cargoIds());
    }

    @Test
    void cargoEmOutroGrupoNaoPodeSerIncluido() {
        int medio = grupo("Nível médio");
        int superior = grupo("Nível superior");
        adicionar(medio, 101);

        assertThrows(ConflitoException.class, () -> adicionar(superior, 101));
    }

    @Test
    void cargoPodeMudarDeGrupoSaindoDoAnterior() {
        int medio = grupo("Nível médio");
        int superior = grupo("Nível superior");
        adicionar(medio, 101);

        new RemoverCargoDoGrupo(c.grupos).executar(IDECAN, medio, 101);
        adicionar(superior, 101);
        assertEquals(List.of(101), c.grupos.cargosDoGrupo(IDECAN, superior));
    }

    @Test
    void cargoDeOutroEditalNaoEntra() {
        int medio = grupo("Nível médio");
        assertThrows(NaoEncontradoException.class, () -> adicionar(medio, 999));
    }

    @Test
    void listaCargosAindaSemGrupo() {
        int medio = grupo("Nível médio");
        adicionar(medio, 102);

        List<CargoResponse> soltos = new ListarCargosSemGrupo(c.fases, c.grupos).executar(IDECAN, 10);
        assertEquals(List.of(101, 103), soltos.stream().map(CargoResponse::id).toList());
    }

    @Test
    void naoGeraFasesParaGrupoSemCargos() {
        int medio = grupo("Nível médio");
        assertThrows(RegraDeNegocioException.class,
                () -> c.gerarFases().executar(IDECAN, medio, pedido("Fase 1", PROVA_OBJETIVA)));
    }

    @Test
    void geraFasesEtapasEEspacosDePublicacao() {
        int medio = grupo("Nível médio");
        adicionar(medio, 101);

        List<FaseGeradaResponse> geradas =
                c.gerarFases().executar(IDECAN, medio, pedido("Fase 1", PROVA_OBJETIVA, TAF));

        FaseGeradaResponse fase = geradas.get(0);
        assertEquals(List.of("Prova objetiva", "Teste de aptidão física (TAF)"),
                fase.etapas().stream().map(e -> e.nome()).toList());
        // 2 da fase + 5 da prova objetiva + 2 do TAF
        assertEquals(9, c.publicacoes.listarPorFase(IDECAN, fase.fase().id()).size());
        assertEquals(List.of(fase.fase().id()),
                new ListarFasesDoGrupo(c.fases, c.grupos).executar(IDECAN, medio).stream()
                        .map(FaseResponse::id).toList());
    }

    @Test
    void gruposDoMesmoEditalTemEtapasDiferentesEOrdemContinua() {
        c.criarFase().executar(IDECAN, 10, fase("Fase antiga", 1));
        int medio = grupo("Nível médio");
        int superior = grupo("Nível superior");
        adicionar(medio, 101);
        adicionar(superior, 102);

        FaseGeradaResponse doMedio = c.gerarFases().executar(IDECAN, medio, pedido("Médio", PROVA_OBJETIVA)).get(0);
        FaseGeradaResponse doSuperior =
                c.gerarFases().executar(IDECAN, superior, pedido("Superior", PROVA_OBJETIVA, TAF)).get(0);

        assertEquals(1, doMedio.etapas().size());
        assertEquals(2, doSuperior.etapas().size());
        assertEquals((short) 2, doMedio.fase().ordem());
        assertEquals((short) 3, doSuperior.fase().ordem());
    }

    @Test
    void grupoComFasesNaoPodeSerExcluido() {
        int medio = grupo("Nível médio");
        adicionar(medio, 101);
        c.gerarFases().executar(IDECAN, medio, pedido("Fase 1", PROVA_OBJETIVA));

        assertThrows(ConflitoException.class, () -> new ExcluirGrupoCargo(c.grupos).executar(IDECAN, medio));
    }

    @Test
    void excluirGrupoSemFasesSoltaOsCargos() {
        int medio = grupo("Nível médio");
        adicionar(medio, 101);

        new ExcluirGrupoCargo(c.grupos).executar(IDECAN, medio);
        assertTrue(c.grupos.grupoDoCargo(IDECAN, 101).isEmpty());
    }
}

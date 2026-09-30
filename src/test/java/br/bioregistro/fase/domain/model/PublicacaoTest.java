package br.bioregistro.fase.domain.model;

import br.bioregistro.fase.domain.exception.ConflitoException;
import br.bioregistro.fase.domain.exception.RegraDeNegocioException;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PublicacaoTest {

    private static final TipoPublicacao GABARITO = new TipoPublicacao(2, "gabarito_preliminar", "Gabarito", true);
    private static final TipoPublicacao LOCAL = new TipoPublicacao(1, "local_prova", "Local de prova", false);

    // Exemplo da resposta 5: gabarito em 21/10, recurso de 22/10 00:00 a 30/10 23:59.
    private static final OffsetDateTime GABARITO_21 = OffsetDateTime.parse("2026-10-21T09:00:00-03:00");
    private static final OffsetDateTime RECURSO_INICIO = OffsetDateTime.parse("2026-10-22T00:00:00-03:00");
    private static final OffsetDateTime RECURSO_FIM = OffsetDateTime.parse("2026-10-30T23:59:00-03:00");
    private static final OffsetDateTime VIS_FIM = OffsetDateTime.parse("2026-12-31T23:59:00-03:00");

    private static Publicacao gabaritoPublicado() {
        return Publicacao.espaco(1, 1, GABARITO)
                .comDatas(null, GABARITO_21, VIS_FIM, RECURSO_INICIO, RECURSO_FIM)
                .publicar(GABARITO_21);
    }

    @Test
    void espacoNasceVazio() {
        assertFalse(Publicacao.espaco(1, 1, GABARITO).preenchida());
    }

    @Test
    void recursoAbertoSoDentroDoPeriodoInformado() {
        Publicacao publicada = gabaritoPublicado();

        assertFalse(publicada.recursoAberto(GABARITO_21));
        assertTrue(publicada.recursoAberto(RECURSO_INICIO));
        assertTrue(publicada.recursoAberto(RECURSO_FIM));
        assertFalse(publicada.recursoAberto(RECURSO_FIM.plusMinutes(1)));
    }

    @Test
    void recursoNaoAbreSemPublicar() {
        Publicacao naoPublicada = Publicacao.espaco(1, 1, GABARITO)
                .comDatas(null, GABARITO_21, VIS_FIM, RECURSO_INICIO, RECURSO_FIM);
        assertFalse(naoPublicada.recursoAberto(RECURSO_INICIO));
    }

    @Test
    void publicarExigeVisualizacao() {
        assertThrows(RegraDeNegocioException.class, () -> Publicacao.espaco(1, null, LOCAL).publicar(GABARITO_21));
    }

    @Test
    void naoPublicaDuasVezes() {
        assertThrows(ConflitoException.class, () -> gabaritoPublicado().publicar(GABARITO_21));
    }

    @Test
    void periodoPrecisaDeInicioEFimNaOrdem() {
        Publicacao espaco = Publicacao.espaco(1, 1, GABARITO);
        assertThrows(RegraDeNegocioException.class,
                () -> espaco.comDatas(null, GABARITO_21, null, null, null));
        assertThrows(RegraDeNegocioException.class,
                () -> espaco.comDatas(null, VIS_FIM, GABARITO_21, null, null));
    }

    @Test
    void tipoQueNaoAbreRecursoRecusaPeriodoDeRecurso() {
        assertThrows(RegraDeNegocioException.class,
                () -> Publicacao.espaco(1, 1, LOCAL).comDatas(null, null, null, RECURSO_INICIO, RECURSO_FIM));
    }
}

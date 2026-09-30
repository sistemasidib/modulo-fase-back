package br.bioregistro.fase.domain.model;

import br.bioregistro.fase.domain.exception.RegraDeNegocioException;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FaseTest {

    private static final OffsetDateTime ABERTURA = OffsetDateTime.parse("2026-10-01T00:00:00-03:00");
    private static final OffsetDateTime FECHAMENTO = OffsetDateTime.parse("2026-10-31T23:59:00-03:00");

    @Test
    void criaFaseValidaComNomeSemEspacosNasPontas() {
        Fase fase = new Fase(null, 10, "  Fase objetiva  ", null, (short) 1, ABERTURA, FECHAMENTO);
        assertEquals("Fase objetiva", fase.nome());
    }

    @Test
    void exigeEdital() {
        assertThrows(RegraDeNegocioException.class,
                () -> new Fase(null, null, "Fase", null, (short) 1, ABERTURA, FECHAMENTO));
    }

    @Test
    void exigeNome() {
        assertThrows(RegraDeNegocioException.class,
                () -> new Fase(null, 10, "  ", null, (short) 1, ABERTURA, FECHAMENTO));
    }

    @Test
    void limitaNomeA150Caracteres() {
        String nome = "x".repeat(Fase.NOME_MAX + 1);
        assertThrows(RegraDeNegocioException.class,
                () -> new Fase(null, 10, nome, null, (short) 1, ABERTURA, FECHAMENTO));
    }

    @Test
    void exigeOrdemPositiva() {
        assertThrows(RegraDeNegocioException.class,
                () -> new Fase(null, 10, "Fase", null, (short) 0, ABERTURA, FECHAMENTO));
    }

    @Test
    void exigeDatas() {
        assertThrows(RegraDeNegocioException.class,
                () -> new Fase(null, 10, "Fase", null, (short) 1, null, FECHAMENTO));
    }

    @Test
    void recusaFechamentoAntesDaAbertura() {
        assertThrows(RegraDeNegocioException.class,
                () -> new Fase(null, 10, "Fase", null, (short) 1, FECHAMENTO, ABERTURA));
    }
}

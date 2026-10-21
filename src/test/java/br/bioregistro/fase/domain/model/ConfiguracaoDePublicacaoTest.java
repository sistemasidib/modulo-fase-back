package br.bioregistro.fase.domain.model;

import br.bioregistro.fase.domain.exception.RegraDeNegocioException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ConfiguracaoDePublicacaoTest {

    @Test
    void diasNoArDevemSerPositivos() {
        assertThrows(RegraDeNegocioException.class,
                () -> new ConfiguracaoDePublicacao(1, "E15", 1, 0, FormatoDePublicacao.LISTA_PDF));
    }

    @Test
    void fimDaVisualizacaoSomaDiasNoAr() {
        ConfiguracaoDePublicacao c = new ConfiguracaoDePublicacao(1, "E15", 1, 5, FormatoDePublicacao.LISTA_PDF);
        assertEquals(LocalDate.of(2026, 10, 25), c.fimDaVisualizacao(LocalDate.of(2026, 10, 21)));
    }
}

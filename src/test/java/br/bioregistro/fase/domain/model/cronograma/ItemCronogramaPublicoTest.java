package br.bioregistro.fase.domain.model.cronograma;

import br.bioregistro.fase.domain.exception.RegraDeNegocioException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ItemCronogramaPublicoTest {

    @Test
    void aceitaMesmoDiaEmInicioETermino() {
        ItemCronogramaPublico item = new ItemCronogramaPublico(1, "Publicação",
                LocalDate.of(2026, 5, 19), LocalDate.of(2026, 5, 19), false);
        assertEquals(LocalDate.of(2026, 5, 19), item.inicio());
        assertEquals(item.inicio(), item.termino());
    }

    @Test
    void rejeitaTerminoAntesDoInicio() {
        assertThrows(RegraDeNegocioException.class, () -> new ItemCronogramaPublico(1, "X",
                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 5, 1), false));
    }

    @Test
    void rejeitaDescricaoVazia() {
        assertThrows(RegraDeNegocioException.class, () -> new ItemCronogramaPublico(1, "  ",
                LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 1), false));
    }
}

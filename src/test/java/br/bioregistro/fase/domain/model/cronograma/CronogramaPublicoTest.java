package br.bioregistro.fase.domain.model.cronograma;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CronogramaPublicoTest {

    @Test
    void excluiModuloDesligadoEItemPersonalizado() {
        EtapaCalculada publica = linha("E16", "Gabarito", true, false);
        EtapaCalculada interna = linha("X01", "Reunião interna", true, true);
        CronogramaCalculado.Grupo s12Off = new CronogramaCalculado.Grupo("S12", "Discursiva", true, "S12", null,
                false, List.of(linha("E21", "Resultado", false, false)));
        CronogramaCalculado.Grupo nucleo = new CronogramaCalculado.Grupo("F1", "Prova", true, null, null,
                true, List.of(publica, interna));
        CronogramaCalculado calc = new CronogramaCalculado(true, LocalDate.of(2026, 1, 1), true,
                List.of(nucleo, s12Off),
                new CronogramaCalculado.Indicadores(1, 3, LocalDate.of(2026, 1, 1), 0, null, 0, null),
                new CronogramaCalculado.Checagens(false, List.of(), List.of()));

        CronogramaPublico publico = CronogramaPublico.de("Concurso X", List.of("Edital 1"),
                OffsetDateTime.parse("2026-10-20T12:00:00-03:00"), calc);

        assertEquals(1, publico.itens().size());
        assertEquals("Gabarito", publico.itens().getFirst().descricao());
        assertEquals(1, publico.itens().getFirst().ordem());
        assertTrue(publico.itens().stream().noneMatch(i -> i.descricao().contains("Reunião")));
        assertEquals("Concurso X", publico.nomeConcurso());
        assertEquals(List.of("Edital 1"), publico.editais());
    }

    @Test
    void deItensPreservaOrdemEDestaque() {
        CronogramaPublico publico = CronogramaPublico.deItens(
                "Edital 9",
                List.of("Edital 9"),
                OffsetDateTime.parse("2026-10-20T12:00:00-03:00"),
                List.of(new ItemCronogramaPublico(1, "Período de inscrições",
                        LocalDate.of(2026, 5, 19), LocalDate.of(2026, 6, 22), true)));
        assertEquals(1, publico.itens().size());
        assertTrue(publico.itens().getFirst().destaque());
    }

    private static EtapaCalculada linha(String codigo, String nome, boolean ativa, boolean personalizada) {
        return new EtapaCalculada(codigo, nome, "F1", "INICIO", "E15", TipoStatus.PUBLICACAO,
                1, 1, null, null, 0, 0, 0, 0, 0,
                LocalDate.of(2026, 10, 21), LocalDate.of(2026, 10, 21),
                ativa, false, true, false, true, false, null, false, null, List.of(), personalizada);
    }
}

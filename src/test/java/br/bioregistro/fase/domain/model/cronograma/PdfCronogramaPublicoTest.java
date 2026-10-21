package br.bioregistro.fase.domain.model.cronograma;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PdfCronogramaPublicoTest {

    @Test
    void geraPdfAnexoIi() {
        CronogramaPublico publico = new CronogramaPublico(
                "Concurso X",
                List.of("Edital 1"),
                OffsetDateTime.of(2026, 10, 20, 12, 0, 0, 0, ZoneOffset.of("-03:00")),
                List.of(new CronogramaPublico.Item(1, "Publicacao do edital",
                                LocalDate.of(2026, 5, 19), LocalDate.of(2026, 5, 19), false),
                        new CronogramaPublico.Item(2, "Periodo de inscricoes",
                                LocalDate.of(2026, 5, 19), LocalDate.of(2026, 6, 22), true)));

        byte[] pdf = PdfCronogramaPublico.gerar(publico);
        String texto = new String(pdf, StandardCharsets.ISO_8859_1);
        assertTrue(texto.startsWith("%PDF-1.4"));
        assertTrue(texto.contains("ANEXO II"));
        assertTrue(texto.contains("Periodo de inscricoes") || texto.contains("inscricoes"));
        assertTrue(texto.contains("19/05/2026"));
        assertTrue(texto.contains("%%EOF"));
    }
}

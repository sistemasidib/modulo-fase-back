package br.bioregistro.fase.application.use_cases;

import br.bioregistro.fase.domain.exception.NaoEncontradoException;
import br.bioregistro.fase.domain.model.cronograma.CronogramaPublico;
import br.bioregistro.fase.domain.model.cronograma.ItemCronogramaPublico;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static br.bioregistro.fase.domain.model.Instituicao.IDECAN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Spec 008 — RNs claras do Anexo II (sem D+n / P−n). */
class Spec008CronogramaPublicoTest {

    private static final Clock RELOGIO =
            Clock.fixed(Instant.parse("2026-10-26T15:00:00Z"), ZoneId.of("America/Sao_Paulo"));

    private final FasesEmMemoria fases = new FasesEmMemoria().comEdital(IDECAN, 10, "PM-XX 2027");
    private final CronogramaPublicoItensEmMemoria itens = new CronogramaPublicoItensEmMemoria();
    private final ItensDoCronogramaPublico casos = new ItensDoCronogramaPublico(itens, fases, RELOGIO);

    @BeforeEach
    void limpar() {
        itens.substituir(IDECAN, 10, List.of());
    }

    @Test
    void retratoUsaNomeDoConcursoNoEdital() {
        casos.salvar(IDECAN, 10, List.of(item("Publicacao do edital", "2026-05-19", "2026-05-19", false)));

        CronogramaPublico retrato = casos.retrato(IDECAN, 10);
        assertEquals("PM-XX 2027", retrato.nomeConcurso());
        assertEquals(List.of("Edital 10"), retrato.editais());
        assertEquals(1, retrato.itens().size());
    }

    @Test
    void pdfContemNomeDoConcursoEDatasAbsolutas() {
        casos.salvar(IDECAN, 10, List.of(
                item("Publicacao do edital", "2026-05-19", "2026-05-19", false),
                item("Periodo de inscricoes", "2026-05-19", "2026-06-22", true)));

        String pdf = new String(casos.pdf(IDECAN, 10), StandardCharsets.ISO_8859_1);
        assertTrue(pdf.startsWith("%PDF-1.4"));
        assertTrue(pdf.contains("PM-XX 2027"));
        assertTrue(pdf.contains("ANEXO II"));
        assertTrue(pdf.contains("19/05/2026"));
        assertTrue(pdf.contains("22/06/2026"));
    }

    @Test
    void salvarSubstituiListaInteiraEPreservaOrdem() {
        casos.salvar(IDECAN, 10, List.of(item("A", "2026-01-01", "2026-01-01", false)));
        List<ItemCronogramaPublico> salvos = casos.salvar(IDECAN, 10, List.of(
                item("Primeiro", "2026-02-01", "2026-02-02", false),
                item("Segundo", "2026-03-01", "2026-03-01", true)));

        assertEquals(2, salvos.size());
        assertEquals(1, salvos.get(0).ordem());
        assertEquals("Primeiro", salvos.get(0).descricao());
        assertEquals(2, salvos.get(1).ordem());
        assertTrue(salvos.get(1).destaque());
        assertEquals(2, casos.listar(IDECAN, 10).size());
    }

    @Test
    void salvarListaVaziaLimpaOCronograma() {
        casos.salvar(IDECAN, 10, List.of(item("A", "2026-01-01", "2026-01-01", false)));
        assertEquals(0, casos.salvar(IDECAN, 10, List.of()).size());
        assertTrue(casos.listar(IDECAN, 10).isEmpty());
    }

    @Test
    void editalInexistenteRetorna404() {
        assertThrows(NaoEncontradoException.class, () -> casos.listar(IDECAN, 999));
        assertThrows(NaoEncontradoException.class,
                () -> casos.salvar(IDECAN, 999, List.of(item("X", "2026-01-01", "2026-01-01", false))));
    }

    private static ItemCronogramaPublico item(String descricao, String inicio, String termino, boolean destaque) {
        return new ItemCronogramaPublico(1, descricao, LocalDate.parse(inicio), LocalDate.parse(termino), destaque);
    }
}

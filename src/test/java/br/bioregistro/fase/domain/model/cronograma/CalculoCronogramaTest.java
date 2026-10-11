package br.bioregistro.fase.domain.model.cronograma;

import br.bioregistro.fase.domain.exception.RegraDeNegocioException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Critérios CB03–CB06, CB08 e CB09 da spec 003-B. */
class CalculoCronogramaTest {

    private static final Esqueleto ESQ = EsqueletoIdecanV1.ESQUELETO;
    private static final Parametros PADRAO = Parametros.padroes();
    private static final LocalDate HOJE = LocalDate.of(2026, 10, 10);

    private static Edicao edital(LocalDate data) {
        return new Edicao(Set.of(), data, Map.of(), Set.of(), Set.of());
    }

    private static CronogramaCalculado calcular(Edicao edicao) {
        return CalculoCronograma.calcular(ESQ, PADRAO, edicao, HOJE);
    }

    private static EtapaCalculada etapa(CronogramaCalculado c, String codigo) {
        return c.etapa(codigo).orElseThrow();
    }

    @Test
    void esqueletoTem54EtapasComReferenciasParaTras() {
        assertEquals(54, ESQ.etapas().size());
        Set<String> vistas = new java.util.HashSet<>();
        for (EtapaModelo e : ESQ.etapas()) {
            if (e.referencia().codigo() != null) {
                assertTrue(vistas.contains(e.referencia().codigo()), e.codigo() + " referencia etapa posterior");
            }
            vistas.add(e.codigo());
        }
    }

    @Test
    void cb03_fase1EhProvaObjetivaEInscricoesFicamNoBlocoAnterior() {
        CronogramaCalculado c = calcular(Edicao.inicial());
        CronogramaCalculado.Grupo primeiro = c.grupos().get(0);
        CronogramaCalculado.Grupo fase1 = c.grupos().get(1);
        assertEquals(EsqueletoIdecanV1.EDITAL_INSCRICOES, primeiro.codigo());
        assertFalse(primeiro.fase());
        assertEquals(EsqueletoIdecanV1.F1_PROVA_OBJETIVA, fase1.codigo());
        assertTrue(fase1.fase());
        assertEquals(List.of("E14", "E15", "E16", "E17", "E18", "E19", "E20"),
                fase1.etapas().stream().map(EtapaCalculada::codigo).toList());
        assertTrue(primeiro.etapas().stream().anyMatch(e -> e.codigo().equals("E04")));
    }

    @Test
    void cb04_edital01Dez2026() {
        CronogramaCalculado c = calcular(edital(LocalDate.of(2026, 12, 1)));
        assertTrue(c.materializado());
        assertEquals(LocalDate.of(2026, 12, 2), etapa(c, "E04").inicio());
        assertEquals(LocalDate.of(2027, 1, 2), etapa(c, "E04").fim());
        assertEquals(LocalDate.of(2027, 2, 14), etapa(c, "E15").inicio());
        assertEquals(0, c.ajusteDomingo());
        assertEquals(LocalDate.of(2027, 3, 1), etapa(c, "E16").inicio());
        assertEquals(LocalDate.of(2027, 3, 1), etapa(c, "E17").inicio());
        assertEquals(LocalDate.of(2027, 3, 3), etapa(c, "E17").fim());
    }

    @Test
    void cb05_provaLevadaAoDomingoSeguinte() {
        CronogramaCalculado c = calcular(edital(LocalDate.of(2026, 12, 3)));
        assertEquals(LocalDate.of(2027, 1, 4), etapa(c, "E04").fim());
        assertEquals(LocalDate.of(2027, 2, 21), etapa(c, "E15").inicio());
        assertEquals(5, c.ajusteDomingo());
        assertTrue(etapa(c, "E15").avisos().get(0).startsWith("+5 dias para cair no domingo"));
        assertEquals(LocalDate.of(2027, 3, 8), etapa(c, "E16").inicio());
        assertEquals(LocalDate.of(2027, 3, 8), etapa(c, "E17").inicio());
        assertEquals(LocalDate.of(2027, 3, 10), etapa(c, "E17").fim());
    }

    @Test
    void cb06_rascunhoEmDmaisN() {
        CronogramaCalculado c = calcular(Edicao.inicial());
        assertFalse(c.materializado());
        assertEquals(1, etapa(c, "E04").inicioRelativo());
        assertNull(etapa(c, "E04").inicio());
    }

    @Test
    void cb06_gabarito15DiasDepoisERecursoDe2Dias() {
        // prova em 10/08: forçar a E15 nessa data a partir de um edital qualquer
        CronogramaCalculado base = calcular(edital(LocalDate.of(2027, 5, 1)));
        int provaRel = etapa(base, "E15").inicioRelativo();
        LocalDate edital = LocalDate.of(2027, 8, 10).minusDays(provaRel);
        CronogramaCalculado c = calcular(edital(edital));
        EtapaCalculada prova = etapa(c, "E15");
        LocalDate dataProva = prova.inicio();
        assertEquals(dataProva.plusDays(15), etapa(c, "E16").inicio());
        assertEquals(etapa(c, "E16").inicio(), etapa(c, "E17").inicio());
        assertEquals(dataProva.plusDays(17), etapa(c, "E17").fim());
    }

    @Test
    void valoresDaRegraVemDosParametros() {
        Parametros dez = new Parametros(Map.of(Parametros.GABARITO_APOS_PROVA, 10));
        CronogramaCalculado c = CalculoCronograma.calcular(ESQ, dez, edital(LocalDate.of(2026, 12, 1)), HOJE);
        assertEquals(LocalDate.of(2027, 2, 24), etapa(c, "E16").inicio());
    }

    @Test
    void moduloDesligadoDeixaGrupoInativoES05DesligadoAtivaPcd() {
        CronogramaCalculado c = calcular(Edicao.inicial());
        assertFalse(grupo(c, "S12").ativo());
        assertTrue(grupo(c, GrupoModelo.S05_DESLIGADO).ativo());
        assertFalse(grupo(c, "S05").ativo());
        assertEquals(26, c.indicadores().etapasAtivas());

        CronogramaCalculado comS05 = calcular(new Edicao(Set.of("S05", "S12"), null, Map.of(), Set.of(), Set.of()));
        assertTrue(grupo(comS05, "S05").ativo());
        assertFalse(grupo(comS05, GrupoModelo.S05_DESLIGADO).ativo());
        assertTrue(grupo(comS05, "S12").ativo());
    }

    @Test
    void resultadoFinalContaDoUltimoResultadoDefinitivoLigado() {
        CronogramaCalculado nucleo = calcular(Edicao.inicial());
        int ultimo = Math.max(etapa(nucleo, "E20").inicioRelativo(), etapa(nucleo, "E53").inicioRelativo());
        assertEquals(ultimo + 4, etapa(nucleo, "E60").inicioRelativo());
    }

    @Test
    void cb08_cascataMoveAsDependentes() {
        Edicao edicao = edital(LocalDate.of(2026, 12, 1));
        Previa previa = CalculoCronograma.prever(ESQ, PADRAO, edicao,
                new Mudanca("E04", Mudanca.Campo.DURACAO, 25, Mudanca.Modo.CASCATA));
        assertTrue(previa.etapasMovidas() > 0);
        assertEquals("E04", previa.itens().get(0).etapaCodigo());
        assertEquals("31", previa.itens().get(0).antes());
        assertTrue(previa.itens().stream().anyMatch(i -> i.etapaCodigo().equals("E14")));
        // fim 02/01 → 27/12: prova 08/02 (seg) volta ao domingo 14/02, então a E15 não se move
        assertTrue(previa.itens().stream().noneMatch(i -> i.etapaCodigo().equals("E15")));
        assertEquals(25, previa.edicao().ajuste("E04").duracao());
    }

    @Test
    void cb08_soEstaCompensaAsDependentesEMantemAsDatas() {
        Edicao edicao = edital(LocalDate.of(2026, 12, 1));
        Previa previa = CalculoCronograma.prever(ESQ, PADRAO, edicao,
                new Mudanca("E04", Mudanca.Campo.DURACAO, 25, Mudanca.Modo.SO_ESTA));
        assertEquals(0, previa.etapasMovidas());
        CronogramaCalculado antes = calcular(edicao);
        CronogramaCalculado depois = calcular(previa.edicao());
        for (EtapaCalculada e : antes.etapas()) {
            if (!e.codigo().equals("E04")) {
                assertEquals(e.inicio(), etapa(depois, e.codigo()).inicio(), e.codigo());
            }
        }
        assertTrue(previa.itens().stream().anyMatch(i -> i.campo().equals("dias (compensado)")));
    }

    @Test
    void valorIgualAoModeloApagaOAjuste() {
        Edicao alterada = edital(null).comAjuste("E14", new Edicao.Ajuste(30, null));
        assertTrue(etapa(calcular(alterada), "E14").alterada());
        Previa volta = CalculoCronograma.prever(ESQ, PADRAO, alterada,
                new Mudanca("E14", Mudanca.Campo.DIAS, 35, Mudanca.Modo.CASCATA));
        assertNull(volta.edicao().ajuste("E14"));
    }

    @Test
    void prazoPadraoTravadoSoMudaComExcecao() {
        Mudanca isencao = new Mudanca("E05", Mudanca.Campo.DURACAO, 3, Mudanca.Modo.CASCATA);
        assertThrows(RegraDeNegocioException.class,
                () -> CalculoCronograma.prever(ESQ, PADRAO, Edicao.inicial(), isencao));
        assertTrue(etapa(calcular(Edicao.inicial()), "E05").duracaoTravada());

        Edicao comExcecao = new Edicao(Set.of(), null, Map.of(), Set.of(), Set.of(Parametros.ISENCAO));
        Previa previa = CalculoCronograma.prever(ESQ, PADRAO, comExcecao, isencao);
        assertEquals(3, previa.edicao().ajuste("E05").duracao());
    }

    @Test
    void cb09_checagens() {
        CronogramaCalculado viola = calcular(edital(LocalDate.of(2026, 12, 1)));
        assertTrue(viola.checagens().viola());
        assertFalse(checagem(viola, "RM-01").cumprida());
        assertEquals("43 dias", checagem(viola, "RM-01").detalhe());

        CronogramaCalculado ok = calcular(edital(LocalDate.of(2026, 12, 3)));
        assertFalse(ok.checagens().viola());
        assertEquals("48 dias", checagem(ok, "RM-01").detalhe());
    }

    @Test
    void editalNoPassadoViolaAChecagem() {
        CronogramaCalculado c = calcular(edital(LocalDate.of(2026, 10, 9)));
        assertFalse(checagem(c, "EDITAL_NO_PRAZO").cumprida());
    }

    @Test
    void avisaFimDeSemanaEFaixaHistorica() {
        // edital 01/12/2026 (ter): E04 começa 02/12; E10 = fim E04 + 1 = 03/01/2027 (dom)
        CronogramaCalculado c = calcular(edital(LocalDate.of(2026, 12, 1)));
        assertTrue(etapa(c, "E10").avisos().stream().anyMatch(a -> a.startsWith("cai em dom")));

        Edicao fora = edital(null).comAjuste("E14", new Edicao.Ajuste(60, null));
        assertTrue(etapa(calcular(fora), "E14").avisos().stream().anyMatch(a -> a.startsWith("fora da faixa")));
    }

    @Test
    void removidaContinuaNaCadeia() {
        Edicao semE03 = new Edicao(Set.of(), null, Map.of(), Set.of("E03"), Set.of());
        CronogramaCalculado c = calcular(semE03);
        assertTrue(etapa(c, "E03").removida());
        assertFalse(etapa(c, "E03").ativa());
        assertEquals(etapa(calcular(Edicao.inicial()), "E04").inicioRelativo(), etapa(c, "E04").inicioRelativo());
    }

    private static CronogramaCalculado.Grupo grupo(CronogramaCalculado c, String codigo) {
        return c.grupos().stream().filter(g -> g.codigo().equals(codigo)).findFirst().orElseThrow();
    }

    private static CronogramaCalculado.Checagem checagem(CronogramaCalculado c, String codigo) {
        return c.checagens().itens().stream().filter(i -> i.codigo().equals(codigo)).findFirst().orElseThrow();
    }
}

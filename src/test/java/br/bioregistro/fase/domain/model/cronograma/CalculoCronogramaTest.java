package br.bioregistro.fase.domain.model.cronograma;

import br.bioregistro.fase.domain.exception.RegraDeNegocioException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

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
    void esqueletoTem54EtapasComReferenciasExistentes() {
        assertEquals(54, ESQ.etapas().size());
        for (EtapaModelo e : ESQ.etapas()) {
            if (e.referencia().codigo() != null) {
                assertTrue(ESQ.existe(e.referencia().codigo()), e.codigo() + " referencia etapa inexistente");
            }
        }
    }

    @Test
    void referenciaCircularNoEsqueletoEhRecusada() {
        GrupoModelo g = new GrupoModelo("G", "Grupo", true, null, null);
        Esqueleto circular = new Esqueleto("circular", List.of(g), List.of(
                new EtapaModelo("E14", "a", "G", Referencia.inicio("E15"), -9, null, null, 0, 0,
                        TipoStatus.PUBLICACAO, null, null, false),
                new EtapaModelo("E15", "b", "G", Referencia.inicio("E14"), 9, null, null, 0, 0,
                        TipoStatus.ATIVIDADE, null, null, false)), Map.of(), Map.of());
        IllegalStateException erro = assertThrows(IllegalStateException.class,
                () -> CalculoCronograma.calcular(circular, PADRAO, Edicao.inicial(), HOJE));
        assertTrue(erro.getMessage().contains("E14"));
    }

    @Test
    void locaisDeProvaSaem9DiasAntesDaProva() {
        for (LocalDate edital : List.of(LocalDate.of(2026, 11, 30), LocalDate.of(2026, 12, 1))) {
            CronogramaCalculado c = calcular(edital(edital));
            assertEquals(etapa(c, "E15").inicio().minusDays(9), etapa(c, "E14").inicio(), edital.toString());
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
    void cb04_edital30Nov2026() {
        CronogramaCalculado c = calcular(edital(LocalDate.of(2026, 11, 30)));
        assertTrue(c.materializado());
        assertEquals(LocalDate.of(2026, 12, 1), etapa(c, "E04").inicio());
        assertEquals(LocalDate.of(2027, 1, 11), etapa(c, "E04").fim());
        assertEquals(LocalDate.of(2027, 2, 5), etapa(c, "E14").inicio());
        assertEquals(LocalDate.of(2027, 2, 14), etapa(c, "E15").inicio());
        assertFalse(c.provaInformada());
        assertEquals(LocalDate.of(2027, 2, 15), etapa(c, "E16").inicio());
        assertEquals(LocalDate.of(2027, 2, 15), etapa(c, "E17").inicio());
        assertEquals(LocalDate.of(2027, 2, 17), etapa(c, "E17").fim());
    }

    /** Spec 005, RN09: sem data da prova, a E15 é sugestão provisória e não vai ao domingo. */
    @Test
    void cb05_provaSugeridaNaoEDeslocada() {
        CronogramaCalculado c = calcular(edital(LocalDate.of(2026, 12, 1)));
        assertEquals(LocalDate.of(2027, 1, 12), etapa(c, "E04").fim());
        assertEquals(LocalDate.of(2027, 2, 15), etapa(c, "E15").inicio()); // segunda
        assertEquals(List.of("data provisória · informe a data da prova"), etapa(c, "E15").avisos());
        assertFalse(checagem(c, "PROVA_INFORMADA").cumprida());
        assertFalse(c.checagens().viola());
    }

    /** Spec 005, RN09: a prova informada fica no dia informado, mesmo no sábado; o resto conta dela. */
    @Test
    void provaInformadaEmSabadoFicaNoSabado() {
        LocalDate sabado = LocalDate.of(2027, 2, 20);
        CronogramaCalculado c = calcular(new Edicao(Set.of(), LocalDate.of(2026, 12, 1), Map.of(), Set.of(), Set.of(),
                List.of(), sabado));
        assertTrue(c.provaInformada());
        assertEquals(sabado, etapa(c, "E15").inicio());
        assertTrue(etapa(c, "E15").avisos().isEmpty());
        assertEquals(39, etapa(c, "E15").dias());
        assertEquals(LocalDate.of(2027, 2, 11), etapa(c, "E14").inicio());
        assertEquals(LocalDate.of(2027, 2, 21), etapa(c, "E16").inicio());
        assertEquals(LocalDate.of(2027, 2, 23), etapa(c, "E17").fim());
        assertEquals("39 dias", checagem(c, "RM-01").detalhe());
        assertTrue(checagem(c, "PROVA_INFORMADA").cumprida());
        assertFalse(c.checagens().viola());
    }

    @Test
    void provaInformadaNaoSeEditaPorDias() {
        Edicao edicao = new Edicao(Set.of(), LocalDate.of(2026, 12, 1), Map.of(), Set.of(),
                Set.of(Parametros.INTERVALO_PROVA), List.of(), LocalDate.of(2027, 2, 20));
        assertThrows(RegraDeNegocioException.class, () -> CalculoCronograma.prever(ESQ, PADRAO, edicao,
                new Mudanca("E15", Mudanca.Campo.DIAS, 30)));
        assertThrows(RegraDeNegocioException.class, () -> new Edicao(Set.of(), null, Map.of(), Set.of(), Set.of(),
                List.of(), LocalDate.of(2027, 2, 20)).validar(ESQ));
        assertThrows(RegraDeNegocioException.class, () -> new Edicao(Set.of(), LocalDate.of(2026, 12, 1), Map.of(),
                Set.of(), Set.of(), List.of(), LocalDate.of(2026, 11, 1)).validar(ESQ));
    }

    @Test
    void cb06_rascunhoEmDmaisN() {
        CronogramaCalculado c = calcular(Edicao.inicial());
        assertFalse(c.materializado());
        assertEquals(1, etapa(c, "E04").inicioRelativo());
        assertNull(etapa(c, "E04").inicio());
    }

    @Test
    void cb06_gabaritoNoDiaSeguinteERecursoDe2Dias() {
        // prova em 10/08: forçar a E15 nessa data a partir de um edital qualquer
        CronogramaCalculado base = calcular(edital(LocalDate.of(2027, 5, 1)));
        int provaRel = etapa(base, "E15").inicioRelativo();
        LocalDate edital = LocalDate.of(2027, 8, 10).minusDays(provaRel);
        CronogramaCalculado c = calcular(edital(edital));
        EtapaCalculada prova = etapa(c, "E15");
        LocalDate dataProva = prova.inicio();
        assertEquals(dataProva.plusDays(1), etapa(c, "E16").inicio());
        assertEquals(etapa(c, "E16").inicio(), etapa(c, "E17").inicio());
        assertEquals(dataProva.plusDays(3), etapa(c, "E17").fim());
    }

    @Test
    void valoresDaRegraVemDosParametros() {
        Parametros dez = new Parametros(Map.of(Parametros.GABARITO_APOS_PROVA, 10));
        CronogramaCalculado c = CalculoCronograma.calcular(ESQ, dez, edital(LocalDate.of(2026, 11, 30)), HOJE);
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
                new Mudanca("E04", Mudanca.Campo.DURACAO, 45));
        assertTrue(previa.etapasMovidas() > 0);
        assertEquals("E04", previa.itens().get(0).etapaCodigo());
        assertEquals("41", previa.itens().get(0).antes());
        assertTrue(previa.itens().stream().anyMatch(i -> i.etapaCodigo().equals("E10")));
        // prova sugerida: fim 12/01 → 16/01 leva a prova de 15/02 a 19/02, e a E14 junto
        assertTrue(previa.itens().stream().anyMatch(i -> i.etapaCodigo().equals("E15")));
        assertTrue(previa.itens().stream().anyMatch(i -> i.etapaCodigo().equals("E14")));
        assertEquals(45, previa.edicao().ajuste("E04").duracao());
    }

    /** Com a data da prova informada, mudar as inscrições não move a prova nem o que conta dela. */
    @Test
    void cascataNaoMoveAProvaInformada() {
        Edicao edicao = new Edicao(Set.of(), LocalDate.of(2026, 12, 1), Map.of(), Set.of(), Set.of(), List.of(),
                LocalDate.of(2027, 2, 20));
        Previa previa = CalculoCronograma.prever(ESQ, PADRAO, edicao, new Mudanca("E04", Mudanca.Campo.DURACAO, 45));
        assertTrue(previa.itens().stream().anyMatch(i -> i.etapaCodigo().equals("E10")));
        assertTrue(previa.itens().stream().noneMatch(i -> i.etapaCodigo().equals("E15")));
        assertTrue(previa.itens().stream().noneMatch(i -> i.etapaCodigo().equals("E16")));
    }

    /** Spec 005, RN11: itens próprios entram na lista do que muda de data. */
    @Test
    void itemProprioEntraNaPrevia() {
        Edicao edicao = comItens(LocalDate.of(2026, 12, 1), new ItemPersonalizado("P01", "Reunião com a comissão",
                EsqueletoIdecanV1.EDITAL_INSCRICOES, Referencia.fim("E04"), 2, 0, TipoStatus.ATIVIDADE));
        Previa previa = CalculoCronograma.prever(ESQ, PADRAO, edicao, new Mudanca("E04", Mudanca.Campo.DURACAO, 45));
        assertTrue(previa.itens().stream().anyMatch(i -> i.etapaCodigo().equals("P01")));
    }

    @Test
    void valorIgualAoModeloApagaOAjuste() {
        Edicao alterada = edital(null).comAjuste("E14", new Edicao.Ajuste(-7, null));
        assertTrue(etapa(calcular(alterada), "E14").alterada());
        Previa volta = CalculoCronograma.prever(ESQ, PADRAO, alterada,
                new Mudanca("E14", Mudanca.Campo.DIAS, -9));
        assertNull(volta.edicao().ajuste("E14"));
    }

    @Test
    void prazoPadraoTravadoSoMudaComExcecao() {
        Mudanca isencao = new Mudanca("E05", Mudanca.Campo.DURACAO, 3);
        assertThrows(RegraDeNegocioException.class,
                () -> CalculoCronograma.prever(ESQ, PADRAO, Edicao.inicial(), isencao));
        assertTrue(etapa(calcular(Edicao.inicial()), "E05").duracaoTravada());

        Edicao comExcecao = new Edicao(Set.of(), null, Map.of(), Set.of(), Set.of(Parametros.ISENCAO));
        Previa previa = CalculoCronograma.prever(ESQ, PADRAO, comExcecao, isencao);
        assertEquals(3, previa.edicao().ajuste("E05").duracao());
    }

    @Test
    void cb09_checagens() {
        Edicao provaAntecipada = new Edicao(Set.of(), LocalDate.of(2026, 11, 30),
                Map.of("E15", new Edicao.Ajuste(20, null)), Set.of(), Set.of(Parametros.INTERVALO_PROVA));
        CronogramaCalculado viola = calcular(provaAntecipada);
        assertTrue(viola.checagens().viola());
        assertFalse(checagem(viola, "RM-01").cumprida());
        assertEquals("20 dias", checagem(viola, "RM-01").detalhe());

        CronogramaCalculado ok = calcular(edital(LocalDate.of(2026, 12, 1)));
        assertFalse(ok.checagens().viola());
        assertEquals("34 dias", checagem(ok, "RM-01").detalhe());
    }

    /** Spec 005, RN08: edital no passado é alerta, não violação. */
    @Test
    void editalNoPassadoSoAlerta() {
        CronogramaCalculado c = calcular(edital(LocalDate.of(2026, 10, 9)));
        assertFalse(checagem(c, "EDITAL_NO_PRAZO").cumprida());
        assertFalse(c.checagens().viola());
    }

    @Test
    void avisaFimDeSemanaEFaixaHistorica() {
        // edital 05/12/2026 (sáb): E04 termina 16/01/2027 (sáb); E10 = fim E04 + 1 = 17/01/2027 (dom)
        CronogramaCalculado c = calcular(edital(LocalDate.of(2026, 12, 5)));
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

    private static Edicao comItens(LocalDate data, ItemPersonalizado... itens) {
        return new Edicao(Set.of(), data, Map.of(), Set.of(), Set.of(), List.of(itens));
    }

    private static ItemPersonalizado item(String codigo, String grupo, Referencia ref, int dias, int duracao) {
        return new ItemPersonalizado(codigo, "Reunião com a comissão", grupo, ref, dias, duracao, null);
    }

    @Test
    void itemPersonalizadoContaDaEtapaDeReferenciaEApareceNoGrupo() {
        Edicao edicao = comItens(LocalDate.of(2026, 12, 3),
                item("P01", EsqueletoIdecanV1.EDITAL_INSCRICOES, Referencia.fim("E04"), 2, 1));
        CronogramaCalculado c = calcular(edicao);

        EtapaCalculada p01 = etapa(c, "P01");
        EtapaCalculada e04 = etapa(c, "E04");
        assertTrue(p01.personalizada());
        assertFalse(e04.personalizada());
        assertEquals(e04.fimRelativo() + 2, p01.inicioRelativo());
        assertEquals(p01.inicioRelativo() + 1, p01.fimRelativo());
        assertEquals(e04.fim().plusDays(2), p01.inicio());
        assertEquals("fim de E04", p01.referencia());
        assertEquals(TipoStatus.ATIVIDADE, p01.tipoStatus());
        assertTrue(grupo(c, EsqueletoIdecanV1.EDITAL_INSCRICOES).etapas().contains(p01));
        assertEquals(55, c.indicadores().etapasTotal());
        assertEquals(calcular(edital(LocalDate.of(2026, 12, 3))).indicadores().etapasAtivas() + 1,
                c.indicadores().etapasAtivas());
    }

    @Test
    void itemNoGrupoDesligadoFicaInativoENaoMexeNasOutrasEtapas() {
        Edicao edicao = comItens(null, item("P01", "S12", Referencia.MARCO_ZERO, 10, 0));
        CronogramaCalculado c = calcular(edicao);
        assertFalse(etapa(c, "P01").ativa());
        assertEquals(10, etapa(c, "P01").inicioRelativo());
        assertEquals(etapa(calcular(Edicao.inicial()), "E60").inicioRelativo(), etapa(c, "E60").inicioRelativo());
    }

    @Test
    void itemPersonalizadoInvalidoEhRecusado() {
        String bloco = EsqueletoIdecanV1.EDITAL_INSCRICOES;
        assertThrows(RegraDeNegocioException.class,
                () -> comItens(null, item("X1", bloco, Referencia.MARCO_ZERO, 0, 0)).validar(ESQ));
        assertThrows(RegraDeNegocioException.class, () -> comItens(null,
                new ItemPersonalizado("P01", "  ", bloco, Referencia.MARCO_ZERO, 0, 0, null)).validar(ESQ));
        assertThrows(RegraDeNegocioException.class,
                () -> comItens(null, item("P01", "NAO_EXISTE", Referencia.MARCO_ZERO, 0, 0)).validar(ESQ));
        assertThrows(RegraDeNegocioException.class,
                () -> comItens(null, item("P01", bloco, Referencia.fim("E99"), 0, 0)).validar(ESQ));
        assertThrows(RegraDeNegocioException.class,
                () -> comItens(null, item("P01", bloco, Referencia.ULTIMO_RESULTADO, 0, 0)).validar(ESQ));
        assertThrows(RegraDeNegocioException.class,
                () -> comItens(null, item("P01", bloco, Referencia.MARCO_ZERO, 0, -1)).validar(ESQ));
        assertThrows(RegraDeNegocioException.class, () -> comItens(null,
                item("P01", bloco, Referencia.MARCO_ZERO, 0, 0), item("P01", bloco, Referencia.MARCO_ZERO, 1, 0))
                .validar(ESQ));
        comItens(null, item("P01", bloco, Referencia.inicio("E15"), -3, 0)).validar(ESQ);
    }

    private static CronogramaCalculado.Grupo grupo(CronogramaCalculado c, String codigo) {
        return c.grupos().stream().filter(g -> g.codigo().equals(codigo)).findFirst().orElseThrow();
    }

    private static CronogramaCalculado.Checagem checagem(CronogramaCalculado c, String codigo) {
        return Stream.concat(c.checagens().itens().stream(), c.checagens().alertas().stream())
                .filter(i -> i.codigo().equals(codigo)).findFirst().orElseThrow();
    }
}

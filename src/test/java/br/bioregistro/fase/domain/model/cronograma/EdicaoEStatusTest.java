package br.bioregistro.fase.domain.model.cronograma;

import br.bioregistro.fase.domain.exception.RegraDeNegocioException;
import br.bioregistro.fase.domain.model.TipoEtapa;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Critérios CB02, CB11, CB12, CB16 e CB17 da spec 003-B. */
class EdicaoEStatusTest {

    private static final Esqueleto ESQ = EsqueletoIdecanV1.ESQUELETO;
    private static final LocalDate HOJE = LocalDate.of(2026, 10, 10);

    /** Spec 005, RN08: edital no passado é válido (publicado, reprogramado, retificado). */
    @Test
    void editalAntesDeHojeEhValido() {
        Edicao ontem = new Edicao(Set.of(), HOJE.minusDays(1), Map.of(), Set.of(), Set.of());
        assertDoesNotThrow(() -> ontem.validar(ESQ));
    }

    @Test
    void cb12_ancorasNaoPodemSerRemovidas() {
        for (String ancora : EtapaModelo.NAO_REMOVIVEIS) {
            Edicao edicao = new Edicao(Set.of(), null, Map.of(), Set.of(ancora), Set.of());
            assertThrows(RegraDeNegocioException.class, () -> edicao.validar(ESQ), ancora);
        }
        Edicao semE03 = new Edicao(Set.of(), null, Map.of(), Set.of("E03"), Set.of());
        assertDoesNotThrow(() -> semE03.validar(ESQ));
    }

    @Test
    void recusaModuloDesconhecidoEDiasNoMarcoZero() {
        assertThrows(RegraDeNegocioException.class,
                () -> new Edicao(Set.of("S99"), null, Map.of(), Set.of(), Set.of()).validar(ESQ));
        assertThrows(RegraDeNegocioException.class,
                () -> new Edicao(Set.of(), null, Map.of("E01", new Edicao.Ajuste(2, null)), Set.of(), Set.of())
                        .validar(ESQ));
    }

    @Test
    void cb16_primeiroRecursoSoDepoisDeAbrirOPrazo() {
        Edicao edicao = new Edicao(Set.of(), LocalDate.of(2026, 12, 1), Map.of(), Set.of(), Set.of());
        CronogramaCalculado c = CalculoCronograma.calcular(ESQ, Parametros.padroes(), edicao, HOJE);
        EtapaCalculada recurso = c.etapa("E17").orElseThrow();

        assertThrows(RegraDeNegocioException.class,
                () -> StatusEtapa.registrarPrimeiroRecurso(recurso, StatusEtapa.A_FAZER, recurso.inicio().minusDays(1)));
        assertEquals(StatusEtapa.EM_ANALISE,
                StatusEtapa.registrarPrimeiroRecurso(recurso, StatusEtapa.A_FAZER, recurso.inicio()));
        assertThrows(RegraDeNegocioException.class,
                () -> StatusEtapa.exigirMarcacao(TipoStatus.RECURSO, StatusEtapa.A_FAZER, StatusEtapa.EM_ANALISE));
        assertThrows(RegraDeNegocioException.class,
                () -> StatusEtapa.exigirMarcacao(TipoStatus.ATIVIDADE, StatusEtapa.A_FAZER, StatusEtapa.PUBLICADO));
    }

    @Test
    void cb17_atividadeVencidaSemFeitoFicaAtrasada() {
        Edicao edicao = new Edicao(Set.of(), LocalDate.of(2026, 12, 1), Map.of(), Set.of(), Set.of());
        CronogramaCalculado c = CalculoCronograma.calcular(ESQ, Parametros.padroes(), edicao, HOJE);
        EtapaCalculada inscricoes = c.etapa("E04").orElseThrow();
        LocalDate depois = inscricoes.fim().plusDays(1);

        assertEquals(StatusEtapa.Automatico.ATRASADO, StatusEtapa.automatico(inscricoes, Map.of(), depois));
        assertNull(StatusEtapa.automatico(inscricoes, Map.of("E04", StatusEtapa.FEITO), depois));

        EtapaCalculada gabarito = c.etapa("E16").orElseThrow();
        assertEquals(StatusEtapa.Automatico.PUBLICACAO_VENCIDA,
                StatusEtapa.automatico(gabarito, Map.of(), gabarito.inicio().plusDays(1)));
    }

    @Test
    void atividadeComReferenciaNaoConcluidaFicaBloqueada() {
        Edicao edicao = new Edicao(Set.of(), LocalDate.of(2026, 12, 1), Map.of(), Set.of(), Set.of());
        CronogramaCalculado c = CalculoCronograma.calcular(ESQ, Parametros.padroes(), edicao, HOJE);
        EtapaCalculada pagamento = c.etapa("E10").orElseThrow(); // fim de E04
        assertEquals(StatusEtapa.Automatico.BLOQUEADO,
                StatusEtapa.automatico(pagamento, Map.of(), pagamento.inicio()));
        assertNull(StatusEtapa.automatico(pagamento, Map.of("E04", StatusEtapa.FEITO), pagamento.inicio()));
    }

    @Test
    void cb02_categoriaDoTipoDeEtapa() {
        assertEquals(TipoEtapa.Categoria.EXECUCAO, new TipoEtapa(9, "prova_objetiva", "").categoria());
        assertEquals(TipoEtapa.Categoria.EXECUCAO, new TipoEtapa(22, "prova_titulos", "").categoria());
        assertEquals(TipoEtapa.Categoria.ANALISE, new TipoEtapa(20, "analise_prova_titulos", "").categoria());
        assertEquals(TipoEtapa.Categoria.ANALISE, new TipoEtapa(15, "analise_pcd", "").categoria());
        assertEquals(TipoEtapa.Categoria.LEGADO, new TipoEtapa(1, "inscricao", "").categoria());
    }
}

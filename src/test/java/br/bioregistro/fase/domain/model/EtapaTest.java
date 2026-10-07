package br.bioregistro.fase.domain.model;

import br.bioregistro.fase.domain.exception.RegraDeNegocioException;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EtapaTest {

    static Etapa etapa(Integer id, Integer faseId, String nome, Integer tipoId,
                       OffsetDateTime abertura, OffsetDateTime fechamento, Integer elegibilidadeEtapaId) {
        return new Etapa(id, faseId, nome, tipoId, null, abertura, fechamento, null,
                false, false, false, false, null, null, elegibilidadeEtapaId, null, null, null);
    }

    @Test
    void etapaPodeNascerSemDatas() {
        Etapa etapa = etapa(null, 1, "Prova objetiva", 1, null, null, null);
        assertNull(etapa.dataAbertura());
    }

    @Test
    void etapaSemStatusNasceAguardando() {
        assertEquals(Etapa.STATUS_INICIAL, etapa(null, 1, "Prova objetiva", 1, null, null, null).statusId());
    }

    @Test
    void exigeFaseNomeETipo() {
        assertThrows(RegraDeNegocioException.class, () -> etapa(null, null, "Prova", 1, null, null, null));
        assertThrows(RegraDeNegocioException.class, () -> etapa(null, 1, "", 1, null, null, null));
        assertThrows(RegraDeNegocioException.class, () -> etapa(null, 1, "Prova", null, null, null, null));
    }

    @Test
    void recusaFechamentoAntesDaAbertura() {
        OffsetDateTime dia20 = OffsetDateTime.parse("2026-10-20T08:00:00-03:00");
        assertThrows(RegraDeNegocioException.class,
                () -> etapa(null, 1, "Prova", 1, dia20, dia20.minusDays(1), null));
    }

    @Test
    void etapaNaoEPreRequisitoDeSiMesma() {
        assertThrows(RegraDeNegocioException.class, () -> etapa(5, 1, "Prova", 1, null, null, 5));
    }
}

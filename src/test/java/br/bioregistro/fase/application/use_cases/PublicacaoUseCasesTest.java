package br.bioregistro.fase.application.use_cases;

import br.bioregistro.fase.application.dtos.CriarPublicacaoRequest;
import br.bioregistro.fase.application.dtos.PublicacaoDatasRequest;
import br.bioregistro.fase.application.dtos.PublicacaoResponse;
import br.bioregistro.fase.domain.exception.ConflitoException;
import br.bioregistro.fase.domain.exception.RegraDeNegocioException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static br.bioregistro.fase.application.use_cases.Cenario.etapa;
import static br.bioregistro.fase.application.use_cases.Cenario.fase;
import static br.bioregistro.fase.application.use_cases.TiposEmMemoria.GABARITO_PRELIMINAR;
import static br.bioregistro.fase.application.use_cases.TiposEmMemoria.LOCAL_PROVA;
import static br.bioregistro.fase.application.use_cases.TiposEmMemoria.PROVA_OBJETIVA;
import static br.bioregistro.fase.domain.model.Instituicao.IDECAN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PublicacaoUseCasesTest {

    private final Cenario c = new Cenario().comEdital(IDECAN, 10);
    private int faseId;
    private int etapaId;

    @BeforeEach
    void criarFaseEEtapa() {
        faseId = c.criarFase().executar(IDECAN, 10, fase("Objetiva", 1)).id();
        etapaId = c.criarEtapa().executar(IDECAN, faseId, etapa("Prova", PROVA_OBJETIVA, null)).id();
    }

    private int espaco(String valor) {
        return c.publicacoes.listarPorEtapa(IDECAN, etapaId).stream()
                .filter(p -> p.tipo().valor().equals(valor))
                .findFirst().orElseThrow().id();
    }

    private static PublicacaoDatasRequest datas(OffsetDateTime visIni, OffsetDateTime visFim,
                                                OffsetDateTime recIni, OffsetDateTime recFim) {
        return new PublicacaoDatasRequest(null, visIni, visFim, recIni, recFim);
    }

    @Test
    void publicarGabaritoPreliminarAbreORecursoNoPeriodoInformado() {
        OffsetDateTime agora = OffsetDateTime.now();
        PublicacaoResponse publicada = new PublicarPublicacao(c.publicacoes).executar(IDECAN,
                espaco("gabarito_preliminar"),
                datas(agora.minusDays(1), agora.plusDays(30), agora.minusHours(1), agora.plusDays(8)));

        assertNotNull(publicada.publicadaEm());
        assertTrue(publicada.recursoAberto());
    }

    @Test
    void gabaritoPreliminarExigePeriodoDeRecursoAoPublicar() {
        OffsetDateTime agora = OffsetDateTime.now();
        assertThrows(RegraDeNegocioException.class, () -> new PublicarPublicacao(c.publicacoes)
                .executar(IDECAN, espaco("gabarito_preliminar"), datas(agora, agora.plusDays(30), null, null)));
    }

    @Test
    void localDeProvaNaoAceitaPeriodoDeRecurso() {
        OffsetDateTime agora = OffsetDateTime.now();
        assertThrows(RegraDeNegocioException.class, () -> new PublicarPublicacao(c.publicacoes)
                .executar(IDECAN, espaco("local_prova"),
                        datas(agora, agora.plusDays(30), agora, agora.plusDays(2))));
    }

    @Test
    void datasSaoPreenchidasManualmenteAntesDePublicar() {
        OffsetDateTime prevista = OffsetDateTime.parse("2026-10-11T09:00:00-03:00");
        PublicacaoResponse atualizada = new AtualizarPublicacao(c.publicacoes).executar(IDECAN,
                espaco("local_prova"), new PublicacaoDatasRequest(prevista, null, null, null, null));

        assertEquals(prevista, atualizada.dataPrevista());
        assertNull(atualizada.publicadaEm());
    }

    @Test
    void naoExcluiPublicacaoJaPublicada() {
        OffsetDateTime agora = OffsetDateTime.now();
        int local = espaco("local_prova");
        new PublicarPublicacao(c.publicacoes).executar(IDECAN, local, datas(agora, agora.plusDays(30), null, null));

        assertThrows(ConflitoException.class, () -> new ExcluirPublicacao(c.publicacoes).executar(IDECAN, local));
    }

    @Test
    void criaEspacoExtraNaFaseOuNaEtapa() {
        CriarPublicacao criar = new CriarPublicacao(c.fases, c.etapas, c.tipos, c.publicacoes);

        PublicacaoResponse daFase = criar.executar(IDECAN, faseId, new CriarPublicacaoRequest(LOCAL_PROVA.id(), null));
        PublicacaoResponse daEtapa =
                criar.executar(IDECAN, faseId, new CriarPublicacaoRequest(GABARITO_PRELIMINAR.id(), etapaId));

        assertNull(daFase.etapaId());
        assertEquals(etapaId, daEtapa.etapaId());
    }

    @Test
    void etapaDoEspacoExtraPrecisaSerDaFase() {
        int outraFase = c.criarFase().executar(IDECAN, 10, fase("Outra", 2)).id();
        CriarPublicacao criar = new CriarPublicacao(c.fases, c.etapas, c.tipos, c.publicacoes);

        assertThrows(RegraDeNegocioException.class, () -> criar.executar(IDECAN, outraFase,
                new CriarPublicacaoRequest(LOCAL_PROVA.id(), etapaId)));
    }
}

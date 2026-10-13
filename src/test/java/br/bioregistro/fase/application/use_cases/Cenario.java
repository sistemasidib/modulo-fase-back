package br.bioregistro.fase.application.use_cases;

import br.bioregistro.fase.application.dtos.EtapaRequest;
import br.bioregistro.fase.application.dtos.FaseRequest;
import br.bioregistro.fase.domain.model.Instituicao;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;

/** Monta repositórios em memória e os use cases ligados a eles. */
class Cenario {

    static final OffsetDateTime ABERTURA = OffsetDateTime.parse("2026-10-01T00:00:00-03:00");
    static final OffsetDateTime FECHAMENTO = OffsetDateTime.parse("2026-10-31T23:59:00-03:00");
    /** {@code dom_status_candidato_etapa}. */
    static final int APROVADO = 3;
    /** "Hoje" = 10/10/2026. */
    static final Clock RELOGIO = Clock.fixed(Instant.parse("2026-10-10T03:00:00Z"), ZoneId.of("America/Sao_Paulo"));

    final FasesEmMemoria fases = new FasesEmMemoria();
    final EtapasEmMemoria etapas = new EtapasEmMemoria();
    final GruposEmMemoria grupos = new GruposEmMemoria(fases);
    final PublicacoesEmMemoria publicacoes = new PublicacoesEmMemoria();
    final TiposEmMemoria tipos = new TiposEmMemoria();
    final EspacosDePublicacao espacos = new EspacosDePublicacao(tipos, publicacoes);
    final CriteriosDesempateEmMemoria criterios = new CriteriosDesempateEmMemoria();
    final DisciplinasEmMemoria disciplinas = new DisciplinasEmMemoria();
    final CriteriosCorrecaoEmMemoria criteriosCorrecao = new CriteriosCorrecaoEmMemoria();
    final CronogramasEmMemoria cronogramas = new CronogramasEmMemoria();
    final VinculosEmMemoria vinculos = new VinculosEmMemoria(cronogramas, publicacoes);

    Cenario comEdital(Instituicao instituicao, int editalId) {
        fases.comEdital(instituicao, editalId);
        return this;
    }

    CriarFase criarFase() {
        return new CriarFase(fases, espacos);
    }

    ExcluirFase excluirFase() {
        return new ExcluirFase(fases, grupos, espacos, criterios, vinculos, ponte());
    }

    CriarEtapa criarEtapa() {
        return new CriarEtapa(fases, etapas, tipos, espacos);
    }

    AtualizarEtapa atualizarEtapa() {
        return new AtualizarEtapa(etapas, tipos, vinculos);
    }

    ExcluirEtapa excluirEtapa() {
        return new ExcluirEtapa(etapas, espacos, disciplinas, criteriosCorrecao);
    }

    CriteriosDeCorrecao criteriosDeCorrecao() {
        return new CriteriosDeCorrecao(criteriosCorrecao, etapas, tipos);
    }

    PonteCronograma ponte() {
        ConsultarCronograma consulta = new ConsultarCronograma(cronogramas, RELOGIO);
        return new PonteCronograma(cronogramas, consulta,
                new EstruturaDoCronograma(cronogramas, vinculos, fases, etapas, tipos, publicacoes));
    }

    PublicacoesDoCronograma doCronograma() {
        return doCronograma(null);
    }

    /** @param encerramento fim do legado; {@code null} = sem data */
    PublicacoesDoCronograma doCronograma(LocalDate encerramento) {
        return new PublicacoesDoCronograma(vinculos,
                new EncerramentoDoLegado(encerramento, RELOGIO, fases, cronogramas));
    }

    GerarFasesDoGrupo gerarFases() {
        return new GerarFasesDoGrupo(grupos, fases, etapas, tipos, espacos);
    }

    static FaseRequest fase(String nome, int ordem) {
        return new FaseRequest(nome, null, (short) ordem, ABERTURA, FECHAMENTO);
    }

    static EtapaRequest etapa(String nome, int tipoId, Integer elegibilidadeEtapaId) {
        return new EtapaRequest(nome, tipoId, null, null, null, null, null, null, null, null, null, null,
                elegibilidadeEtapaId, elegibilidadeEtapaId == null ? null : APROVADO, null, null);
    }
}

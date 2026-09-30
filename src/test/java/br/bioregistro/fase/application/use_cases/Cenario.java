package br.bioregistro.fase.application.use_cases;

import br.bioregistro.fase.application.dtos.EtapaRequest;
import br.bioregistro.fase.application.dtos.FaseRequest;
import br.bioregistro.fase.domain.model.Instituicao;

import java.time.OffsetDateTime;

/** Monta repositórios em memória e os use cases ligados a eles. */
class Cenario {

    static final OffsetDateTime ABERTURA = OffsetDateTime.parse("2026-10-01T00:00:00-03:00");
    static final OffsetDateTime FECHAMENTO = OffsetDateTime.parse("2026-10-31T23:59:00-03:00");

    final FasesEmMemoria fases = new FasesEmMemoria();
    final EtapasEmMemoria etapas = new EtapasEmMemoria();
    final GruposEmMemoria grupos = new GruposEmMemoria(fases);
    final PublicacoesEmMemoria publicacoes = new PublicacoesEmMemoria();
    final TiposEmMemoria tipos = new TiposEmMemoria();
    final EspacosDePublicacao espacos = new EspacosDePublicacao(tipos, publicacoes);

    Cenario comEdital(Instituicao instituicao, int editalId) {
        fases.comEdital(instituicao, editalId);
        return this;
    }

    CriarFase criarFase() {
        return new CriarFase(fases, espacos);
    }

    ExcluirFase excluirFase() {
        return new ExcluirFase(fases, grupos, espacos);
    }

    CriarEtapa criarEtapa() {
        return new CriarEtapa(fases, etapas, tipos, espacos);
    }

    AtualizarEtapa atualizarEtapa() {
        return new AtualizarEtapa(etapas, tipos);
    }

    ExcluirEtapa excluirEtapa() {
        return new ExcluirEtapa(etapas, espacos);
    }

    GerarFasesDoGrupo gerarFases() {
        return new GerarFasesDoGrupo(grupos, fases, etapas, tipos, espacos);
    }

    static FaseRequest fase(String nome, int ordem) {
        return new FaseRequest(nome, null, (short) ordem, ABERTURA, FECHAMENTO);
    }

    static EtapaRequest etapa(String nome, int tipoId, Integer elegibilidadeEtapaId) {
        return new EtapaRequest(nome, tipoId, null, null, null, null, null, null, null, null, null, null,
                elegibilidadeEtapaId, null, null, null);
    }
}

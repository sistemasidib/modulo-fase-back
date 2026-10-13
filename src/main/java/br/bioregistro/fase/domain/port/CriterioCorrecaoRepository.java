package br.bioregistro.fase.domain.port;

import br.bioregistro.fase.domain.model.CriterioCorrecao;
import br.bioregistro.fase.domain.model.Instituicao;

import java.util.List;
import java.util.Optional;

/** Tabela {@code criterio_correcao} ({@code db/011}). */
public interface CriterioCorrecaoRepository {

    /** Na ordem. */
    List<CriterioCorrecao> daEtapa(Instituicao instituicao, int etapaId);

    Optional<CriterioCorrecao> buscar(Instituicao instituicao, int id);

    /** Insere ({@code id} nulo) ou atualiza; devolve com o id. */
    CriterioCorrecao salvar(Instituicao instituicao, CriterioCorrecao criterio);

    void excluir(Instituicao instituicao, int id);

    void excluirDaEtapa(Instituicao instituicao, int etapaId);
}

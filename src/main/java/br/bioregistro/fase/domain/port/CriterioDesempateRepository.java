package br.bioregistro.fase.domain.port;

import br.bioregistro.fase.domain.model.CriterioDesempate;
import br.bioregistro.fase.domain.model.Instituicao;

import java.util.List;
import java.util.Optional;

/** Tabela {@code criterio_desempate} ({@code db/008}; por fase desde {@code db/009}). */
public interface CriterioDesempateRepository {

    /** Na ordem de aplicação. */
    List<CriterioDesempate> daFase(Instituicao instituicao, int faseId);

    Optional<CriterioDesempate> buscar(Instituicao instituicao, int id);

    /** Insere ({@code id} nulo) ou atualiza; devolve com o id. */
    CriterioDesempate salvar(Instituicao instituicao, CriterioDesempate criterio);

    void excluir(Instituicao instituicao, int id);

    void excluirDaFase(Instituicao instituicao, int faseId);
}

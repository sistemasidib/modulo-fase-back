package br.bioregistro.fase.domain.port;

import br.bioregistro.fase.domain.model.CriterioDesempate;
import br.bioregistro.fase.domain.model.Instituicao;

import java.util.List;
import java.util.Optional;

/** Tabela {@code criterio_desempate} ({@code db/008}; por etapa desde {@code db/030}). */
public interface CriterioDesempateRepository {

    /** Na ordem de aplicação. */
    List<CriterioDesempate> daEtapa(Instituicao instituicao, int etapaId);

    /**
     * Critérios de todas as etapas da fase (agregação para leitura de estrutura).
     * Cada etapa mantém a própria lista; isto só junta na ordem das etapas.
     */
    List<CriterioDesempate> daFase(Instituicao instituicao, int faseId);

    Optional<CriterioDesempate> buscar(Instituicao instituicao, int id);

    /** Insere ({@code id} nulo) ou atualiza; devolve com o id. */
    CriterioDesempate salvar(Instituicao instituicao, CriterioDesempate criterio);

    void excluir(Instituicao instituicao, int id);

    void excluirDaEtapa(Instituicao instituicao, int etapaId);

    /** Remove os critérios de todas as etapas da fase (antes de excluir a fase). */
    void excluirDaFase(Instituicao instituicao, int faseId);
}

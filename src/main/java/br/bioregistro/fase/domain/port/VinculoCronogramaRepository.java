package br.bioregistro.fase.domain.port;

import br.bioregistro.fase.domain.model.Instituicao;
import br.bioregistro.fase.domain.model.cronograma.LinhaDoCronograma;
import br.bioregistro.fase.domain.model.cronograma.ModuloDeExecucao;

import java.util.Map;
import java.util.Optional;

/**
 * Vínculos da estrutura com o cronograma (spec 005, {@code db/015}): fase ↔ módulo
 * ({@code fase_cronograma}) e publicação → linha de leitura (FKs em {@code publicacao}).
 */
public interface VinculoCronogramaRepository {

    Optional<Integer> faseDoModulo(Instituicao instituicao, int editalDoCronogramaId, ModuloDeExecucao modulo);

    Optional<ModuloDeExecucao> moduloDaFase(Instituicao instituicao, int faseId);

    void vincularFase(Instituicao instituicao, int faseId, int editalDoCronogramaId, ModuloDeExecucao modulo);

    void desvincularFase(Instituicao instituicao, int faseId);

    /** @param recursoLinhaId {@code null} = não abre recurso */
    void vincularPublicacao(Instituicao instituicao, int publicacaoId, int linhaId, Integer recursoLinhaId);

    /** Publicações da fase (e das etapas dela) que apontam para o cronograma: id → linhas. */
    Map<Integer, LinhaDoCronograma.DaPublicacao> publicacoesDaFase(Instituicao instituicao, int faseId);

    Optional<LinhaDoCronograma.DaPublicacao> daPublicacao(Instituicao instituicao, int publicacaoId);
}

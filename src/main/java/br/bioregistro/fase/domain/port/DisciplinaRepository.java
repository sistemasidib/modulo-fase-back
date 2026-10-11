package br.bioregistro.fase.domain.port;

import br.bioregistro.fase.domain.model.DisciplinaDaProva;
import br.bioregistro.fase.domain.model.Instituicao;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/** Disciplinas do edital e suas faixas de questões (tabelas do modulo-dados), com o peso e a etapa. */
public interface DisciplinaRepository {

    /**
     * Faixas ativas de disciplinas ativas do edital que são da etapa, por questão inicial.
     * {@code incluirSemEtapa}: inclui também as faixas antigas, sem etapa (prova objetiva).
     */
    List<DisciplinaDaProva> daEtapa(Instituicao instituicao, int editalId, int etapaId, boolean incluirSemEtapa);

    Optional<DisciplinaDaProva> buscar(Instituicao instituicao, int faixaId);

    void definirPeso(Instituicao instituicao, int faixaId, BigDecimal peso);

    /** Disciplina do edital com este nome (sem diferença de maiúsculas); cria ou reativa se preciso. */
    int disciplinaDoEdital(Instituicao instituicao, int editalId, String nome);

    /** Devolve o id da faixa nova. */
    int inserirFaixa(Instituicao instituicao, int disciplinaEditalId, int etapaId, int inicio, int fim, int cargoId);

    void removerFaixa(Instituicao instituicao, int faixaId);

    /** Desativa as faixas da etapa e solta o vínculo, para a etapa poder ser excluída. */
    void removerDaEtapa(Instituicao instituicao, int etapaId);
}

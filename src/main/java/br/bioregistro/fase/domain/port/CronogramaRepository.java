package br.bioregistro.fase.domain.port;

import br.bioregistro.fase.domain.model.Instituicao;
import br.bioregistro.fase.domain.model.cronograma.Acao;
import br.bioregistro.fase.domain.model.cronograma.Cronograma;
import br.bioregistro.fase.domain.model.cronograma.CronogramaCalculado;
import br.bioregistro.fase.domain.model.cronograma.Parametros;
import br.bioregistro.fase.domain.model.cronograma.Particularidade;
import br.bioregistro.fase.domain.model.cronograma.StatusEtapa;

import java.util.List;
import java.util.Optional;

public interface CronogramaRepository {

    /** Números atuais do modelo ({@code cronograma_parametro_padrao}). */
    Parametros parametrosPadrao(Instituicao instituicao);

    Optional<Integer> idDoConcurso(Instituicao instituicao, int concursoId);

    /** Grava o cronograma novo com uma linha por etapa e devolve o id. */
    int criar(Instituicao instituicao, Cronograma cronograma, CronogramaCalculado calculado);

    Optional<Cronograma> buscar(Instituicao instituicao, int id);

    /**
     * Grava a edição e a cópia das datas, se a versão ainda for {@code cronograma.versao()}; a
     * versão sobe 1. Versão desatualizada: {@code ConflitoException}.
     */
    void salvar(Instituicao instituicao, Cronograma cronograma, CronogramaCalculado calculado);

    void mudarStatus(Instituicao instituicao, int cronogramaId, String codigo, StatusEtapa status);

    /** Histórico só de inclusão (RN27). */
    void registrar(Instituicao instituicao, int cronogramaId, Acao acao);

    /** Da mais nova para a mais antiga. */
    List<Acao> historico(Instituicao instituicao, int cronogramaId);

    List<Particularidade> particularidades(Instituicao instituicao, int cronogramaId);

    Particularidade adicionarParticularidade(Instituicao instituicao, int cronogramaId, Particularidade particularidade);

    Optional<Particularidade> particularidade(Instituicao instituicao, int cronogramaId, int id);

    void removerParticularidade(Instituicao instituicao, int cronogramaId, int id);
}

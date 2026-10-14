package br.bioregistro.fase.domain.port;

import br.bioregistro.fase.domain.model.Instituicao;
import br.bioregistro.fase.domain.model.cronograma.Acao;
import br.bioregistro.fase.domain.model.cronograma.Cronograma;
import br.bioregistro.fase.domain.model.cronograma.CronogramaCalculado;
import br.bioregistro.fase.domain.model.cronograma.EditalDoCronograma;
import br.bioregistro.fase.domain.model.cronograma.Parametros;
import br.bioregistro.fase.domain.model.cronograma.Particularidade;
import br.bioregistro.fase.domain.model.cronograma.StatusEtapa;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface CronogramaRepository {

    /** Números atuais do modelo ({@code cronograma_parametro_padrao}). */
    Parametros parametrosPadrao(Instituicao instituicao);

    /** Editais da entidade que seguem algum cronograma: edital → cronograma. */
    Map<Integer, Integer> cronogramasDosEditais(Instituicao instituicao, int entidadeId);

    /**
     * Grava o cronograma novo com uma linha por etapa (itens próprios e status incluídos) e
     * devolve o id.
     */
    int criar(Instituicao instituicao, Cronograma cronograma, CronogramaCalculado calculado);

    Optional<Cronograma> buscar(Instituicao instituicao, int id);

    /**
     * Grava a edição e a cópia das datas, se a versão ainda for {@code cronograma.versao()}; a
     * versão sobe 1. Versão desatualizada: {@code ConflitoException}.
     */
    void salvar(Instituicao instituicao, Cronograma cronograma, CronogramaCalculado calculado);

    void mudarStatus(Instituicao instituicao, int cronogramaId, String codigo, StatusEtapa status);

    // ── editais que seguem o cronograma ───────────────────────────────────────

    Optional<EditalDoCronograma> doEdital(Instituicao instituicao, int editalId);

    List<EditalDoCronograma> editais(Instituicao instituicao, int cronogramaId);

    /**
     * Passa o edital a seguir o cronograma, sem sobreposição. Se o edital já seguia outro, o vínculo
     * (e as linhas de leitura, que as publicações referenciam) continua o mesmo.
     */
    EditalDoCronograma ligarEdital(Instituicao instituicao, int cronogramaId, int editalId);

    /** Grava a sobreposição se a versão ainda for a lida; a versão sobe 1. Senão, {@code ConflitoException}. */
    void salvarSobreposicao(Instituicao instituicao, EditalDoCronograma edital);

    /**
     * Linhas de leitura do edital ({@code cronograma_edital_etapa}): datas e status efetivos de cada
     * etapa. Cria as que faltam e apaga as de itens que deixaram de existir.
     */
    void materializar(Instituicao instituicao, EditalDoCronograma edital, CronogramaCalculado calculado,
                      Map<String, StatusEtapa> status);

    /** Código → id da linha de leitura do edital, para as publicações apontarem (FK). */
    Map<String, Integer> linhasDoEdital(Instituicao instituicao, int editalDoCronogramaId);

    // ── histórico e particularidades ──────────────────────────────────────────

    /** Histórico só de inclusão (RN27). */
    void registrar(Instituicao instituicao, int cronogramaId, Acao acao);

    /** Da mais nova para a mais antiga. */
    List<Acao> historico(Instituicao instituicao, int cronogramaId);

    List<Particularidade> particularidades(Instituicao instituicao, int cronogramaId);

    Particularidade adicionarParticularidade(Instituicao instituicao, int cronogramaId, Particularidade particularidade);

    Optional<Particularidade> particularidade(Instituicao instituicao, int cronogramaId, int id);

    void removerParticularidade(Instituicao instituicao, int cronogramaId, int id);
}

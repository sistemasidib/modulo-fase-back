package br.bioregistro.fase.application.use_cases;

import br.bioregistro.fase.domain.exception.NaoEncontradoException;
import br.bioregistro.fase.domain.model.Instituicao;
import br.bioregistro.fase.domain.model.cronograma.Cronograma;
import br.bioregistro.fase.domain.model.cronograma.CronogramaCalculado;
import br.bioregistro.fase.domain.model.cronograma.EditalDoCronograma;
import br.bioregistro.fase.domain.model.cronograma.ModuloDeExecucao;
import br.bioregistro.fase.domain.port.CronogramaRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

/**
 * Ponte cronograma → edital (spec 005): depois de qualquer mudança no cronograma, grava as linhas de
 * leitura de cada edital que o segue (datas e status efetivos, com a sobreposição do edital). É
 * idempotente: rodar de novo não muda nada.
 */
@ApplicationScoped
public class PonteCronograma {

    private final CronogramaRepository cronogramas;
    private final ConsultarCronograma consulta;
    private final EstruturaDoCronograma estrutura;

    public PonteCronograma(CronogramaRepository cronogramas, ConsultarCronograma consulta,
                           EstruturaDoCronograma estrutura) {
        this.cronogramas = cronogramas;
        this.consulta = consulta;
        this.estrutura = estrutura;
    }

    /** Todos os editais que seguem o cronograma. */
    public void sincronizarCronograma(Instituicao instituicao, int cronogramaId) {
        cronogramas.editais(instituicao, cronogramaId).forEach(e -> sincronizar(instituicao, e));
    }

    @Transactional
    public void sincronizarEdital(Instituicao instituicao, int editalId) {
        EditalDoCronograma edital = cronogramas.doEdital(instituicao, editalId)
                .orElseThrow(() -> new NaoEncontradoException("Cronograma do edital", editalId));
        sincronizar(instituicao, edital);
    }

    /** O módulo está ligado no cronograma que o edital segue? Edital sem cronograma: não. */
    public boolean ligado(Instituicao instituicao, int editalId, ModuloDeExecucao modulo) {
        return cronogramas.doEdital(instituicao, editalId)
                .map(edital -> {
                    Cronograma efetivo = ConsultarCronograma.exigir(cronogramas, instituicao, edital.cronogramaId())
                            .doEdital(edital);
                    return modulo.ligado(consulta.calcular(efetivo, efetivo.edicao()));
                })
                .orElse(false);
    }

    private void sincronizar(Instituicao instituicao, EditalDoCronograma edital) {
        Cronograma efetivo = ConsultarCronograma.exigir(cronogramas, instituicao, edital.cronogramaId())
                .doEdital(edital);
        CronogramaCalculado calculado = consulta.calcular(efetivo, efetivo.edicao());
        cronogramas.materializar(instituicao, edital, calculado, efetivo.status());
        estrutura.sincronizar(instituicao, edital, calculado);
    }
}

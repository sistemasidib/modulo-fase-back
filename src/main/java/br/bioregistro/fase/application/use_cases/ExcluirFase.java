package br.bioregistro.fase.application.use_cases;

import br.bioregistro.fase.domain.exception.ConflitoException;
import br.bioregistro.fase.domain.exception.NaoEncontradoException;
import br.bioregistro.fase.domain.model.Fase;
import br.bioregistro.fase.domain.model.Instituicao;
import br.bioregistro.fase.domain.port.CriterioDesempateRepository;
import br.bioregistro.fase.domain.port.FaseRepository;
import br.bioregistro.fase.domain.port.GrupoCargoRepository;
import br.bioregistro.fase.domain.port.VinculoCronogramaRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

/**
 * Exclui a fase, seus espaços de publicação vazios, os critérios de desempate das suas etapas e os
 * vínculos com os grupos e com o cronograma. Se ela ainda tiver etapas ou candidatos vinculados, o
 * banco recusa e o repositório lança {@code ConflitoException} (a transação desfaz tudo). A fase
 * criada pelo cronograma só sai depois que o módulo dela é desligado lá (senão a ponte a criaria de
 * novo).
 */
@ApplicationScoped
public class ExcluirFase {

    private final FaseRepository fases;
    private final GrupoCargoRepository grupos;
    private final EspacosDePublicacao espacos;
    private final CriterioDesempateRepository criterios;
    private final VinculoCronogramaRepository vinculos;
    private final PonteCronograma ponte;

    public ExcluirFase(FaseRepository fases, GrupoCargoRepository grupos, EspacosDePublicacao espacos,
                       CriterioDesempateRepository criterios, VinculoCronogramaRepository vinculos,
                       PonteCronograma ponte) {
        this.fases = fases;
        this.grupos = grupos;
        this.espacos = espacos;
        this.criterios = criterios;
        this.vinculos = vinculos;
        this.ponte = ponte;
    }

    @Transactional
    public void executar(Instituicao instituicao, int id) {
        Fase fase = fases.buscar(instituicao, id).orElseThrow(() -> new NaoEncontradoException("Fase", id));
        vinculos.moduloDaFase(instituicao, id).ifPresent(modulo -> {
            if (ponte.ligado(instituicao, fase.editalId(), modulo)) {
                throw new ConflitoException("A fase " + id + " é a " + modulo.nome() + " do cronograma do edital. "
                        + "Desligue o módulo no cronograma antes de excluí-la.");
            }
            vinculos.desvincularFase(instituicao, id);
        });
        espacos.removerDaFase(instituicao, id);
        criterios.excluirDaFase(instituicao, id);
        grupos.desvincularFase(instituicao, id);
        fases.excluir(instituicao, id);
    }
}

package br.bioregistro.fase.application.use_cases;

import br.bioregistro.fase.domain.exception.NaoEncontradoException;
import br.bioregistro.fase.domain.model.Instituicao;
import br.bioregistro.fase.domain.port.CriterioDesempateRepository;
import br.bioregistro.fase.domain.port.FaseRepository;
import br.bioregistro.fase.domain.port.GrupoCargoRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

/**
 * Exclui a fase, seus espaços de publicação vazios, seus critérios de desempate e o vínculo com o
 * grupo. Se ela ainda tiver etapas ou candidatos vinculados, o banco recusa e o repositório lança
 * {@code ConflitoException} (a transação desfaz tudo).
 */
@ApplicationScoped
public class ExcluirFase {

    private final FaseRepository fases;
    private final GrupoCargoRepository grupos;
    private final EspacosDePublicacao espacos;
    private final CriterioDesempateRepository criterios;

    public ExcluirFase(FaseRepository fases, GrupoCargoRepository grupos, EspacosDePublicacao espacos,
                       CriterioDesempateRepository criterios) {
        this.fases = fases;
        this.grupos = grupos;
        this.espacos = espacos;
        this.criterios = criterios;
    }

    @Transactional
    public void executar(Instituicao instituicao, int id) {
        if (fases.buscar(instituicao, id).isEmpty()) {
            throw new NaoEncontradoException("Fase", id);
        }
        espacos.removerDaFase(instituicao, id);
        criterios.excluirDaFase(instituicao, id);
        grupos.desvincularFase(instituicao, id);
        fases.excluir(instituicao, id);
    }
}

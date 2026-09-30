package br.bioregistro.fase.application.use_cases;

import br.bioregistro.fase.domain.exception.NaoEncontradoException;
import br.bioregistro.fase.domain.model.Instituicao;
import br.bioregistro.fase.domain.port.FaseRepository;
import br.bioregistro.fase.domain.port.GrupoCargoRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

/**
 * Exclui a fase, seus espaços de publicação vazios e o vínculo com o grupo. Se ela ainda
 * tiver etapas ou candidatos vinculados, o banco recusa e o repositório lança
 * {@code ConflitoException} (a transação desfaz tudo).
 */
@ApplicationScoped
public class ExcluirFase {

    private final FaseRepository fases;
    private final GrupoCargoRepository grupos;
    private final EspacosDePublicacao espacos;

    public ExcluirFase(FaseRepository fases, GrupoCargoRepository grupos, EspacosDePublicacao espacos) {
        this.fases = fases;
        this.grupos = grupos;
        this.espacos = espacos;
    }

    @Transactional
    public void executar(Instituicao instituicao, int id) {
        if (fases.buscar(instituicao, id).isEmpty()) {
            throw new NaoEncontradoException("Fase", id);
        }
        espacos.removerDaFase(instituicao, id);
        grupos.desvincularFase(instituicao, id);
        fases.excluir(instituicao, id);
    }
}

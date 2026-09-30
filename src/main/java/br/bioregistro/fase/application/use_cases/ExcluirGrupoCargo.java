package br.bioregistro.fase.application.use_cases;

import br.bioregistro.fase.domain.exception.ConflitoException;
import br.bioregistro.fase.domain.exception.NaoEncontradoException;
import br.bioregistro.fase.domain.model.Instituicao;
import br.bioregistro.fase.domain.port.GrupoCargoRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

/** Exclui o grupo e solta os cargos dele. Grupo com fases geradas não pode ser excluído. */
@ApplicationScoped
public class ExcluirGrupoCargo {

    private final GrupoCargoRepository grupos;

    public ExcluirGrupoCargo(GrupoCargoRepository grupos) {
        this.grupos = grupos;
    }

    @Transactional
    public void executar(Instituicao instituicao, int id) {
        if (grupos.buscar(instituicao, id).isEmpty()) {
            throw new NaoEncontradoException("Grupo de cargos", id);
        }
        if (grupos.possuiFases(instituicao, id)) {
            throw new ConflitoException("O grupo " + id + " tem fases geradas. Exclua as fases antes.");
        }
        grupos.cargosDoGrupo(instituicao, id).forEach(cargoId -> grupos.removerCargo(instituicao, id, cargoId));
        grupos.excluir(instituicao, id);
    }
}

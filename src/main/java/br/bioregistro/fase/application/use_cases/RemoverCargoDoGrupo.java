package br.bioregistro.fase.application.use_cases;

import br.bioregistro.fase.application.dtos.GrupoCargoResponse;
import br.bioregistro.fase.domain.exception.NaoEncontradoException;
import br.bioregistro.fase.domain.model.GrupoCargo;
import br.bioregistro.fase.domain.model.Instituicao;
import br.bioregistro.fase.domain.port.GrupoCargoRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class RemoverCargoDoGrupo {

    private final GrupoCargoRepository grupos;

    public RemoverCargoDoGrupo(GrupoCargoRepository grupos) {
        this.grupos = grupos;
    }

    @Transactional
    public GrupoCargoResponse executar(Instituicao instituicao, int grupoId, int cargoId) {
        GrupoCargo grupo = grupos.buscar(instituicao, grupoId)
                .orElseThrow(() -> new NaoEncontradoException("Grupo de cargos", grupoId));
        if (!grupos.cargosDoGrupo(instituicao, grupoId).contains(cargoId)) {
            throw new NaoEncontradoException("Cargo no grupo " + grupoId + ":", cargoId);
        }
        grupos.removerCargo(instituicao, grupoId, cargoId);
        return GrupoCargoResponse.de(grupo, grupos.cargosDoGrupo(instituicao, grupoId));
    }
}

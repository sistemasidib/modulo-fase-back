package br.bioregistro.fase.application.use_cases;

import br.bioregistro.fase.application.dtos.GrupoCargoResponse;
import br.bioregistro.fase.domain.exception.ConflitoException;
import br.bioregistro.fase.domain.exception.NaoEncontradoException;
import br.bioregistro.fase.domain.model.GrupoCargo;
import br.bioregistro.fase.domain.model.Instituicao;
import br.bioregistro.fase.domain.port.GrupoCargoRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.Optional;

/**
 * Coloca um cargo do edital no grupo. Pode ser feito a qualquer momento, inclusive para
 * cargos criados depois da geração. Um cargo que já está em outro grupo precisa ser
 * removido de lá antes (um cargo, um grupo).
 */
@ApplicationScoped
public class AdicionarCargoAoGrupo {

    private final GrupoCargoRepository grupos;

    public AdicionarCargoAoGrupo(GrupoCargoRepository grupos) {
        this.grupos = grupos;
    }

    @Transactional
    public GrupoCargoResponse executar(Instituicao instituicao, int grupoId, int cargoId) {
        GrupoCargo grupo = grupos.buscar(instituicao, grupoId)
                .orElseThrow(() -> new NaoEncontradoException("Grupo de cargos", grupoId));
        boolean cargoDoEdital = grupos.cargosDoEdital(instituicao, grupo.editalId()).stream()
                .anyMatch(c -> c.id() == cargoId);
        if (!cargoDoEdital) {
            throw new NaoEncontradoException("Cargo do edital " + grupo.editalId() + ":", cargoId);
        }
        Optional<Integer> grupoAtual = grupos.grupoDoCargo(instituicao, cargoId);
        if (grupoAtual.isPresent() && grupoAtual.get() != grupoId) {
            throw new ConflitoException(
                    "O cargo " + cargoId + " já está no grupo " + grupoAtual.get() + ". Remova-o de lá antes.");
        }
        if (grupoAtual.isEmpty()) {
            grupos.adicionarCargo(instituicao, grupoId, cargoId);
        }
        return GrupoCargoResponse.de(grupo, grupos.cargosDoGrupo(instituicao, grupoId));
    }
}

package br.bioregistro.fase.application.use_cases;

import br.bioregistro.fase.application.dtos.FaseResponse;
import br.bioregistro.fase.domain.exception.NaoEncontradoException;
import br.bioregistro.fase.domain.model.Instituicao;
import br.bioregistro.fase.domain.port.FaseRepository;
import br.bioregistro.fase.domain.port.GrupoCargoRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

/** Fases geradas para o grupo, em ordem crescente. */
@ApplicationScoped
public class ListarFasesDoGrupo {

    private final FaseRepository fases;
    private final GrupoCargoRepository grupos;

    public ListarFasesDoGrupo(FaseRepository fases, GrupoCargoRepository grupos) {
        this.fases = fases;
        this.grupos = grupos;
    }

    public List<FaseResponse> executar(Instituicao instituicao, int grupoId) {
        if (grupos.buscar(instituicao, grupoId).isEmpty()) {
            throw new NaoEncontradoException("Grupo de cargos", grupoId);
        }
        return fases.listarPorGrupo(instituicao, grupoId).stream()
                .map(FaseResponse::de)
                .toList();
    }
}

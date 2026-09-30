package br.bioregistro.fase.application.use_cases;

import br.bioregistro.fase.application.dtos.GrupoCargoResponse;
import br.bioregistro.fase.domain.exception.NaoEncontradoException;
import br.bioregistro.fase.domain.model.Instituicao;
import br.bioregistro.fase.domain.port.FaseRepository;
import br.bioregistro.fase.domain.port.GrupoCargoRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class ListarGruposCargo {

    private final FaseRepository fases;
    private final GrupoCargoRepository grupos;

    public ListarGruposCargo(FaseRepository fases, GrupoCargoRepository grupos) {
        this.fases = fases;
        this.grupos = grupos;
    }

    public List<GrupoCargoResponse> executar(Instituicao instituicao, int editalId) {
        if (!fases.editalExiste(instituicao, editalId)) {
            throw new NaoEncontradoException("Edital", editalId);
        }
        return grupos.listarPorEdital(instituicao, editalId).stream()
                .map(g -> GrupoCargoResponse.de(g, grupos.cargosDoGrupo(instituicao, g.id())))
                .toList();
    }
}

package br.bioregistro.fase.application.use_cases;

import br.bioregistro.fase.application.dtos.CargoResponse;
import br.bioregistro.fase.domain.exception.NaoEncontradoException;
import br.bioregistro.fase.domain.model.Instituicao;
import br.bioregistro.fase.domain.port.FaseRepository;
import br.bioregistro.fase.domain.port.GrupoCargoRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Cargos do edital ainda sem grupo (alimenta o botão "agrupar cargo"). */
@ApplicationScoped
public class ListarCargosSemGrupo {

    private final FaseRepository fases;
    private final GrupoCargoRepository grupos;

    public ListarCargosSemGrupo(FaseRepository fases, GrupoCargoRepository grupos) {
        this.fases = fases;
        this.grupos = grupos;
    }

    public List<CargoResponse> executar(Instituicao instituicao, int editalId) {
        if (!fases.editalExiste(instituicao, editalId)) {
            throw new NaoEncontradoException("Edital", editalId);
        }
        Set<Integer> agrupados = new HashSet<>();
        grupos.listarPorEdital(instituicao, editalId)
                .forEach(g -> agrupados.addAll(grupos.cargosDoGrupo(instituicao, g.id())));
        return grupos.cargosDoEdital(instituicao, editalId).stream()
                .filter(c -> !agrupados.contains(c.id()))
                .map(CargoResponse::de)
                .toList();
    }
}

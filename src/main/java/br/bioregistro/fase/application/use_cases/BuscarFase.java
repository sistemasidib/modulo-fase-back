package br.bioregistro.fase.application.use_cases;

import br.bioregistro.fase.application.dtos.FaseResponse;
import br.bioregistro.fase.domain.exception.NaoEncontradoException;
import br.bioregistro.fase.domain.model.Instituicao;
import br.bioregistro.fase.domain.port.FaseRepository;
import br.bioregistro.fase.domain.port.GrupoCargoRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class BuscarFase {

    private final FaseRepository fases;
    private final GrupoCargoRepository grupos;

    public BuscarFase(FaseRepository fases, GrupoCargoRepository grupos) {
        this.fases = fases;
        this.grupos = grupos;
    }

    public FaseResponse executar(Instituicao instituicao, int id) {
        return fases.buscar(instituicao, id)
                .map(f -> FaseResponse.de(f, grupos.gruposDaFase(instituicao, f.id())))
                .orElseThrow(() -> new NaoEncontradoException("Fase", id));
    }
}

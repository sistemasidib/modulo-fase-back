package br.bioregistro.fase.application.use_cases;

import br.bioregistro.fase.application.dtos.FaseResponse;
import br.bioregistro.fase.domain.exception.NaoEncontradoException;
import br.bioregistro.fase.domain.model.Instituicao;
import br.bioregistro.fase.domain.port.FaseRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class BuscarFase {

    private final FaseRepository fases;

    public BuscarFase(FaseRepository fases) {
        this.fases = fases;
    }

    public FaseResponse executar(Instituicao instituicao, int id) {
        return fases.buscar(instituicao, id)
                .map(FaseResponse::de)
                .orElseThrow(() -> new NaoEncontradoException("Fase", id));
    }
}

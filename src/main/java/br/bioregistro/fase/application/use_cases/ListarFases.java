package br.bioregistro.fase.application.use_cases;

import br.bioregistro.fase.application.dtos.FaseResponse;
import br.bioregistro.fase.domain.exception.NaoEncontradoException;
import br.bioregistro.fase.domain.model.Instituicao;
import br.bioregistro.fase.domain.port.FaseRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

/** Fases do edital, em ordem crescente. */
@ApplicationScoped
public class ListarFases {

    private final FaseRepository fases;

    public ListarFases(FaseRepository fases) {
        this.fases = fases;
    }

    public List<FaseResponse> executar(Instituicao instituicao, int editalId) {
        if (!fases.editalExiste(instituicao, editalId)) {
            throw new NaoEncontradoException("Edital", editalId);
        }
        return fases.listarPorEdital(instituicao, editalId).stream()
                .map(FaseResponse::de)
                .toList();
    }
}

package br.bioregistro.fase.application.use_cases;

import br.bioregistro.fase.application.dtos.EtapaResponse;
import br.bioregistro.fase.domain.exception.NaoEncontradoException;
import br.bioregistro.fase.domain.model.Instituicao;
import br.bioregistro.fase.domain.port.EtapaRepository;
import br.bioregistro.fase.domain.port.FaseRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class ListarEtapas {

    private final FaseRepository fases;
    private final EtapaRepository etapas;

    public ListarEtapas(FaseRepository fases, EtapaRepository etapas) {
        this.fases = fases;
        this.etapas = etapas;
    }

    public List<EtapaResponse> executar(Instituicao instituicao, int faseId) {
        if (fases.buscar(instituicao, faseId).isEmpty()) {
            throw new NaoEncontradoException("Fase", faseId);
        }
        return etapas.listarPorFase(instituicao, faseId).stream()
                .map(EtapaResponse::de)
                .toList();
    }
}

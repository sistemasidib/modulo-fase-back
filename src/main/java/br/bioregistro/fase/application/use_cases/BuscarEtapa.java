package br.bioregistro.fase.application.use_cases;

import br.bioregistro.fase.application.dtos.EtapaResponse;
import br.bioregistro.fase.domain.exception.NaoEncontradoException;
import br.bioregistro.fase.domain.model.Instituicao;
import br.bioregistro.fase.domain.port.EtapaRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class BuscarEtapa {

    private final EtapaRepository etapas;

    public BuscarEtapa(EtapaRepository etapas) {
        this.etapas = etapas;
    }

    public EtapaResponse executar(Instituicao instituicao, int id) {
        return etapas.buscar(instituicao, id)
                .map(EtapaResponse::de)
                .orElseThrow(() -> new NaoEncontradoException("Etapa", id));
    }
}

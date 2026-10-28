package br.bioregistro.fase.application.use_cases;

import br.bioregistro.fase.application.dtos.EtapaResponse;
import br.bioregistro.fase.domain.exception.NaoEncontradoException;
import br.bioregistro.fase.domain.model.Instituicao;
import br.bioregistro.fase.domain.port.EtapaRepository;
import br.bioregistro.fase.domain.port.FaseRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.List;

/**
 * Lista etapas da fase. Garante Resultado Final (fases antigas) e o devolve sempre por último.
 */
@ApplicationScoped
public class ListarEtapas {

    private final FaseRepository fases;
    private final EtapaRepository etapas;
    private final ResultadoFinalDaFase resultadoFinal;
    private final EspacosDePublicacao espacos;

    public ListarEtapas(FaseRepository fases, EtapaRepository etapas, ResultadoFinalDaFase resultadoFinal,
                        EspacosDePublicacao espacos) {
        this.fases = fases;
        this.etapas = etapas;
        this.resultadoFinal = resultadoFinal;
        this.espacos = espacos;
    }

    @Transactional
    public List<EtapaResponse> executar(Instituicao instituicao, int faseId) {
        if (fases.buscar(instituicao, faseId).isEmpty()) {
            throw new NaoEncontradoException("Fase", faseId);
        }
        resultadoFinal.garantir(instituicao, faseId);
        espacos.sincronizarResultadoDaFase(instituicao, faseId);
        return resultadoFinal.comFinalPorUltimo(instituicao, etapas.listarPorFase(instituicao, faseId)).stream()
                .map(EtapaResponse::de)
                .toList();
    }
}

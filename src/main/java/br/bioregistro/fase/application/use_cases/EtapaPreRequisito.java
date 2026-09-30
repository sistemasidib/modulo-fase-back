package br.bioregistro.fase.application.use_cases;

import br.bioregistro.fase.domain.exception.NaoEncontradoException;
import br.bioregistro.fase.domain.model.Etapa;
import br.bioregistro.fase.domain.model.Instituicao;
import br.bioregistro.fase.domain.port.EtapaRepository;

/** A etapa de elegibilidade (pré-requisito), quando informada, precisa existir. */
final class EtapaPreRequisito {

    private EtapaPreRequisito() {
    }

    static void exigirExistente(EtapaRepository etapas, Instituicao instituicao, Etapa etapa) {
        Integer preRequisitoId = etapa.elegibilidadeEtapaId();
        if (preRequisitoId != null && etapas.buscar(instituicao, preRequisitoId).isEmpty()) {
            throw new NaoEncontradoException("Etapa de elegibilidade", preRequisitoId);
        }
    }
}

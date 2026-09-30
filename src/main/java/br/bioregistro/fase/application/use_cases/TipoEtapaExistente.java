package br.bioregistro.fase.application.use_cases;

import br.bioregistro.fase.domain.exception.NaoEncontradoException;
import br.bioregistro.fase.domain.model.Instituicao;
import br.bioregistro.fase.domain.model.TipoEtapa;
import br.bioregistro.fase.domain.port.TipoRepository;

/** O tipo da etapa precisa existir em {@code dom_tipo_etapa}. */
final class TipoEtapaExistente {

    private TipoEtapaExistente() {
    }

    static TipoEtapa exigir(TipoRepository tipos, Instituicao instituicao, int tipoId) {
        return tipos.tipoEtapa(instituicao, tipoId)
                .orElseThrow(() -> new NaoEncontradoException("Tipo de etapa", tipoId));
    }
}

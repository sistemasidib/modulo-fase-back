package br.bioregistro.fase.application.use_cases;

import br.bioregistro.fase.domain.exception.NaoEncontradoException;
import br.bioregistro.fase.domain.exception.RegraDeNegocioException;
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

    /**
     * Para etapas novas: análise de solicitações não é fase (spec 003, RN05). Etapas antigas
     * com esses tipos continuam editáveis por {@link #exigir}.
     */
    static TipoEtapa exigirNovo(TipoRepository tipos, Instituicao instituicao, int tipoId) {
        TipoEtapa tipo = exigir(tipos, instituicao, tipoId);
        if (tipo.categoria() == TipoEtapa.Categoria.ANALISE) {
            throw new RegraDeNegocioException("\"" + tipo.descricao()
                    + "\" é análise de solicitações e não pode ser etapa de fase. Use um tipo de execução.");
        }
        if (tipo.ehResultadoFinal()) {
            throw new RegraDeNegocioException(
                    "\"Resultado Final\" é obrigatória em toda fase e não se escolhe nas opções de fase do grupo.");
        }
        return tipo;
    }
}

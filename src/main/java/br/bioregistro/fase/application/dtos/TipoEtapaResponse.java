package br.bioregistro.fase.application.dtos;

import br.bioregistro.fase.domain.model.TipoEtapa;
import br.bioregistro.fase.domain.model.TipoPublicacao;

import java.util.List;

/**
 * @param categoria        EXECUCAO, ANALISE ou LEGADO (spec 003, RN05)
 * @param publicacoesPadrao ids dos tipos de publicação sugeridos para uma etapa deste tipo
 *                          ({@code dom_tipo_etapa_publicacao})
 */
public record TipoEtapaResponse(Integer id, String valor, String descricao, TipoEtapa.Categoria categoria,
                                List<Integer> publicacoesPadrao) {

    public static TipoEtapaResponse de(TipoEtapa tipo, List<TipoPublicacao> padrao) {
        return new TipoEtapaResponse(tipo.id(), tipo.valor(), tipo.descricao(), tipo.categoria(),
                padrao.stream().map(TipoPublicacao::id).toList());
    }
}

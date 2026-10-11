package br.bioregistro.fase.application.dtos;

import br.bioregistro.fase.domain.model.TipoEtapa;

/** @param categoria EXECUCAO, ANALISE ou LEGADO (spec 003, RN05) */
public record TipoEtapaResponse(Integer id, String valor, String descricao, TipoEtapa.Categoria categoria) {

    public static TipoEtapaResponse de(TipoEtapa tipo) {
        return new TipoEtapaResponse(tipo.id(), tipo.valor(), tipo.descricao(), tipo.categoria());
    }
}

package br.bioregistro.fase.application.dtos;

import br.bioregistro.fase.domain.model.TipoEtapa;

public record TipoEtapaResponse(Integer id, String valor, String descricao) {

    public static TipoEtapaResponse de(TipoEtapa tipo) {
        return new TipoEtapaResponse(tipo.id(), tipo.valor(), tipo.descricao());
    }
}

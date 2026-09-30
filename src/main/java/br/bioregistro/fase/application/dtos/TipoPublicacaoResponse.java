package br.bioregistro.fase.application.dtos;

import br.bioregistro.fase.domain.model.TipoPublicacao;

public record TipoPublicacaoResponse(Integer id, String valor, String descricao, boolean abreRecurso) {

    public static TipoPublicacaoResponse de(TipoPublicacao tipo) {
        return new TipoPublicacaoResponse(tipo.id(), tipo.valor(), tipo.descricao(), tipo.abreRecurso());
    }
}

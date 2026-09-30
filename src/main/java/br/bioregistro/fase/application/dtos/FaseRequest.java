package br.bioregistro.fase.application.dtos;

import br.bioregistro.fase.domain.model.Fase;

import java.time.OffsetDateTime;

/** Corpo de criação e edição de fase. O edital vem da URL. */
public record FaseRequest(
        String nome,
        String descricao,
        Short ordem,
        OffsetDateTime dataAbertura,
        OffsetDateTime dataFechamento
) {

    public Fase paraDominio(Integer id, Integer editalId) {
        return new Fase(id, editalId, nome, descricao, ordem, dataAbertura, dataFechamento);
    }
}

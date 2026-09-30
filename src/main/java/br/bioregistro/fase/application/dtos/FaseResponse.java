package br.bioregistro.fase.application.dtos;

import br.bioregistro.fase.domain.model.Fase;

import java.time.OffsetDateTime;

public record FaseResponse(
        Integer id,
        Integer editalId,
        String nome,
        String descricao,
        Short ordem,
        OffsetDateTime dataAbertura,
        OffsetDateTime dataFechamento
) {

    public static FaseResponse de(Fase fase) {
        return new FaseResponse(fase.id(), fase.editalId(), fase.nome(), fase.descricao(), fase.ordem(),
                fase.dataAbertura(), fase.dataFechamento());
    }
}

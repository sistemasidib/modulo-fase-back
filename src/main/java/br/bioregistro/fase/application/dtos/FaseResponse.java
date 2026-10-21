package br.bioregistro.fase.application.dtos;

import br.bioregistro.fase.domain.model.Fase;

import java.time.OffsetDateTime;
import java.util.List;

public record FaseResponse(
        Integer id,
        Integer editalId,
        String nome,
        String descricao,
        Short ordem,
        OffsetDateTime dataAbertura,
        OffsetDateTime dataFechamento,
        List<Integer> grupoIds,
        Integer gruposAfetados
) {

    public static FaseResponse de(Fase fase) {
        return de(fase, List.of());
    }

    public static FaseResponse de(Fase fase, List<Integer> grupoIds) {
        List<Integer> ids = grupoIds == null ? List.of() : List.copyOf(grupoIds);
        return new FaseResponse(fase.id(), fase.editalId(), fase.nome(), fase.descricao(), fase.ordem(),
                fase.dataAbertura(), fase.dataFechamento(), ids, ids.size());
    }
}

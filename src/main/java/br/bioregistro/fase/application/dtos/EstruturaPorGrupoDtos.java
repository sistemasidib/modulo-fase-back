package br.bioregistro.fase.application.dtos;

import java.util.List;

/** Leitura da estrutura por grupo (spec 006, RN01–RN03). */
public final class EstruturaPorGrupoDtos {

    private EstruturaPorGrupoDtos() {
    }

    public record GrupoEstruturaResponse(
            Integer id,
            String nome,
            List<Integer> cargoIds,
            List<FaseNaEstrutura> fases,
            List<CriterioDesempateDtos.CriterioDesempateResponse> criteriosDesempate
    ) {
    }

    public record FaseNaEstrutura(
            FaseResponse fase,
            List<Integer> grupoIds,
            int gruposAfetados,
            List<EtapaResponse> etapas
    ) {
    }
}

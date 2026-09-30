package br.bioregistro.fase.application.dtos;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Estrutura escolhida pelo usuário para o grupo: as fases e, em cada uma, os tipos de etapa
 * marcados (múltipla escolha). O nome da etapa é opcional; se vier vazio, usa o nome do tipo.
 */
public record GerarFasesRequest(List<FaseGerada> fases) {

    public record FaseGerada(
            String nome,
            String descricao,
            OffsetDateTime dataAbertura,
            OffsetDateTime dataFechamento,
            List<EtapaGerada> etapas
    ) {
    }

    public record EtapaGerada(Integer tipoId, String nome) {
    }
}

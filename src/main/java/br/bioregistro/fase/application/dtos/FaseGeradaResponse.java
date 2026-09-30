package br.bioregistro.fase.application.dtos;

import java.util.List;

public record FaseGeradaResponse(FaseResponse fase, List<EtapaResponse> etapas) {
}

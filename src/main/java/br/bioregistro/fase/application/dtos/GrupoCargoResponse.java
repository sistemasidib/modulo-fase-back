package br.bioregistro.fase.application.dtos;

import br.bioregistro.fase.domain.model.GrupoCargo;

import java.util.List;

public record GrupoCargoResponse(Integer id, Integer editalId, String nome, List<Integer> cargoIds) {

    public static GrupoCargoResponse de(GrupoCargo grupo, List<Integer> cargoIds) {
        return new GrupoCargoResponse(grupo.id(), grupo.editalId(), grupo.nome(), cargoIds);
    }
}

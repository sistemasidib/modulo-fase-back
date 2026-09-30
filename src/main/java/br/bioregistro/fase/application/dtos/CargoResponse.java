package br.bioregistro.fase.application.dtos;

import br.bioregistro.fase.domain.model.Cargo;

public record CargoResponse(Integer id, String descricao) {

    public static CargoResponse de(Cargo cargo) {
        return new CargoResponse(cargo.id(), cargo.descricao());
    }
}

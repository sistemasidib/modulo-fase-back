package br.bioregistro.fase.application.dtos;

import br.bioregistro.fase.domain.model.TipoEtapa;

import java.util.List;

/** Fase do concurso: tipo de etapa de execução e os grupos de cargos que têm fase desse tipo. */
public record FaseDoConcursoResponse(int ordem, int tipoId, String valor, String descricao, List<Integer> grupoIds) {

    public static FaseDoConcursoResponse de(int ordem, TipoEtapa tipo, List<Integer> grupoIds) {
        return new FaseDoConcursoResponse(ordem, tipo.id(), tipo.valor(), tipo.descricao(), grupoIds);
    }
}

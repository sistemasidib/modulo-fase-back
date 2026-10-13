package br.bioregistro.fase.application.dtos;

import br.bioregistro.fase.domain.model.CriterioCorrecao;

import java.math.BigDecimal;
import java.util.List;

public final class CriterioCorrecaoDtos {

    private CriterioCorrecaoDtos() {}

    /** A ordem não vem aqui: o novo vai para o fim e a ordem muda por {@link OrdemRequest}. */
    public record CriterioCorrecaoRequest(
            String modulo,
            String criterio,
            BigDecimal descontoPorErro,
            BigDecimal pontuacaoMaxima
    ) {}

    /** Todos os critérios da prova, do primeiro ao último. */
    public record OrdemRequest(List<Integer> ids) {}

    /** Provas discursivas que recebem uma cópia dos critérios (os delas são substituídos). */
    public record CopiarRequest(List<Integer> etapaIds) {}

    public record CriterioCorrecaoResponse(
            int id,
            int etapaId,
            int ordem,
            String modulo,
            String criterio,
            BigDecimal descontoPorErro,
            BigDecimal pontuacaoMaxima
    ) {
        public static CriterioCorrecaoResponse de(CriterioCorrecao c) {
            return new CriterioCorrecaoResponse(c.id(), c.etapaId(), c.ordem(), c.modulo(), c.criterio(),
                    c.descontoPorErro(), c.pontuacaoMaxima());
        }
    }
}

package br.bioregistro.fase.application.dtos;

import br.bioregistro.fase.domain.model.CriterioDesempate;
import br.bioregistro.fase.domain.model.CriterioDesempate.FaseAplicacao;
import br.bioregistro.fase.domain.model.CriterioDesempate.Regra;
import br.bioregistro.fase.domain.model.CriterioDesempate.Tipo;

import java.util.List;

public final class CriterioDesempateDtos {

    private CriterioDesempateDtos() {}

    /** A ordem não vem aqui: o novo vai para o fim e a ordem muda por {@link OrdemRequest}. */
    public record CriterioDesempateRequest(
            String criterio,
            Tipo tipo,
            Regra regra,
            String referencia,
            FaseAplicacao faseAplicacao,
            String fundamentacao,
            String observacao
    ) {}

    /** Todos os critérios da fase, do 1º ao último. */
    public record OrdemRequest(List<Integer> ids) {}

    /** Fases que recebem uma cópia dos critérios (os delas são substituídos). */
    public record CopiarRequest(List<Integer> faseIds) {}

    public record CriterioDesempateResponse(
            int id,
            int faseId,
            int ordem,
            String criterio,
            Tipo tipo,
            Regra regra,
            String referencia,
            FaseAplicacao faseAplicacao,
            String fundamentacao,
            String observacao
    ) {
        public static CriterioDesempateResponse de(CriterioDesempate c) {
            return new CriterioDesempateResponse(c.id(), c.faseId(), c.ordem(), c.criterio(), c.tipo(), c.regra(),
                    c.referencia(), c.faseAplicacao(), c.fundamentacao(), c.observacao());
        }
    }
}

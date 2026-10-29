package br.bioregistro.fase.application.dtos;

import br.bioregistro.fase.domain.model.CriterioDesempate;
import br.bioregistro.fase.domain.model.CriterioDesempate.Condicao;
import br.bioregistro.fase.domain.model.CriterioDesempate.Direcao;
import br.bioregistro.fase.domain.model.CriterioDesempate.Tipo;

import java.util.List;

public final class CriterioDesempateDtos {

    private CriterioDesempateDtos() {}

    /** A ordem não vem aqui: o novo vai para o fim e a ordem muda por {@link OrdemRequest}. */
    public record CriterioDesempateRequest(
            String label,
            Tipo tipo,
            Direcao direcao,
            Condicao condicao,
            String dataReferencia,
            String fonte,
            String baseLegal
    ) {}

    /** Todos os critérios da etapa, do 1º ao último. */
    public record OrdemRequest(List<Integer> ids) {}

    /** Etapas que recebem uma cópia dos critérios (os delas são substituídos). */
    public record CopiarRequest(List<Integer> etapaIds) {}

    public record CriterioDesempateResponse(
            int id,
            int etapaId,
            int ordem,
            String label,
            Tipo tipo,
            Direcao direcao,
            Condicao condicao,
            String dataReferencia,
            String fonte,
            String baseLegal
    ) {
        public static CriterioDesempateResponse de(CriterioDesempate c) {
            return new CriterioDesempateResponse(
                    c.id(), c.etapaId(), c.ordem(), c.label(), c.tipo(), c.direcao(),
                    c.condicao(), c.dataReferencia(), c.fonte(), c.baseLegal());
        }
    }
}

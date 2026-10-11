package br.bioregistro.fase.application.dtos;

import br.bioregistro.fase.domain.model.Concurso;
import br.bioregistro.fase.domain.model.EditalResumo;

import java.util.List;

/** Contrato HTTP do agrupamento de editais por concurso (spec 003, RB01–RB02). */
public final class ConcursoDtos {

    private ConcursoDtos() {
    }

    public record ConcursoRequest(String nome) {
    }

    public record ConcursoResponse(Integer id, String nome, List<EditalResumo> editais, Integer cronogramaId) {

        public static ConcursoResponse de(Concurso concurso, List<EditalResumo> editais, Integer cronogramaId) {
            return new ConcursoResponse(concurso.id(), concurso.nome(), editais, cronogramaId);
        }
    }

    /** Edital solto, com o concurso sugerido pelo mesmo nome ({@code ediNomeConcurso}). */
    public record EditalSemConcursoResponse(int id, String nome, Integer concursoSugeridoId,
                                            String concursoSugeridoNome) {
    }
}

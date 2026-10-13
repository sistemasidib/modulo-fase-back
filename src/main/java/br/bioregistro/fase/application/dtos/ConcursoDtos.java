package br.bioregistro.fase.application.dtos;

import br.bioregistro.fase.domain.model.Concurso;
import br.bioregistro.fase.domain.model.EditalResumo;

import java.util.List;
import java.util.Map;

/** Contrato HTTP do agrupamento de editais por concurso (spec 003, RB01–RB02). */
public final class ConcursoDtos {

    private ConcursoDtos() {
    }

    public record ConcursoRequest(String nome) {
    }

    /**
     * @param cronogramaId          cronograma aberto no concurso, compartilhado pelos editais que o seguem
     * @param cronogramasDosEditais cronograma que cada edital segue (o do concurso ou um separado)
     */
    public record ConcursoResponse(Integer id, String nome, List<EditalResumo> editais, Integer cronogramaId,
                                   List<CronogramaDoEdital> cronogramasDosEditais) {

        public static ConcursoResponse de(Concurso concurso, List<EditalResumo> editais, Integer cronogramaId,
                                          Map<Integer, Integer> dosEditais) {
            return new ConcursoResponse(concurso.id(), concurso.nome(), editais, cronogramaId,
                    dosEditais.entrySet().stream()
                            .map(p -> new CronogramaDoEdital(p.getKey(), p.getValue()))
                            .toList());
        }
    }

    public record CronogramaDoEdital(int editalId, int cronogramaId) {
    }

    /** Edital solto, com o concurso sugerido pelo mesmo nome ({@code ediNomeConcurso}). */
    public record EditalSemConcursoResponse(int id, String nome, Integer concursoSugeridoId,
                                            String concursoSugeridoNome) {
    }
}

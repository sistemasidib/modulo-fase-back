package br.bioregistro.fase.application.dtos;

import br.bioregistro.fase.domain.model.EditalResumo;
import br.bioregistro.fase.domain.model.Entidade;

import java.util.List;
import java.util.Map;

/** Contrato HTTP da entidade (órgão responsável pelos editais) e dos editais dela. */
public final class EntidadeDtos {

    private EntidadeDtos() {
    }

    /** @param cnpj opcional; com ou sem pontuação */
    public record EntidadeRequest(String nome, String cnpj) {
    }

    /** @param cronogramasDosEditais cronograma que cada edital da entidade segue (só os que seguem algum) */
    public record EntidadeResponse(Integer id, String nome, String cnpj, List<EditalResumo> editais,
                                   List<CronogramaDoEdital> cronogramasDosEditais) {

        public static EntidadeResponse de(Entidade entidade, List<EditalResumo> editais,
                                          Map<Integer, Integer> dosEditais) {
            return new EntidadeResponse(entidade.id(), entidade.nome(), entidade.cnpj(), editais,
                    dosEditais.entrySet().stream()
                            .map(p -> new CronogramaDoEdital(p.getKey(), p.getValue()))
                            .toList());
        }
    }

    public record CronogramaDoEdital(int editalId, int cronogramaId) {
    }

    /** Edital solto, com a entidade sugerida pelo mesmo nome de concurso ({@code ediNomeConcurso}). */
    public record EditalSemEntidadeResponse(int id, String nome, Integer entidadeSugeridaId,
                                            String entidadeSugeridaNome) {
    }
}

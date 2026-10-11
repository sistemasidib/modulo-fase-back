package br.bioregistro.fase.application.dtos;

import br.bioregistro.fase.domain.model.DisciplinaDaProva;

import java.math.BigDecimal;
import java.util.List;

public final class DisciplinasDtos {

    private DisciplinasDtos() {}

    public record PesoRequest(BigDecimal peso) {}

    /** {@code cargoIds} vazio = todos os cargos do grupo da etapa (uma faixa por cargo). */
    public record NovaDisciplinaRequest(String nome, Integer questaoInicio, Integer questaoFim, List<Integer> cargoIds) {}

    public record DisciplinaResponse(
            int faixaId,
            int disciplinaEditalId,
            int disciplinaId,
            String nome,
            int questaoInicio,
            int questaoFim,
            int questoes,
            Integer cargoId,
            String cargoNome,
            Integer etapaId,
            BigDecimal peso,
            BigDecimal pontos
    ) {
        public static DisciplinaResponse de(DisciplinaDaProva d) {
            return new DisciplinaResponse(d.faixaId(), d.disciplinaEditalId(), d.disciplinaId(), d.nome(),
                    d.questaoInicio(), d.questaoFim(), d.questoes(), d.cargoId(), d.cargoNome(), d.etapaId(), d.peso(),
                    d.pontos());
        }
    }

    /**
     * Sem totais: a faixa pode ser de um cargo só, então o total da prova é por cargo
     * (faixas do cargo + faixas de todos os cargos) e quem soma é a tela.
     * {@code grupoId} é nulo nas fases do modelo antigo, sem grupo.
     */
    public record DisciplinasDaEtapaResponse(int etapaId, int faseId, Integer grupoId, List<Integer> cargoIds,
                                             List<DisciplinaResponse> disciplinas) {
        public static DisciplinasDaEtapaResponse de(int etapaId, int faseId, Integer grupoId, List<Integer> cargoIds,
                                                    List<DisciplinaDaProva> disciplinas) {
            return new DisciplinasDaEtapaResponse(etapaId, faseId, grupoId, cargoIds,
                    disciplinas.stream().map(DisciplinaResponse::de).toList());
        }
    }
}

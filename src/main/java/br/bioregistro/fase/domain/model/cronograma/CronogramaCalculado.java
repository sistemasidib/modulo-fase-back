package br.bioregistro.fase.domain.model.cronograma;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Resultado do {@link CalculoCronograma}: grupos na ordem do esqueleto, indicadores e checagens.
 *
 * @param provaInformada a E15 vem da data da prova informada; {@code false} = sugestão provisória
 */
public record CronogramaCalculado(boolean materializado, LocalDate dataEdital, boolean provaInformada,
                                  List<Grupo> grupos, Indicadores indicadores, Checagens checagens) {

    public record Grupo(String codigo, String nome, boolean fase, String modulo, String nota, boolean ativo,
                        List<EtapaCalculada> etapas) {
    }

    public record Indicadores(int etapasAtivas, int etapasTotal, LocalDate edital, int provaRelativa,
                              LocalDate prova, int resultadoFinalRelativo, LocalDate resultadoFinal) {
    }

    public record Checagem(String codigo, String descricao, boolean cumprida, String detalhe) {
    }

    /**
     * RN24: "o cronograma viola alguma restrição?". Só {@code itens} contam para {@code viola} e
     * pedem justificativa; {@code alertas} só informam (spec 005, RN08 e RN09).
     */
    public record Checagens(boolean viola, List<Checagem> itens, List<Checagem> alertas) {
    }

    public List<EtapaCalculada> etapas() {
        return grupos.stream().flatMap(g -> g.etapas().stream()).toList();
    }

    public Optional<EtapaCalculada> etapa(String codigo) {
        return etapas().stream().filter(e -> e.codigo().equals(codigo)).findFirst();
    }
}

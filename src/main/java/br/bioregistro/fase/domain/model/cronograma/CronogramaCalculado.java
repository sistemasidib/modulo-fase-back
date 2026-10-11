package br.bioregistro.fase.domain.model.cronograma;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Resultado do {@link CalculoCronograma}: grupos na ordem do esqueleto, indicadores e checagens.
 *
 * @param ajusteDomingo dias somados à prova para cair no domingo (RN14); 0 no rascunho
 */
public record CronogramaCalculado(boolean materializado, LocalDate dataEdital, int ajusteDomingo,
                                  List<Grupo> grupos, Indicadores indicadores, Checagens checagens) {

    public record Grupo(String codigo, String nome, boolean fase, String modulo, String nota, boolean ativo,
                        List<EtapaCalculada> etapas) {
    }

    public record Indicadores(int etapasAtivas, int etapasTotal, LocalDate edital, int provaRelativa,
                              LocalDate prova, int resultadoFinalRelativo, LocalDate resultadoFinal) {
    }

    public record Checagem(String codigo, String descricao, boolean cumprida, String detalhe) {
    }

    /** RN24: "o cronograma viola alguma restrição?" */
    public record Checagens(boolean viola, List<Checagem> itens) {
    }

    public List<EtapaCalculada> etapas() {
        return grupos.stream().flatMap(g -> g.etapas().stream()).toList();
    }

    public Optional<EtapaCalculada> etapa(String codigo) {
        return etapas().stream().filter(e -> e.codigo().equals(codigo)).findFirst();
    }
}

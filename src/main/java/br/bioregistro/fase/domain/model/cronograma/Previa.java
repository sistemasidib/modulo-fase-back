package br.bioregistro.fase.domain.model.cronograma;

import java.util.List;

/**
 * Efeito de uma {@link Mudanca}, sem gravar nada.
 *
 * @param edicao        edição já com a mudança (e as compensações), pronta para aplicar
 * @param itens         a mudança, as compensações e as etapas que mudaram de data, nesta ordem
 * @param etapasMovidas quantas etapas mudam de data (sem contar a editada)
 */
public record Previa(Edicao edicao, List<Item> itens, int etapasMovidas) {

    /** Um item do lote: etapa, campo, antes → depois (RN27). */
    public record Item(String etapaCodigo, String campo, String antes, String depois) {
    }
}

package br.bioregistro.fase.domain.model.cronograma;

/**
 * Edição de uma célula da tabela, antes de ser aplicada (RN20). Sempre em cascata: as etapas que
 * dependem desta se movem junto (o modo "só esta" saiu na spec 005).
 */
public record Mudanca(String codigo, Campo campo, int valor) {

    public enum Campo { DIAS, DURACAO }
}

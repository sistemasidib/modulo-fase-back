package br.bioregistro.fase.domain.model.cronograma;

/** Edição de uma célula da tabela, antes de ser aplicada (RN20). */
public record Mudanca(String codigo, Campo campo, int valor, Modo modo) {

    public enum Campo { DIAS, DURACAO }

    public enum Modo {
        /** Aplicar e mover as etapas que dependem desta. */
        CASCATA,
        /** Mover só esta: as dependentes diretas são compensadas e ficam na mesma data. */
        SO_ESTA
    }
}

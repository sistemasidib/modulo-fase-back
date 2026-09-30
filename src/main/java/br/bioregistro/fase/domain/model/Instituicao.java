package br.bioregistro.fase.domain.model;

import br.bioregistro.fase.domain.exception.RegraDeNegocioException;

import java.util.Locale;

/** Instituição dona do edital. Cada uma tem seu próprio banco (datasource). */
public enum Instituicao {
    IDECAN,
    IDIB;

    /** Usado pelo JAX-RS para converter o path param ({@code idecan}, {@code idib}). */
    public static Instituicao fromString(String valor) {
        try {
            return valueOf(valor.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new RegraDeNegocioException("Instituição inválida: " + valor + ". Use idecan ou idib.");
        }
    }
}

package br.bioregistro.fase.domain.model;

import br.bioregistro.fase.domain.exception.RegraDeNegocioException;

/** Forma de realização das etapas S05 (heteroidentificação e biopsicossocial). */
public enum Modalidade {
    PRESENCIAL,
    /** Realização à distância (link / instruções). */
    REMOTA,
    /**
     * Alias legado de {@link #REMOTA}. Preferir {@code REMOTA} em código novo; a API ainda aceita
     * {@code ONLINE} e normaliza para {@code REMOTA}.
     */
    ONLINE;

    public static Modalidade de(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        try {
            Modalidade m = Modalidade.valueOf(valor.trim().toUpperCase());
            return m == ONLINE ? REMOTA : m;
        } catch (IllegalArgumentException e) {
            throw new RegraDeNegocioException("Modalidade inválida: use PRESENCIAL ou REMOTA.");
        }
    }

    /** Valor persistido / exposto na API (nunca {@code ONLINE}). */
    public Modalidade canonica() {
        return this == ONLINE ? REMOTA : this;
    }

    public boolean remota() {
        return this == REMOTA || this == ONLINE;
    }
}

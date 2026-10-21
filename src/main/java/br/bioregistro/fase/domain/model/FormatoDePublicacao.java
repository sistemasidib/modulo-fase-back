package br.bioregistro.fase.domain.model;

import br.bioregistro.fase.domain.exception.RegraDeNegocioException;

/** Formato em que a publicação fica disponível (spec 006, RN19). */
public enum FormatoDePublicacao {
    LISTA_PDF,
    TABELA_LINK;

    public static FormatoDePublicacao de(String valor) {
        if (valor == null || valor.isBlank()) {
            throw new RegraDeNegocioException("Informe o formato da publicação (LISTA_PDF ou TABELA_LINK).");
        }
        try {
            return FormatoDePublicacao.valueOf(valor.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new RegraDeNegocioException("Formato inválido: use LISTA_PDF ou TABELA_LINK.");
        }
    }
}

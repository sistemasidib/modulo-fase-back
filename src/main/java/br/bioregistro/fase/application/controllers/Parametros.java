package br.bioregistro.fase.application.controllers;

import br.bioregistro.fase.domain.exception.RegraDeNegocioException;

/** Checagem de entrada HTTP comum aos controllers. */
final class Parametros {

    private Parametros() {
    }

    static int obrigatorio(Integer valor, String nome) {
        if (valor == null) {
            throw new RegraDeNegocioException("O parâmetro " + nome + " é obrigatório.");
        }
        return valor;
    }

    static <T> T corpo(T request) {
        if (request == null) {
            throw new RegraDeNegocioException("O corpo da requisição é obrigatório.");
        }
        return request;
    }
}

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

    /**
     * Autor das ações do histórico, do header {@code X-Usuario} (URL-encoded pelo front, por causa
     * dos acentos). Pendência "Autor das ações" da spec 003-B: trocar pelo usuário autenticado.
     */
    static String autor(String header) {
        if (header == null || header.isBlank()) {
            return "não identificado";
        }
        String nome = java.net.URLDecoder.decode(header, java.nio.charset.StandardCharsets.UTF_8).strip();
        return nome.length() > 150 ? nome.substring(0, 150) : nome;
    }
}

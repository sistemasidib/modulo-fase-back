package br.bioregistro.fase.domain.model;

import br.bioregistro.fase.domain.exception.RegraDeNegocioException;

import java.time.OffsetDateTime;

/** Invariantes comuns. {@code sujeito} já leva o artigo: "da fase", "do grupo". */
final class Validacoes {

    private Validacoes() {
    }

    static String nomeObrigatorio(String nome, String sujeito, int tamanhoMaximo) {
        if (nome == null || nome.isBlank()) {
            throw new RegraDeNegocioException("O nome " + sujeito + " é obrigatório.");
        }
        String limpo = nome.strip();
        if (limpo.length() > tamanhoMaximo) {
            throw new RegraDeNegocioException(
                    "O nome " + sujeito + " deve ter no máximo " + tamanhoMaximo + " caracteres.");
        }
        return limpo;
    }

    static void periodoValido(OffsetDateTime abertura, OffsetDateTime fechamento, String sujeito) {
        if (abertura != null && fechamento != null && fechamento.isBefore(abertura)) {
            throw new RegraDeNegocioException(
                    "A data de fechamento " + sujeito + " não pode ser anterior à data de abertura.");
        }
    }
}

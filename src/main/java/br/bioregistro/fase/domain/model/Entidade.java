package br.bioregistro.fase.domain.model;

import br.bioregistro.fase.domain.exception.RegraDeNegocioException;

import java.util.Locale;

/**
 * Órgão responsável pelos editais (ex.: uma prefeitura). Um edital fica em no máximo uma entidade;
 * a entidade não tem cronograma: cada concurso dela segue o seu.
 *
 * @param cnpj opcional; guardado sem pontuação, em maiúsculas (aceita o CNPJ alfanumérico)
 */
public record Entidade(Integer id, String nome, String cnpj) {

    public static final int NOME_MAX = 200;

    private static final int[] PESOS_DV1 = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
    private static final int[] PESOS_DV2 = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};

    public Entidade {
        nome = Validacoes.nomeObrigatorio(nome, "da entidade", NOME_MAX);
        cnpj = cnpjValido(cnpj);
    }

    public Entidade comId(Integer novoId) {
        return new Entidade(novoId, nome, cnpj);
    }

    /** Vazio = sem CNPJ. Os 12 primeiros caracteres podem ser letras; os 2 dígitos verificadores, não. */
    static String cnpjValido(String cnpj) {
        if (cnpj == null || cnpj.isBlank()) {
            return null;
        }
        String limpo = cnpj.replaceAll("[.\\-/\\s]", "").toUpperCase(Locale.ROOT);
        if (!limpo.matches("[0-9A-Z]{12}[0-9]{2}") || limpo.chars().distinct().count() == 1
                || digito(limpo, PESOS_DV1) != limpo.charAt(12) - '0'
                || digito(limpo, PESOS_DV2) != limpo.charAt(13) - '0') {
            throw new RegraDeNegocioException("CNPJ inválido: " + cnpj.strip() + ".");
        }
        return limpo;
    }

    private static int digito(String cnpj, int[] pesos) {
        int soma = 0;
        for (int i = 0; i < pesos.length; i++) {
            soma += (cnpj.charAt(i) - '0') * pesos[i];
        }
        int resto = soma % 11;
        return resto < 2 ? 0 : 11 - resto;
    }
}

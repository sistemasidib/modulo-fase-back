package br.bioregistro.fase.domain.model;

import br.bioregistro.fase.domain.exception.RegraDeNegocioException;

/** Critério de desempate de uma fase (de um grupo); {@code ordem} 1 = primeiro a ser aplicado. */
public record CriterioDesempate(
        Integer id,
        int faseId,
        int ordem,
        String criterio,
        Tipo tipo,
        Regra regra,
        String referencia,
        FaseAplicacao faseAplicacao,
        String fundamentacao,
        String observacao
) {

    public enum Tipo { IDADE, NOTA, DISCIPLINA, SORTEIO, OUTRO }

    public enum Regra { MAIOR, MENOR, SIM_NAO }

    public enum FaseAplicacao { PROVA_OBJETIVA, RESULTADO_FINAL, AMBAS }

    public static final int CRITERIO_MAX = 300;
    public static final int REFERENCIA_MAX = 200;
    public static final int FUNDAMENTACAO_MAX = 300;
    public static final int OBSERVACAO_MAX = 500;

    public CriterioDesempate {
        if (ordem < 1) {
            throw new RegraDeNegocioException("A ordem do critério deve ser 1 ou maior.");
        }
        if (criterio == null || criterio.isBlank()) {
            throw new RegraDeNegocioException("Descreva o critério de desempate.");
        }
        criterio = limite(criterio.strip(), "A descrição do critério", CRITERIO_MAX);
        if (tipo == null) {
            throw new RegraDeNegocioException("Escolha o tipo do critério.");
        }
        if (regra == null) {
            throw new RegraDeNegocioException("Escolha a regra do critério.");
        }
        if (faseAplicacao == null) {
            throw new RegraDeNegocioException("Escolha em que fase o critério se aplica.");
        }
        referencia = opcional(referencia, "A referência", REFERENCIA_MAX);
        fundamentacao = opcional(fundamentacao, "A fundamentação", FUNDAMENTACAO_MAX);
        observacao = opcional(observacao, "A observação", OBSERVACAO_MAX);
        if (tipo == Tipo.DISCIPLINA && referencia == null) {
            throw new RegraDeNegocioException("Informe na referência a disciplina do critério.");
        }
    }

    public CriterioDesempate comId(int novoId) {
        return new CriterioDesempate(novoId, faseId, ordem, criterio, tipo, regra, referencia, faseAplicacao,
                fundamentacao, observacao);
    }

    public CriterioDesempate comOrdem(int novaOrdem) {
        return new CriterioDesempate(id, faseId, novaOrdem, criterio, tipo, regra, referencia, faseAplicacao,
                fundamentacao, observacao);
    }

    private static String opcional(String texto, String sujeito, int maximo) {
        return texto == null || texto.isBlank() ? null : limite(texto.strip(), sujeito, maximo);
    }

    private static String limite(String texto, String sujeito, int maximo) {
        if (texto.length() > maximo) {
            throw new RegraDeNegocioException(sujeito + " deve ter no máximo " + maximo + " caracteres.");
        }
        return texto;
    }
}

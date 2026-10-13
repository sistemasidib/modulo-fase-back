package br.bioregistro.fase.domain.model;

import br.bioregistro.fase.domain.exception.RegraDeNegocioException;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Critério de correção de uma prova discursiva, dentro de um módulo (ex.: Formal, Textual, Técnico).
 * {@code descontoPorErro} nulo = a banca atribui a nota direto, sem descontar por erro.
 */
public record CriterioCorrecao(
        Integer id,
        int etapaId,
        int ordem,
        String modulo,
        String criterio,
        BigDecimal descontoPorErro,
        BigDecimal pontuacaoMaxima
) {

    public static final int MODULO_MAX = 100;
    public static final int CRITERIO_MAX = 500;
    /** {@code DECIMAL(6,2)}. */
    public static final BigDecimal VALOR_MAXIMO = new BigDecimal("9999.99");

    public CriterioCorrecao {
        if (ordem < 1) {
            throw new RegraDeNegocioException("A ordem do critério deve ser 1 ou maior.");
        }
        modulo = obrigatorio(modulo, "Informe o módulo do critério (ex.: Formal).", "O módulo", MODULO_MAX);
        criterio = obrigatorio(criterio, "Descreva o critério de correção.", "A descrição do critério", CRITERIO_MAX);
        if (pontuacaoMaxima == null) {
            throw new RegraDeNegocioException("Informe a pontuação máxima do critério.");
        }
        pontuacaoMaxima = valor(pontuacaoMaxima, "A pontuação máxima");
        if (descontoPorErro != null) {
            descontoPorErro = valor(descontoPorErro, "O desconto por erro");
            if (descontoPorErro.compareTo(pontuacaoMaxima) > 0) {
                throw new RegraDeNegocioException("O desconto por erro não pode passar da pontuação máxima.");
            }
        }
    }

    public CriterioCorrecao comId(int novoId) {
        return new CriterioCorrecao(novoId, etapaId, ordem, modulo, criterio, descontoPorErro, pontuacaoMaxima);
    }

    public CriterioCorrecao comOrdem(int novaOrdem) {
        return new CriterioCorrecao(id, etapaId, novaOrdem, modulo, criterio, descontoPorErro, pontuacaoMaxima);
    }

    private static String obrigatorio(String texto, String vazio, String sujeito, int maximo) {
        if (texto == null || texto.isBlank()) {
            throw new RegraDeNegocioException(vazio);
        }
        String limpo = texto.strip();
        if (limpo.length() > maximo) {
            throw new RegraDeNegocioException(sujeito + " deve ter no máximo " + maximo + " caracteres.");
        }
        return limpo;
    }

    /** Maior que zero, até {@link #VALOR_MAXIMO} e com no máximo duas casas. */
    private static BigDecimal valor(BigDecimal valor, String sujeito) {
        if (valor.signum() <= 0) {
            throw new RegraDeNegocioException(sujeito + " deve ser maior que zero.");
        }
        if (valor.compareTo(VALOR_MAXIMO) > 0) {
            throw new RegraDeNegocioException(sujeito + " deve ser no máximo " + VALOR_MAXIMO + ".");
        }
        if (valor.stripTrailingZeros().scale() > 2) {
            throw new RegraDeNegocioException(sujeito + " deve ter no máximo duas casas decimais.");
        }
        return valor.setScale(2, RoundingMode.UNNECESSARY);
    }
}

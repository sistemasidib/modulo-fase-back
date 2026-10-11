package br.bioregistro.fase.domain.model;

import br.bioregistro.fase.domain.exception.RegraDeNegocioException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;

/**
 * Faixa de questões de uma disciplina do edital ({@code DisciplinasEditaisQuestoes}), com o valor
 * de cada questão. {@code cargoId} nulo = a faixa vale para todos os cargos do edital.
 * {@code etapaId} é a prova a que a faixa pertence; nulo = faixa antiga, da prova objetiva.
 */
public record DisciplinaDaProva(
        int faixaId,
        int disciplinaEditalId,
        int disciplinaId,
        String nome,
        int questaoInicio,
        int questaoFim,
        Integer cargoId,
        String cargoNome,
        Integer etapaId,
        BigDecimal peso
) {

    public static final BigDecimal PESO_MAXIMO = new BigDecimal("100");
    /** {@code Disciplinas.nome} é VARCHAR(150). */
    public static final int NOME_MAX = 150;

    public int questoes() {
        return questaoFim - questaoInicio + 1;
    }

    /** Nulo enquanto o peso não foi definido. */
    public BigDecimal pontos() {
        return peso == null ? null : peso.multiply(BigDecimal.valueOf(questoes())).setScale(2, RoundingMode.HALF_UP);
    }

    public boolean valePara(Collection<Integer> cargos) {
        return cargoId == null || cargos.contains(cargoId);
    }

    /** A faixa vale para o cargo e tem alguma questão entre {@code inicio} e {@code fim}? */
    public boolean conflitaCom(int cargo, int inicio, int fim) {
        return (cargoId == null || cargoId == cargo) && questaoInicio <= fim && inicio <= questaoFim;
    }

    public DisciplinaDaProva comPeso(BigDecimal novo) {
        return new DisciplinaDaProva(faixaId, disciplinaEditalId, disciplinaId, nome, questaoInicio, questaoFim,
                cargoId, cargoNome, etapaId, novo);
    }

    /** Peso com duas casas, maior que zero e até {@link #PESO_MAXIMO}. */
    public static BigDecimal pesoValido(BigDecimal peso) {
        if (peso == null) {
            throw new RegraDeNegocioException("Informe o valor da questão.");
        }
        if (peso.signum() <= 0) {
            throw new RegraDeNegocioException("O valor da questão deve ser maior que zero.");
        }
        if (peso.compareTo(PESO_MAXIMO) > 0) {
            throw new RegraDeNegocioException("O valor da questão deve ser no máximo " + PESO_MAXIMO + ".");
        }
        if (peso.stripTrailingZeros().scale() > 2) {
            throw new RegraDeNegocioException("Use no máximo duas casas decimais no valor da questão.");
        }
        return peso.setScale(2, RoundingMode.UNNECESSARY);
    }

    /** Nome sem espaços nas pontas, obrigatório e com até {@link #NOME_MAX} caracteres. */
    public static String nomeValido(String nome) {
        String limpo = nome == null ? "" : nome.strip();
        if (limpo.isEmpty()) {
            throw new RegraDeNegocioException("Informe o nome da disciplina.");
        }
        if (limpo.length() > NOME_MAX) {
            throw new RegraDeNegocioException("O nome da disciplina deve ter no máximo " + NOME_MAX + " caracteres.");
        }
        return limpo;
    }

    public static void faixaValida(Integer inicio, Integer fim) {
        if (inicio == null || fim == null) {
            throw new RegraDeNegocioException("Informe a primeira e a última questão.");
        }
        if (inicio < 1) {
            throw new RegraDeNegocioException("A primeira questão deve ser 1 ou maior.");
        }
        if (fim < inicio) {
            throw new RegraDeNegocioException("A última questão não pode ser menor que a primeira.");
        }
    }
}

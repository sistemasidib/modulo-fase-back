package br.bioregistro.fase.domain.model;

import br.bioregistro.fase.domain.exception.RegraDeNegocioException;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/** Critério de desempate de uma fase (de um grupo); {@code ordem} 1 = primeiro a ser aplicado. */
public record CriterioDesempate(
        Integer id,
        int faseId,
        int ordem,
        String label,
        Tipo tipo,
        Direcao direcao,
        Condicao condicao,
        String dataReferencia,
        String fonte,
        String baseLegal
) {

    public enum Tipo {
        idade, nota, booleano;

        @JsonValue
        public String json() {
            return name();
        }

        @JsonCreator
        public static Tipo de(String valor) {
            if (valor == null || valor.isBlank()) {
                throw new RegraDeNegocioException("Escolha o tipo do critério.");
            }
            String v = valor.strip().toLowerCase();
            return switch (v) {
                case "idade" -> idade;
                case "nota", "disciplina" -> nota;
                case "booleano", "sorteio", "outro" -> booleano;
                default -> throw new RegraDeNegocioException("Tipo de critério inválido: " + valor + ".");
            };
        }
    }

    public enum Direcao {
        asc, desc;

        @JsonValue
        public String json() {
            return name();
        }

        @JsonCreator
        public static Direcao de(String valor) {
            if (valor == null || valor.isBlank()) {
                throw new RegraDeNegocioException("Escolha a direção do critério.");
            }
            try {
                return Direcao.valueOf(valor.strip().toLowerCase());
            } catch (IllegalArgumentException e) {
                throw new RegraDeNegocioException("Direção inválida: " + valor + ".");
            }
        }
    }

    /** Condição opcional (ex.: idade &gt;= 60 anos, nota &gt; 0,5%). */
    public record Condicao(String operador, Object valor, String unidade) {
        public Condicao {
            if (operador == null || operador.isBlank()) {
                throw new RegraDeNegocioException("Informe o operador da condição.");
            }
            operador = operador.strip();
            if (valor == null) {
                throw new RegraDeNegocioException("Informe o valor da condição.");
            }
            unidade = unidade == null || unidade.isBlank() ? null : unidade.strip();
            if (unidade != null && unidade.length() > UNIDADE_MAX) {
                throw new RegraDeNegocioException("A unidade deve ter no máximo " + UNIDADE_MAX + " caracteres.");
            }
            if (operador.length() > OPERADOR_MAX) {
                throw new RegraDeNegocioException("O operador deve ter no máximo " + OPERADOR_MAX + " caracteres.");
            }
        }
    }

    public static final int LABEL_MAX = 300;
    public static final int DATA_REFERENCIA_MAX = 80;
    public static final int FONTE_MAX = 120;
    public static final int BASE_LEGAL_MAX = 80;
    public static final int OPERADOR_MAX = 10;
    public static final int UNIDADE_MAX = 40;

    public CriterioDesempate {
        if (ordem < 1) {
            throw new RegraDeNegocioException("A ordem do critério deve ser 1 ou maior.");
        }
        if (label == null || label.isBlank()) {
            throw new RegraDeNegocioException("Descreva o critério de desempate.");
        }
        label = limite(label.strip(), "O rótulo do critério", LABEL_MAX);
        if (tipo == null) {
            throw new RegraDeNegocioException("Escolha o tipo do critério.");
        }
        if (direcao == null) {
            throw new RegraDeNegocioException("Escolha a direção do critério.");
        }
        dataReferencia = opcional(dataReferencia, "A data de referência", DATA_REFERENCIA_MAX);
        fonte = opcional(fonte, "A fonte", FONTE_MAX);
        baseLegal = opcional(baseLegal, "A base legal", BASE_LEGAL_MAX);
        if (tipo == Tipo.nota && fonte == null) {
            throw new RegraDeNegocioException("Informe a fonte da nota (ex.: prova_objetiva, disciplina).");
        }
    }

    public CriterioDesempate comId(int novoId) {
        return new CriterioDesempate(novoId, faseId, ordem, label, tipo, direcao, condicao,
                dataReferencia, fonte, baseLegal);
    }

    public CriterioDesempate comOrdem(int novaOrdem) {
        return new CriterioDesempate(id, faseId, novaOrdem, label, tipo, direcao, condicao,
                dataReferencia, fonte, baseLegal);
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

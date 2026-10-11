package br.bioregistro.fase.domain.model.cronograma;

/** De onde a etapa conta: marco zero, início ou fim de outra etapa, ou último resultado definitivo. */
public record Referencia(Tipo tipo, String codigo) {

    public enum Tipo { MARCO_ZERO, INICIO, FIM, ULTIMO_RESULTADO }

    public static final Referencia MARCO_ZERO = new Referencia(Tipo.MARCO_ZERO, null);
    public static final Referencia ULTIMO_RESULTADO = new Referencia(Tipo.ULTIMO_RESULTADO, null);

    public static Referencia inicio(String codigo) {
        return new Referencia(Tipo.INICIO, codigo);
    }

    public static Referencia fim(String codigo) {
        return new Referencia(Tipo.FIM, codigo);
    }

    public String texto() {
        return switch (tipo) {
            case MARCO_ZERO -> "marco zero (D0)";
            case INICIO -> "início de " + codigo;
            case FIM -> "fim de " + codigo;
            case ULTIMO_RESULTADO -> "último resultado definitivo";
        };
    }
}

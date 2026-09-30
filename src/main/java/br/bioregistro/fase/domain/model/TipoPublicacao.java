package br.bioregistro.fase.domain.model;

/**
 * Linha de {@code dom_tipo_publicacao}. {@code abreRecurso}: publicar este tipo abre o
 * período de recurso (gabarito preliminar, resultado preliminar).
 */
public record TipoPublicacao(Integer id, String valor, String descricao, boolean abreRecurso) {

    public static final String RESULTADO_PRELIMINAR = "resultado_preliminar";
    public static final String RESULTADO_OFICIAL = "resultado_oficial";
}

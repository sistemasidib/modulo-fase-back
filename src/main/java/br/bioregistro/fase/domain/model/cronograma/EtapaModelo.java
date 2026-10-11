package br.bioregistro.fase.domain.model.cronograma;

import java.util.Set;

/**
 * Etapa do esqueleto. {@code parametroDias}/{@code parametroDuracao}, quando presentes, dizem
 * que o valor do modelo vem de {@link Parametros} (RN17) e não de {@code dias}/{@code duracao}.
 *
 * @param resultadoFinal último resultado definitivo de um bloco: a E60 conta a partir do maior deles
 */
public record EtapaModelo(String codigo, String descricao, String grupo, Referencia referencia,
                          Integer dias, Integer faixaP25, Integer faixaP75, int duracao, int baseConcursos,
                          TipoStatus tipoStatus, String parametroDias, String parametroDuracao,
                          boolean resultadoFinal) {

    /** RN22: âncoras do cronograma. */
    public static final Set<String> NAO_REMOVIVEIS = Set.of("E01", "E04", "E15", "E60");

    public static final String PROVA = "E15";
    public static final String INSCRICOES = "E04";
    public static final String EDITAL = "E01";
    public static final String RESULTADO_FINAL = "E60";

    public boolean removivel() {
        return !NAO_REMOVIVEIS.contains(codigo);
    }

    /** A E01 é o marco zero: não tem "dias" para editar. */
    public boolean temDias() {
        return dias != null || parametroDias != null;
    }

    public int diasModelo(Parametros parametros) {
        if (parametroDias != null) {
            return parametros.valor(parametroDias);
        }
        return dias == null ? 0 : dias;
    }

    public int duracaoModelo(Parametros parametros) {
        return parametroDuracao != null ? parametros.valor(parametroDuracao) : duracao;
    }

    public boolean temFaixa() {
        return faixaP25 != null && faixaP75 != null;
    }
}

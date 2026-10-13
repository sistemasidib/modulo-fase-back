package br.bioregistro.fase.domain.model.cronograma;

import java.time.LocalDate;

/**
 * Linha de leitura do cronograma de um edital ({@code cronograma_edital_etapa}): datas e status
 * efetivos, já com a sobreposição do edital. É para onde a publicação aponta.
 *
 * @param ativa  módulo ligado e etapa não removida; inativa = sem data
 * @param inicio {@code null} no rascunho
 */
public record LinhaDoCronograma(String codigo, boolean ativa, LocalDate inicio, LocalDate fim, StatusEtapa status) {

    public boolean comData() {
        return ativa && inicio != null;
    }

    /**
     * Publicação vinculada ao cronograma: a linha com a data dela e, se abre recurso, a linha do
     * recurso.
     */
    public record DaPublicacao(LinhaDoCronograma data, LinhaDoCronograma recurso) {
    }
}

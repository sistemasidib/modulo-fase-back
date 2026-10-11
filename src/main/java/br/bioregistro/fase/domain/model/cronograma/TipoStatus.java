package br.bioregistro.fase.domain.model.cronograma;

/** Família da etapa, que define o fluxo de status (RN30). */
public enum TipoStatus {
    /** Períodos, realizações, últimos dias: A fazer → Feito. */
    ATIVIDADE,
    /** Recurso e impugnação: "Em análise" só pelo gatilho do 1º recurso. */
    RECURSO,
    /** Demais publicações. */
    PUBLICACAO
}

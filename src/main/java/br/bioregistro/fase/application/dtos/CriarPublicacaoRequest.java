package br.bioregistro.fase.application.dtos;

/** Espaço de publicação extra. {@code etapaId} nulo: publicação da própria fase. */
public record CriarPublicacaoRequest(Integer tipoPublicacaoId, Integer etapaId) {
}

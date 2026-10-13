package br.bioregistro.fase.application.dtos;

import java.util.List;

/** @param tipoPublicacaoIds tipos de publicação da fase do concurso (vazia = nenhum) */
public record PublicacoesDaFaseRequest(List<Integer> tipoPublicacaoIds) {
}

package br.bioregistro.fase.application.dtos;

/** Formato de erro devolvido ao front (mesmo campo {@code message} dos outros módulos). */
public record ErroResponse(String message) {
}

package br.bioregistro.fase.domain.model.cronograma;

import br.bioregistro.fase.domain.exception.RegraDeNegocioException;

import java.time.LocalDate;

/**
 * Linha do cronograma público no formato Anexo II: descrição + início + término.
 */
public record ItemCronogramaPublico(
        int ordem,
        String descricao,
        LocalDate inicio,
        LocalDate termino,
        boolean destaque
) {

    public ItemCronogramaPublico {
        if (ordem < 1) {
            throw new RegraDeNegocioException("A ordem do item deve ser maior que zero.");
        }
        if (descricao == null || descricao.isBlank()) {
            throw new RegraDeNegocioException("Informe a descrição da atividade.");
        }
        descricao = descricao.trim();
        if (descricao.length() > 500) {
            throw new RegraDeNegocioException("A descrição da atividade pode ter no máximo 500 caracteres.");
        }
        if (inicio == null) {
            throw new RegraDeNegocioException("Informe a data de início.");
        }
        if (termino == null) {
            throw new RegraDeNegocioException("Informe a data de término.");
        }
        if (termino.isBefore(inicio)) {
            throw new RegraDeNegocioException("A data de término não pode ser anterior à de início.");
        }
    }
}

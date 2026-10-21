package br.bioregistro.fase.domain.model;

import br.bioregistro.fase.domain.exception.RegraDeNegocioException;

import java.time.LocalDate;

/**
 * Configuração de um tipo de publicação: referência no cronograma, dias no ar e formato
 * (spec 006, RN19–RN22). A data de saída continua vindo do cronograma (RN33 da 005); o período
 * de visualização é derivado ({@link #fimDaVisualizacao}).
 */
public record ConfiguracaoDePublicacao(
        Integer tipoPublicacaoId,
        String referenciaCodigo,
        Integer diasAposReferencia,
        int diasNoAr,
        FormatoDePublicacao formato
) {

    public ConfiguracaoDePublicacao {
        if (tipoPublicacaoId == null) {
            throw new RegraDeNegocioException("O tipo de publicação da configuração é obrigatório.");
        }
        if (diasNoAr <= 0) {
            throw new RegraDeNegocioException("Os dias no ar devem ser maiores que zero.");
        }
        if (formato == null) {
            throw new RegraDeNegocioException("Informe o formato da publicação (LISTA_PDF ou TABELA_LINK).");
        }
        if (referenciaCodigo != null) {
            referenciaCodigo = referenciaCodigo.isBlank() ? null : referenciaCodigo.trim();
        }
    }

    /** Fim da visualização = data de saída + (dias no ar − 1), inclusive no dia da saída. */
    public LocalDate fimDaVisualizacao(LocalDate dataSaida) {
        if (dataSaida == null) {
            return null;
        }
        return dataSaida.plusDays(diasNoAr - 1L);
    }
}

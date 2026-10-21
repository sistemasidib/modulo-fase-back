package br.bioregistro.fase.application.dtos;

import br.bioregistro.fase.domain.model.ConfiguracaoDePublicacao;
import br.bioregistro.fase.domain.model.FormatoDePublicacao;

public final class ConfigPublicacaoDtos {

    private ConfigPublicacaoDtos() {
    }

    public record ConfigPublicacaoRequest(
            String referenciaCodigo,
            Integer diasAposReferencia,
            Integer diasNoAr,
            String formato
    ) {
        public ConfiguracaoDePublicacao paraDominio(int tipoPublicacaoId) {
            if (diasNoAr == null) {
                throw new br.bioregistro.fase.domain.exception.RegraDeNegocioException(
                        "Informe os dias no ar.");
            }
            return new ConfiguracaoDePublicacao(tipoPublicacaoId, referenciaCodigo, diasAposReferencia,
                    diasNoAr, FormatoDePublicacao.de(formato));
        }
    }

    public record ConfigPublicacaoResponse(
            Integer tipoPublicacaoId,
            String referenciaCodigo,
            Integer diasAposReferencia,
            int diasNoAr,
            String formato
    ) {
        public static ConfigPublicacaoResponse de(ConfiguracaoDePublicacao c) {
            return new ConfigPublicacaoResponse(c.tipoPublicacaoId(), c.referenciaCodigo(),
                    c.diasAposReferencia(), c.diasNoAr(), c.formato().name());
        }
    }
}

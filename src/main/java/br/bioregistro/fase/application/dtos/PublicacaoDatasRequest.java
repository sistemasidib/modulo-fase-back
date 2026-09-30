package br.bioregistro.fase.application.dtos;

import java.time.OffsetDateTime;

/** Datas informadas pelo usuário ao preencher ou publicar. Recurso só para tipos que abrem recurso. */
public record PublicacaoDatasRequest(
        OffsetDateTime dataPrevista,
        OffsetDateTime visualizacaoInicio,
        OffsetDateTime visualizacaoFim,
        OffsetDateTime recursoInicio,
        OffsetDateTime recursoFim
) {
}

package br.bioregistro.fase.application.dtos;

import br.bioregistro.fase.domain.model.Publicacao;

import java.time.OffsetDateTime;

public record PublicacaoResponse(
        Integer id,
        Integer faseId,
        Integer etapaId,
        Integer tipoPublicacaoId,
        String tipo,
        String tipoDescricao,
        boolean abreRecurso,
        OffsetDateTime dataPrevista,
        OffsetDateTime visualizacaoInicio,
        OffsetDateTime visualizacaoFim,
        OffsetDateTime recursoInicio,
        OffsetDateTime recursoFim,
        OffsetDateTime publicadaEm,
        boolean recursoAberto
) {

    public static PublicacaoResponse de(Publicacao p, OffsetDateTime agora) {
        return new PublicacaoResponse(p.id(), p.faseId(), p.etapaId(), p.tipo().id(), p.tipo().valor(),
                p.tipo().descricao(), p.tipo().abreRecurso(), p.dataPrevista(), p.visualizacaoInicio(),
                p.visualizacaoFim(), p.recursoInicio(), p.recursoFim(), p.publicadaEm(), p.recursoAberto(agora));
    }
}

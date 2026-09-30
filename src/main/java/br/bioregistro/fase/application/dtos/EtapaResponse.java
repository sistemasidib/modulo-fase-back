package br.bioregistro.fase.application.dtos;

import br.bioregistro.fase.domain.model.Etapa;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

public record EtapaResponse(
        Integer id,
        Integer faseId,
        String nome,
        Integer tipoId,
        String descricao,
        OffsetDateTime dataAbertura,
        OffsetDateTime dataFechamento,
        Integer statusId,
        boolean reprovavel,
        boolean obrigatoriaParaFase,
        boolean todosCandidatos,
        boolean aceitaDocumentos,
        Boolean aceitaRecurso,
        Boolean preliminar,
        Integer elegibilidadeEtapaId,
        Integer elegibilidadeStatusId,
        Integer responsavelId,
        LocalDateTime publicadaEm
) {

    public static EtapaResponse de(Etapa e) {
        return new EtapaResponse(e.id(), e.faseId(), e.nome(), e.tipoId(), e.descricao(), e.dataAbertura(),
                e.dataFechamento(), e.statusId(), e.reprovavel(), e.obrigatoriaParaFase(), e.todosCandidatos(),
                e.aceitaDocumentos(), e.aceitaRecurso(), e.preliminar(), e.elegibilidadeEtapaId(),
                e.elegibilidadeStatusId(), e.responsavelId(), e.publicadaEm());
    }
}

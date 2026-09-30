package br.bioregistro.fase.application.dtos;

import br.bioregistro.fase.domain.model.Etapa;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

/** Corpo de criação e edição de etapa. A fase vem da URL. Flags ausentes valem {@code false}. */
public record EtapaRequest(
        String nome,
        Integer tipoId,
        String descricao,
        OffsetDateTime dataAbertura,
        OffsetDateTime dataFechamento,
        Integer statusId,
        Boolean reprovavel,
        Boolean obrigatoriaParaFase,
        Boolean todosCandidatos,
        Boolean aceitaDocumentos,
        Boolean aceitaRecurso,
        Boolean preliminar,
        Integer elegibilidadeEtapaId,
        Integer elegibilidadeStatusId,
        Integer responsavelId,
        LocalDateTime publicadaEm
) {

    public Etapa paraDominio(Integer id, Integer faseId) {
        return new Etapa(id, faseId, nome, tipoId, descricao, dataAbertura, dataFechamento, statusId,
                Boolean.TRUE.equals(reprovavel),
                Boolean.TRUE.equals(obrigatoriaParaFase),
                Boolean.TRUE.equals(todosCandidatos),
                Boolean.TRUE.equals(aceitaDocumentos),
                aceitaRecurso, preliminar, elegibilidadeEtapaId, elegibilidadeStatusId, responsavelId, publicadaEm);
    }
}

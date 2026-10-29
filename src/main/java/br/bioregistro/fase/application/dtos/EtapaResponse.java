package br.bioregistro.fase.application.dtos;

import br.bioregistro.fase.domain.model.Etapa;

import java.math.BigDecimal;
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
        LocalDateTime publicadaEm,
        Boolean notaZeroElimina,
        BigDecimal notaMinimaPercentual,
        String modalidade,
        Integer diasRecurso,
        BigDecimal pontuacaoMaxima,
        BigDecimal notaMinimaEmPontos,
        String aviso
) {

    public static EtapaResponse de(Etapa e) {
        return de(e, null, null);
    }

    public static EtapaResponse de(Etapa e, BigDecimal pontuacaoMaxima) {
        return de(e, pontuacaoMaxima, null);
    }

    public static EtapaResponse de(Etapa e, BigDecimal pontuacaoMaxima, String aviso) {
        BigDecimal minima = Etapa.notaMinimaEmPontos(e.notaMinimaPercentual(), pontuacaoMaxima);
        return new EtapaResponse(e.id(), e.faseId(), e.nome(), e.tipoId(), e.descricao(), e.dataAbertura(),
                e.dataFechamento(), e.statusId(), e.reprovavel(), e.obrigatoriaParaFase(), e.todosCandidatos(),
                e.aceitaDocumentos(), e.aceitaRecurso(), e.preliminar(), e.elegibilidadeEtapaId(),
                e.elegibilidadeStatusId(), e.responsavelId(), e.publicadaEm(), e.notaZeroElimina(),
                e.notaMinimaPercentual(),
                e.modalidade() == null ? null : e.modalidade().canonica().name(),
                e.diasRecurso(),
                pontuacaoMaxima, minima, aviso);
    }
}

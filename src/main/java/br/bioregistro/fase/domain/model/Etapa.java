package br.bioregistro.fase.domain.model;

import br.bioregistro.fase.domain.exception.RegraDeNegocioException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;

/**
 * Etapa de uma fase. Espelha {@code dbo.etapa}; as datas são opcionais porque a
 * etapa pode existir antes de ter data definida. Discursiva e S05 ganham campos da spec 006.
 */
public record Etapa(
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
        Modalidade modalidade
) {

    public static final int NOME_MAX = 150;
    /** {@code dom_status_etapa} "aguardando"; {@code etapa.status_id} é NOT NULL e o INSERT envia a coluna. */
    public static final int STATUS_INICIAL = 1;

    public Etapa {
        if (statusId == null) {
            statusId = STATUS_INICIAL;
        }
        if (faseId == null) {
            throw new RegraDeNegocioException("A fase da etapa é obrigatória.");
        }
        nome = Validacoes.nomeObrigatorio(nome, "da etapa", NOME_MAX);
        if (tipoId == null) {
            throw new RegraDeNegocioException("O tipo da etapa é obrigatório.");
        }
        Validacoes.periodoValido(dataAbertura, dataFechamento, "da etapa");
        if (id != null && id.equals(elegibilidadeEtapaId)) {
            throw new RegraDeNegocioException("A etapa não pode ser pré-requisito de si mesma.");
        }
        notaMinimaPercentual = normalizarPercentual(notaMinimaPercentual);
    }

    public Etapa comId(Integer novoId) {
        return new Etapa(novoId, faseId, nome, tipoId, descricao, dataAbertura, dataFechamento, statusId,
                reprovavel, obrigatoriaParaFase, todosCandidatos, aceitaDocumentos, aceitaRecurso, preliminar,
                elegibilidadeEtapaId, elegibilidadeStatusId, responsavelId, publicadaEm,
                notaZeroElimina, notaMinimaPercentual, modalidade);
    }

    /** Nota mínima em pontos = percentual × pontuação máxima / 100 (CA05). */
    public static BigDecimal notaMinimaEmPontos(BigDecimal percentual, BigDecimal pontuacaoMaxima) {
        if (percentual == null || pontuacaoMaxima == null) {
            return null;
        }
        return pontuacaoMaxima.multiply(percentual).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    private static BigDecimal normalizarPercentual(BigDecimal percentual) {
        if (percentual == null) {
            return null;
        }
        if (percentual.compareTo(BigDecimal.ZERO) <= 0 || percentual.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new RegraDeNegocioException("A nota mínima deve ser maior que 0 e no máximo 100.");
        }
        if (percentual.scale() > 2) {
            throw new RegraDeNegocioException("A nota mínima aceita no máximo duas casas decimais.");
        }
        return percentual.stripTrailingZeros().scale() < 0 ? percentual.setScale(0) : percentual;
    }
}

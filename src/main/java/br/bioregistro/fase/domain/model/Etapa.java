package br.bioregistro.fase.domain.model;

import br.bioregistro.fase.domain.exception.RegraDeNegocioException;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

/**
 * Etapa de uma fase. Espelha {@code dbo.etapa}; as datas são opcionais porque a
 * etapa pode existir antes de ter data definida.
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
        LocalDateTime publicadaEm
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
    }

    public Etapa comId(Integer novoId) {
        return new Etapa(novoId, faseId, nome, tipoId, descricao, dataAbertura, dataFechamento, statusId,
                reprovavel, obrigatoriaParaFase, todosCandidatos, aceitaDocumentos, aceitaRecurso, preliminar,
                elegibilidadeEtapaId, elegibilidadeStatusId, responsavelId, publicadaEm);
    }
}

package br.bioregistro.fase.domain.model;

import br.bioregistro.fase.domain.exception.RegraDeNegocioException;

import java.time.OffsetDateTime;

/**
 * Fase do edital. {@code ordem} é a posição da fase no edital e é única por edital
 * (UK {@code UQ_fase_edital_ordem} em {@code dbo.fases}).
 */
public record Fase(
        Integer id,
        Integer editalId,
        String nome,
        String descricao,
        Short ordem,
        OffsetDateTime dataAbertura,
        OffsetDateTime dataFechamento
) {

    public static final int NOME_MAX = 150;

    public Fase {
        if (editalId == null) {
            throw new RegraDeNegocioException("O edital da fase é obrigatório.");
        }
        nome = Validacoes.nomeObrigatorio(nome, "da fase", NOME_MAX);
        if (ordem == null || ordem < 1) {
            throw new RegraDeNegocioException("A ordem da fase deve ser maior ou igual a 1.");
        }
        if (dataAbertura == null || dataFechamento == null) {
            throw new RegraDeNegocioException("As datas de abertura e fechamento da fase são obrigatórias.");
        }
        Validacoes.periodoValido(dataAbertura, dataFechamento, "da fase");
    }

    public Fase comId(Integer novoId) {
        return new Fase(novoId, editalId, nome, descricao, ordem, dataAbertura, dataFechamento);
    }
}

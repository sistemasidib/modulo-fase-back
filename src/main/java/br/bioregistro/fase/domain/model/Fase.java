package br.bioregistro.fase.domain.model;

import br.bioregistro.fase.domain.exception.RegraDeNegocioException;

import java.time.OffsetDateTime;

/**
 * Fase do edital. {@code ordem} é a posição da fase no edital e é única por edital
 * (UK {@code UQ_fase_edital_ordem} em {@code dbo.fases}). As datas vêm do cronograma e podem faltar
 * (fase de edital sem cronograma ou do legado). {@code clausulaBarreira} (RN09) é opcional por fase.
 */
public record Fase(
        Integer id,
        Integer editalId,
        String nome,
        String descricao,
        Short ordem,
        boolean clausulaBarreira,
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
        Validacoes.periodoValido(dataAbertura, dataFechamento, "da fase");
    }

    public Fase comId(Integer novoId) {
        return new Fase(novoId, editalId, nome, descricao, ordem, clausulaBarreira, dataAbertura, dataFechamento);
    }
}

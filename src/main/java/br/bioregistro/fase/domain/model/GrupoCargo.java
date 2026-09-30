package br.bioregistro.fase.domain.model;

import br.bioregistro.fase.domain.exception.RegraDeNegocioException;

/** Grupo de cargos do edital que compartilha a mesma estrutura de fases e etapas. */
public record GrupoCargo(Integer id, Integer editalId, String nome) {

    public static final int NOME_MAX = 150;

    public GrupoCargo {
        if (editalId == null) {
            throw new RegraDeNegocioException("O edital do grupo é obrigatório.");
        }
        nome = Validacoes.nomeObrigatorio(nome, "do grupo", NOME_MAX);
    }

    public GrupoCargo comId(Integer novoId) {
        return new GrupoCargo(novoId, editalId, nome);
    }
}

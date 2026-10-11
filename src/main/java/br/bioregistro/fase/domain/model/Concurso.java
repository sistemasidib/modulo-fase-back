package br.bioregistro.fase.domain.model;

/** Agrupa os editais de um mesmo certame (RN01 da spec 003). */
public record Concurso(Integer id, String nome) {

    public static final int NOME_MAX = 200;

    public Concurso {
        nome = Validacoes.nomeObrigatorio(nome, "do concurso", NOME_MAX);
    }

    public Concurso comId(Integer novoId) {
        return new Concurso(novoId, nome);
    }
}

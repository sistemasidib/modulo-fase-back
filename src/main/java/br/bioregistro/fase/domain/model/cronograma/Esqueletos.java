package br.bioregistro.fase.domain.model.cronograma;

import br.bioregistro.fase.domain.exception.NaoEncontradoException;

/** Esqueletos conhecidos. O cronograma guarda o nome do que o gerou (RB04). */
public final class Esqueletos {

    public static final Esqueleto ATUAL = EsqueletoIdecanV1.ESQUELETO;

    private Esqueletos() {
    }

    public static Esqueleto porNome(String nome) {
        if (ATUAL.nome().equals(nome)) {
            return ATUAL;
        }
        throw new NaoEncontradoException("Esqueleto", nome);
    }
}

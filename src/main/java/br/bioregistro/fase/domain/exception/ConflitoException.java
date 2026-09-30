package br.bioregistro.fase.domain.exception;

/** A operação conflita com o estado atual (duplicidade, registro em uso). */
public class ConflitoException extends RuntimeException {

    public ConflitoException(String mensagem) {
        super(mensagem);
    }
}

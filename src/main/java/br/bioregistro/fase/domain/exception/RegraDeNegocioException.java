package br.bioregistro.fase.domain.exception;

/** Dado recebido viola uma invariante do domínio. */
public class RegraDeNegocioException extends RuntimeException {

    public RegraDeNegocioException(String mensagem) {
        super(mensagem);
    }
}

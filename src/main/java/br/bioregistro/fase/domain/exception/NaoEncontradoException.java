package br.bioregistro.fase.domain.exception;

public class NaoEncontradoException extends RuntimeException {

    public NaoEncontradoException(String recurso, Object id) {
        super(recurso + " " + id + " não encontrado(a).");
    }
}

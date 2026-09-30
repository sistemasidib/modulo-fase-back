package br.bioregistro.fase.infrastructure.persistence;

import br.bioregistro.fase.domain.exception.ConflitoException;
import jakarta.persistence.PersistenceException;

/**
 * Força o flush para que violações de FK/UK aconteçam aqui (e não no commit) e as
 * traduz em {@link ConflitoException}.
 */
public final class RestricaoDoBanco {

    private RestricaoDoBanco() {
    }

    public static void flush(Runnable flush, String mensagemConflito) {
        try {
            flush.run();
        } catch (PersistenceException e) {
            if (e instanceof org.hibernate.exception.ConstraintViolationException
                    || e.getCause() instanceof org.hibernate.exception.ConstraintViolationException) {
                throw new ConflitoException(mensagemConflito);
            }
            throw e;
        }
    }
}

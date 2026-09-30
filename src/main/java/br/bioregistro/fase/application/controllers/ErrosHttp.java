package br.bioregistro.fase.application.controllers;

import br.bioregistro.fase.application.dtos.ErroResponse;
import br.bioregistro.fase.domain.exception.ConflitoException;
import br.bioregistro.fase.domain.exception.NaoEncontradoException;
import br.bioregistro.fase.domain.exception.RegraDeNegocioException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;

/** Converte as exceções do domínio em respostas HTTP com {@code {"message": ...}}. */
public class ErrosHttp {

    @ServerExceptionMapper
    public Response regraDeNegocio(RegraDeNegocioException e) {
        return resposta(Response.Status.BAD_REQUEST, e);
    }

    @ServerExceptionMapper
    public Response naoEncontrado(NaoEncontradoException e) {
        return resposta(Response.Status.NOT_FOUND, e);
    }

    @ServerExceptionMapper
    public Response conflito(ConflitoException e) {
        return resposta(Response.Status.CONFLICT, e);
    }

    private static Response resposta(Response.Status status, RuntimeException e) {
        return Response.status(status)
                .type(MediaType.APPLICATION_JSON_TYPE)
                .entity(new ErroResponse(e.getMessage()))
                .build();
    }
}

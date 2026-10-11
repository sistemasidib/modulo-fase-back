package br.bioregistro.fase.application.controllers;

import br.bioregistro.fase.application.dtos.DisciplinasDtos.DisciplinaResponse;
import br.bioregistro.fase.application.dtos.DisciplinasDtos.DisciplinasDaEtapaResponse;
import br.bioregistro.fase.application.dtos.DisciplinasDtos.NovaDisciplinaRequest;
import br.bioregistro.fase.application.dtos.DisciplinasDtos.PesoRequest;
import br.bioregistro.fase.application.use_cases.DisciplinasDaProva;
import br.bioregistro.fase.domain.model.Instituicao;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/** Disciplinas de cada prova (etapa): faixas de questões por cargo e o valor de cada questão. */
@Path("/v1/{instituicao}")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class DisciplinaController {

    private final DisciplinasDaProva disciplinas;

    public DisciplinaController(DisciplinasDaProva disciplinas) {
        this.disciplinas = disciplinas;
    }

    @GET
    @Path("/etapas/{etapaId}/disciplinas")
    public DisciplinasDaEtapaResponse daEtapa(@PathParam("instituicao") Instituicao instituicao,
                                              @PathParam("etapaId") int etapaId) {
        return disciplinas.daEtapa(instituicao, etapaId);
    }

    @POST
    @Path("/etapas/{etapaId}/disciplinas")
    public Response criar(@PathParam("instituicao") Instituicao instituicao, @PathParam("etapaId") int etapaId,
                          NovaDisciplinaRequest request) {
        DisciplinasDaEtapaResponse lista = disciplinas.criar(instituicao, etapaId, Parametros.corpo(request));
        return Response.status(Response.Status.CREATED).entity(lista).build();
    }

    @DELETE
    @Path("/disciplinas/{faixaId}")
    public Response remover(@PathParam("instituicao") Instituicao instituicao, @PathParam("faixaId") int faixaId) {
        disciplinas.remover(instituicao, faixaId);
        return Response.noContent().build();
    }

    @PUT
    @Path("/disciplinas/{faixaId}/peso")
    public DisciplinaResponse definirPeso(@PathParam("instituicao") Instituicao instituicao,
                                          @PathParam("faixaId") int faixaId, PesoRequest request) {
        return disciplinas.definirPeso(instituicao, faixaId, request == null ? null : request.peso());
    }
}

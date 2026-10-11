package br.bioregistro.fase.application.controllers;

import br.bioregistro.fase.application.dtos.CriterioDesempateDtos.CopiarRequest;
import br.bioregistro.fase.application.dtos.CriterioDesempateDtos.CriterioDesempateRequest;
import br.bioregistro.fase.application.dtos.CriterioDesempateDtos.CriterioDesempateResponse;
import br.bioregistro.fase.application.dtos.CriterioDesempateDtos.OrdemRequest;
import br.bioregistro.fase.application.use_cases.CriteriosDeDesempate;
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

import java.util.List;

/** Critérios de desempate de uma fase (de um grupo). */
@Path("/v1/{instituicao}")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CriterioDesempateController {

    private final CriteriosDeDesempate criterios;

    public CriterioDesempateController(CriteriosDeDesempate criterios) {
        this.criterios = criterios;
    }

    @GET
    @Path("/fases/{faseId}/criterios-desempate")
    public List<CriterioDesempateResponse> listar(@PathParam("instituicao") Instituicao instituicao,
                                                  @PathParam("faseId") int faseId) {
        return criterios.listar(instituicao, faseId);
    }

    @POST
    @Path("/fases/{faseId}/criterios-desempate")
    public Response criar(@PathParam("instituicao") Instituicao instituicao, @PathParam("faseId") int faseId,
                          CriterioDesempateRequest request) {
        CriterioDesempateResponse criado = criterios.criar(instituicao, faseId, Parametros.corpo(request));
        return Response.status(Response.Status.CREATED).entity(criado).build();
    }

    @PUT
    @Path("/fases/{faseId}/criterios-desempate/ordem")
    public List<CriterioDesempateResponse> reordenar(@PathParam("instituicao") Instituicao instituicao,
                                                     @PathParam("faseId") int faseId, OrdemRequest request) {
        return criterios.reordenar(instituicao, faseId, Parametros.corpo(request).ids());
    }

    @POST
    @Path("/fases/{faseId}/criterios-desempate/copiar")
    public Response copiar(@PathParam("instituicao") Instituicao instituicao, @PathParam("faseId") int faseId,
                           CopiarRequest request) {
        criterios.copiar(instituicao, faseId, Parametros.corpo(request).faseIds());
        return Response.noContent().build();
    }

    @PUT
    @Path("/criterios-desempate/{id}")
    public CriterioDesempateResponse atualizar(@PathParam("instituicao") Instituicao instituicao,
                                               @PathParam("id") int id, CriterioDesempateRequest request) {
        return criterios.atualizar(instituicao, id, Parametros.corpo(request));
    }

    @DELETE
    @Path("/criterios-desempate/{id}")
    public Response excluir(@PathParam("instituicao") Instituicao instituicao, @PathParam("id") int id) {
        criterios.excluir(instituicao, id);
        return Response.noContent().build();
    }
}

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

/** Critérios de desempate de uma etapa. */
@Path("/v1/{instituicao}")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CriterioDesempateController {

    private final CriteriosDeDesempate criterios;

    public CriterioDesempateController(CriteriosDeDesempate criterios) {
        this.criterios = criterios;
    }

    @GET
    @Path("/etapas/{etapaId}/criterios-desempate")
    public List<CriterioDesempateResponse> listar(@PathParam("instituicao") Instituicao instituicao,
                                                  @PathParam("etapaId") int etapaId) {
        return criterios.listar(instituicao, etapaId);
    }

    @POST
    @Path("/etapas/{etapaId}/criterios-desempate")
    public Response criar(@PathParam("instituicao") Instituicao instituicao, @PathParam("etapaId") int etapaId,
                          CriterioDesempateRequest request) {
        CriterioDesempateResponse criado = criterios.criar(instituicao, etapaId, Parametros.corpo(request));
        return Response.status(Response.Status.CREATED).entity(criado).build();
    }

    @PUT
    @Path("/etapas/{etapaId}/criterios-desempate/ordem")
    public List<CriterioDesempateResponse> reordenar(@PathParam("instituicao") Instituicao instituicao,
                                                     @PathParam("etapaId") int etapaId, OrdemRequest request) {
        return criterios.reordenar(instituicao, etapaId, Parametros.corpo(request).ids());
    }

    @POST
    @Path("/etapas/{etapaId}/criterios-desempate/copiar")
    public Response copiar(@PathParam("instituicao") Instituicao instituicao, @PathParam("etapaId") int etapaId,
                           CopiarRequest request) {
        criterios.copiar(instituicao, etapaId, Parametros.corpo(request).etapaIds());
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

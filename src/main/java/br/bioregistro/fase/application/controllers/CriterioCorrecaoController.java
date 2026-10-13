package br.bioregistro.fase.application.controllers;

import br.bioregistro.fase.application.dtos.CriterioCorrecaoDtos.CopiarRequest;
import br.bioregistro.fase.application.dtos.CriterioCorrecaoDtos.CriterioCorrecaoRequest;
import br.bioregistro.fase.application.dtos.CriterioCorrecaoDtos.CriterioCorrecaoResponse;
import br.bioregistro.fase.application.dtos.CriterioCorrecaoDtos.OrdemRequest;
import br.bioregistro.fase.application.use_cases.CriteriosDeCorrecao;
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

/** Critérios de correção de uma prova discursiva (etapa). */
@Path("/v1/{instituicao}")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CriterioCorrecaoController {

    private final CriteriosDeCorrecao criterios;

    public CriterioCorrecaoController(CriteriosDeCorrecao criterios) {
        this.criterios = criterios;
    }

    @GET
    @Path("/etapas/{etapaId}/criterios-correcao")
    public List<CriterioCorrecaoResponse> listar(@PathParam("instituicao") Instituicao instituicao,
                                                 @PathParam("etapaId") int etapaId) {
        return criterios.listar(instituicao, etapaId);
    }

    @POST
    @Path("/etapas/{etapaId}/criterios-correcao")
    public Response criar(@PathParam("instituicao") Instituicao instituicao, @PathParam("etapaId") int etapaId,
                          CriterioCorrecaoRequest request) {
        CriterioCorrecaoResponse criado = criterios.criar(instituicao, etapaId, Parametros.corpo(request));
        return Response.status(Response.Status.CREATED).entity(criado).build();
    }

    @PUT
    @Path("/etapas/{etapaId}/criterios-correcao/ordem")
    public List<CriterioCorrecaoResponse> reordenar(@PathParam("instituicao") Instituicao instituicao,
                                                    @PathParam("etapaId") int etapaId, OrdemRequest request) {
        return criterios.reordenar(instituicao, etapaId, Parametros.corpo(request).ids());
    }

    @POST
    @Path("/etapas/{etapaId}/criterios-correcao/copiar")
    public Response copiar(@PathParam("instituicao") Instituicao instituicao, @PathParam("etapaId") int etapaId,
                           CopiarRequest request) {
        criterios.copiar(instituicao, etapaId, Parametros.corpo(request).etapaIds());
        return Response.noContent().build();
    }

    @PUT
    @Path("/criterios-correcao/{id}")
    public CriterioCorrecaoResponse atualizar(@PathParam("instituicao") Instituicao instituicao,
                                              @PathParam("id") int id, CriterioCorrecaoRequest request) {
        return criterios.atualizar(instituicao, id, Parametros.corpo(request));
    }

    @DELETE
    @Path("/criterios-correcao/{id}")
    public Response excluir(@PathParam("instituicao") Instituicao instituicao, @PathParam("id") int id) {
        criterios.excluir(instituicao, id);
        return Response.noContent().build();
    }
}

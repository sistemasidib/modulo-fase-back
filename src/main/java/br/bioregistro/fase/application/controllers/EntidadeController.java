package br.bioregistro.fase.application.controllers;

import br.bioregistro.fase.application.dtos.EntidadeDtos.EditalSemEntidadeResponse;
import br.bioregistro.fase.application.dtos.EntidadeDtos.EntidadeRequest;
import br.bioregistro.fase.application.dtos.EntidadeDtos.EntidadeResponse;
import br.bioregistro.fase.application.use_cases.ConsultarEntidades;
import br.bioregistro.fase.application.use_cases.ManterEntidade;
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

/** Entidades: órgãos responsáveis pelos editais (ex.: uma prefeitura). O cronograma é de cada edital. */
@Path("/v1/{instituicao}/entidades")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class EntidadeController {

    private final ConsultarEntidades consultar;
    private final ManterEntidade manter;

    public EntidadeController(ConsultarEntidades consultar, ManterEntidade manter) {
        this.consultar = consultar;
        this.manter = manter;
    }

    @GET
    public List<EntidadeResponse> listar(@PathParam("instituicao") Instituicao instituicao) {
        return consultar.listar(instituicao);
    }

    @POST
    public Response criar(@PathParam("instituicao") Instituicao instituicao, EntidadeRequest request) {
        EntidadeResponse criada = manter.criar(instituicao, Parametros.corpo(request));
        return Response.status(Response.Status.CREATED).entity(criada).build();
    }

    @GET
    @Path("editais-sem-entidade")
    public List<EditalSemEntidadeResponse> editaisSemEntidade(@PathParam("instituicao") Instituicao instituicao) {
        return consultar.editaisSemEntidade(instituicao);
    }

    @GET
    @Path("{id}")
    public EntidadeResponse buscar(@PathParam("instituicao") Instituicao instituicao, @PathParam("id") int id) {
        return consultar.buscar(instituicao, id);
    }

    @PUT
    @Path("{id}")
    public EntidadeResponse editar(@PathParam("instituicao") Instituicao instituicao, @PathParam("id") int id,
                                   EntidadeRequest request) {
        return manter.editar(instituicao, id, Parametros.corpo(request));
    }

    @DELETE
    @Path("{id}")
    public Response excluir(@PathParam("instituicao") Instituicao instituicao, @PathParam("id") int id) {
        manter.excluir(instituicao, id);
        return Response.noContent().build();
    }

    @PUT
    @Path("{id}/editais/{editalId}")
    public EntidadeResponse incluirEdital(@PathParam("instituicao") Instituicao instituicao, @PathParam("id") int id,
                                          @PathParam("editalId") int editalId) {
        return manter.incluirEdital(instituicao, id, editalId);
    }

    @DELETE
    @Path("{id}/editais/{editalId}")
    public EntidadeResponse retirarEdital(@PathParam("instituicao") Instituicao instituicao, @PathParam("id") int id,
                                          @PathParam("editalId") int editalId) {
        return manter.retirarEdital(instituicao, id, editalId);
    }
}

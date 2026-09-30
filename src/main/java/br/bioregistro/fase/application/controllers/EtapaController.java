package br.bioregistro.fase.application.controllers;

import br.bioregistro.fase.application.dtos.EtapaRequest;
import br.bioregistro.fase.application.dtos.EtapaResponse;
import br.bioregistro.fase.application.use_cases.AtualizarEtapa;
import br.bioregistro.fase.application.use_cases.BuscarEtapa;
import br.bioregistro.fase.application.use_cases.CriarEtapa;
import br.bioregistro.fase.application.use_cases.ExcluirEtapa;
import br.bioregistro.fase.application.use_cases.ListarEtapas;
import br.bioregistro.fase.domain.model.Instituicao;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;

/** CRUD de etapas. {@code instituicao}: {@code idecan} ou {@code idib}. */
@Path("/v1/{instituicao}/etapas")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class EtapaController {

    private final CriarEtapa criarEtapa;
    private final BuscarEtapa buscarEtapa;
    private final ListarEtapas listarEtapas;
    private final AtualizarEtapa atualizarEtapa;
    private final ExcluirEtapa excluirEtapa;

    public EtapaController(CriarEtapa criarEtapa, BuscarEtapa buscarEtapa, ListarEtapas listarEtapas,
                           AtualizarEtapa atualizarEtapa, ExcluirEtapa excluirEtapa) {
        this.criarEtapa = criarEtapa;
        this.buscarEtapa = buscarEtapa;
        this.listarEtapas = listarEtapas;
        this.atualizarEtapa = atualizarEtapa;
        this.excluirEtapa = excluirEtapa;
    }

    @GET
    public List<EtapaResponse> listar(@PathParam("instituicao") Instituicao instituicao,
                                      @QueryParam("faseId") Integer faseId) {
        return listarEtapas.executar(instituicao, Parametros.obrigatorio(faseId, "faseId"));
    }

    @POST
    public Response criar(@PathParam("instituicao") Instituicao instituicao,
                          @QueryParam("faseId") Integer faseId,
                          EtapaRequest request) {
        EtapaResponse criada = criarEtapa.executar(
                instituicao, Parametros.obrigatorio(faseId, "faseId"), Parametros.corpo(request));
        return Response.status(Response.Status.CREATED).entity(criada).build();
    }

    @GET
    @Path("{id}")
    public EtapaResponse buscar(@PathParam("instituicao") Instituicao instituicao, @PathParam("id") int id) {
        return buscarEtapa.executar(instituicao, id);
    }

    @PUT
    @Path("{id}")
    public EtapaResponse atualizar(@PathParam("instituicao") Instituicao instituicao, @PathParam("id") int id,
                                   EtapaRequest request) {
        return atualizarEtapa.executar(instituicao, id, Parametros.corpo(request));
    }

    @DELETE
    @Path("{id}")
    public Response excluir(@PathParam("instituicao") Instituicao instituicao, @PathParam("id") int id) {
        excluirEtapa.executar(instituicao, id);
        return Response.noContent().build();
    }
}

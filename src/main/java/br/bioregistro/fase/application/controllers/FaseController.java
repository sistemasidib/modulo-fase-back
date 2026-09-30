package br.bioregistro.fase.application.controllers;

import br.bioregistro.fase.application.dtos.FaseRequest;
import br.bioregistro.fase.application.dtos.FaseResponse;
import br.bioregistro.fase.application.use_cases.AtualizarFase;
import br.bioregistro.fase.application.use_cases.BuscarFase;
import br.bioregistro.fase.application.use_cases.CriarFase;
import br.bioregistro.fase.application.use_cases.ExcluirFase;
import br.bioregistro.fase.application.use_cases.ListarFases;
import br.bioregistro.fase.application.use_cases.ListarFasesDoGrupo;
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

/** CRUD de fases. {@code instituicao}: {@code idecan} ou {@code idib}. */
@Path("/v1/{instituicao}/fases")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class FaseController {

    private final CriarFase criarFase;
    private final BuscarFase buscarFase;
    private final ListarFases listarFases;
    private final ListarFasesDoGrupo listarFasesDoGrupo;
    private final AtualizarFase atualizarFase;
    private final ExcluirFase excluirFase;

    public FaseController(CriarFase criarFase, BuscarFase buscarFase, ListarFases listarFases,
                          ListarFasesDoGrupo listarFasesDoGrupo, AtualizarFase atualizarFase,
                          ExcluirFase excluirFase) {
        this.criarFase = criarFase;
        this.buscarFase = buscarFase;
        this.listarFases = listarFases;
        this.listarFasesDoGrupo = listarFasesDoGrupo;
        this.atualizarFase = atualizarFase;
        this.excluirFase = excluirFase;
    }

    /** Por edital ({@code editalId}) ou só as fases de um grupo de cargos ({@code grupoCargoId}). */
    @GET
    public List<FaseResponse> listar(@PathParam("instituicao") Instituicao instituicao,
                                     @QueryParam("editalId") Integer editalId,
                                     @QueryParam("grupoCargoId") Integer grupoCargoId) {
        if (grupoCargoId != null) {
            return listarFasesDoGrupo.executar(instituicao, grupoCargoId);
        }
        return listarFases.executar(instituicao, Parametros.obrigatorio(editalId, "editalId ou grupoCargoId"));
    }

    @POST
    public Response criar(@PathParam("instituicao") Instituicao instituicao,
                          @QueryParam("editalId") Integer editalId,
                          FaseRequest request) {
        FaseResponse criada = criarFase.executar(
                instituicao, Parametros.obrigatorio(editalId, "editalId"), Parametros.corpo(request));
        return Response.status(Response.Status.CREATED).entity(criada).build();
    }

    @GET
    @Path("{id}")
    public FaseResponse buscar(@PathParam("instituicao") Instituicao instituicao, @PathParam("id") int id) {
        return buscarFase.executar(instituicao, id);
    }

    @PUT
    @Path("{id}")
    public FaseResponse atualizar(@PathParam("instituicao") Instituicao instituicao, @PathParam("id") int id,
                                  FaseRequest request) {
        return atualizarFase.executar(instituicao, id, Parametros.corpo(request));
    }

    @DELETE
    @Path("{id}")
    public Response excluir(@PathParam("instituicao") Instituicao instituicao, @PathParam("id") int id) {
        excluirFase.executar(instituicao, id);
        return Response.noContent().build();
    }
}

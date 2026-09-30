package br.bioregistro.fase.application.controllers;

import br.bioregistro.fase.application.dtos.CargoResponse;
import br.bioregistro.fase.application.dtos.FaseGeradaResponse;
import br.bioregistro.fase.application.dtos.GerarFasesRequest;
import br.bioregistro.fase.application.dtos.GrupoCargoRequest;
import br.bioregistro.fase.application.dtos.GrupoCargoResponse;
import br.bioregistro.fase.application.use_cases.AdicionarCargoAoGrupo;
import br.bioregistro.fase.application.use_cases.CriarGrupoCargo;
import br.bioregistro.fase.application.use_cases.ExcluirGrupoCargo;
import br.bioregistro.fase.application.use_cases.GerarFasesDoGrupo;
import br.bioregistro.fase.application.use_cases.ListarCargosSemGrupo;
import br.bioregistro.fase.application.use_cases.ListarGruposCargo;
import br.bioregistro.fase.application.use_cases.RemoverCargoDoGrupo;
import br.bioregistro.fase.application.use_cases.RenomearGrupoCargo;
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

/** Grupos de cargos do edital e geração das fases de cada grupo. */
@Path("/v1/{instituicao}/grupos-cargo")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class GrupoCargoController {

    private final CriarGrupoCargo criarGrupo;
    private final ListarGruposCargo listarGrupos;
    private final RenomearGrupoCargo renomearGrupo;
    private final ExcluirGrupoCargo excluirGrupo;
    private final AdicionarCargoAoGrupo adicionarCargo;
    private final RemoverCargoDoGrupo removerCargo;
    private final ListarCargosSemGrupo listarCargosSemGrupo;
    private final GerarFasesDoGrupo gerarFases;

    public GrupoCargoController(CriarGrupoCargo criarGrupo, ListarGruposCargo listarGrupos,
                                RenomearGrupoCargo renomearGrupo, ExcluirGrupoCargo excluirGrupo,
                                AdicionarCargoAoGrupo adicionarCargo, RemoverCargoDoGrupo removerCargo,
                                ListarCargosSemGrupo listarCargosSemGrupo, GerarFasesDoGrupo gerarFases) {
        this.criarGrupo = criarGrupo;
        this.listarGrupos = listarGrupos;
        this.renomearGrupo = renomearGrupo;
        this.excluirGrupo = excluirGrupo;
        this.adicionarCargo = adicionarCargo;
        this.removerCargo = removerCargo;
        this.listarCargosSemGrupo = listarCargosSemGrupo;
        this.gerarFases = gerarFases;
    }

    @GET
    public List<GrupoCargoResponse> listar(@PathParam("instituicao") Instituicao instituicao,
                                           @QueryParam("editalId") Integer editalId) {
        return listarGrupos.executar(instituicao, Parametros.obrigatorio(editalId, "editalId"));
    }

    @POST
    public Response criar(@PathParam("instituicao") Instituicao instituicao,
                          @QueryParam("editalId") Integer editalId,
                          GrupoCargoRequest request) {
        GrupoCargoResponse criado = criarGrupo.executar(
                instituicao, Parametros.obrigatorio(editalId, "editalId"), Parametros.corpo(request));
        return Response.status(Response.Status.CREATED).entity(criado).build();
    }

    @PUT
    @Path("{id}")
    public GrupoCargoResponse renomear(@PathParam("instituicao") Instituicao instituicao, @PathParam("id") int id,
                                       GrupoCargoRequest request) {
        return renomearGrupo.executar(instituicao, id, Parametros.corpo(request));
    }

    @DELETE
    @Path("{id}")
    public Response excluir(@PathParam("instituicao") Instituicao instituicao, @PathParam("id") int id) {
        excluirGrupo.executar(instituicao, id);
        return Response.noContent().build();
    }

    @GET
    @Path("cargos-sem-grupo")
    public List<CargoResponse> cargosSemGrupo(@PathParam("instituicao") Instituicao instituicao,
                                              @QueryParam("editalId") Integer editalId) {
        return listarCargosSemGrupo.executar(instituicao, Parametros.obrigatorio(editalId, "editalId"));
    }

    @PUT
    @Path("{id}/cargos/{cargoId}")
    public GrupoCargoResponse adicionarCargo(@PathParam("instituicao") Instituicao instituicao,
                                             @PathParam("id") int id, @PathParam("cargoId") int cargoId) {
        return adicionarCargo.executar(instituicao, id, cargoId);
    }

    @DELETE
    @Path("{id}/cargos/{cargoId}")
    public GrupoCargoResponse removerCargo(@PathParam("instituicao") Instituicao instituicao,
                                           @PathParam("id") int id, @PathParam("cargoId") int cargoId) {
        return removerCargo.executar(instituicao, id, cargoId);
    }

    @POST
    @Path("{id}/gerar-fases")
    public Response gerarFases(@PathParam("instituicao") Instituicao instituicao, @PathParam("id") int id,
                               GerarFasesRequest request) {
        List<FaseGeradaResponse> geradas = gerarFases.executar(instituicao, id, Parametros.corpo(request));
        return Response.status(Response.Status.CREATED).entity(geradas).build();
    }
}

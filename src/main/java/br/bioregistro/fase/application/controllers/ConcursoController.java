package br.bioregistro.fase.application.controllers;

import br.bioregistro.fase.application.dtos.ConcursoDtos.ConcursoRequest;
import br.bioregistro.fase.application.dtos.ConcursoDtos.ConcursoResponse;
import br.bioregistro.fase.application.dtos.ConcursoDtos.EditalSemConcursoResponse;
import br.bioregistro.fase.application.dtos.CronogramaDtos.AbrirCronogramaResponse;
import br.bioregistro.fase.application.use_cases.ConsultarConcursos;
import br.bioregistro.fase.application.use_cases.EditarCronograma;
import br.bioregistro.fase.application.use_cases.ManterConcurso;
import br.bioregistro.fase.domain.model.Instituicao;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;

/** Concursos: agrupam editais e têm o cronograma (spec 003, RB01, RB02, RB12). */
@Path("/v1/{instituicao}/concursos")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ConcursoController {

    private final ConsultarConcursos consultar;
    private final ManterConcurso manter;
    private final EditarCronograma cronograma;

    public ConcursoController(ConsultarConcursos consultar, ManterConcurso manter, EditarCronograma cronograma) {
        this.consultar = consultar;
        this.manter = manter;
        this.cronograma = cronograma;
    }

    @GET
    public List<ConcursoResponse> listar(@PathParam("instituicao") Instituicao instituicao) {
        return consultar.listar(instituicao);
    }

    @POST
    public Response criar(@PathParam("instituicao") Instituicao instituicao, ConcursoRequest request) {
        ConcursoResponse criado = manter.criar(instituicao, Parametros.corpo(request));
        return Response.status(Response.Status.CREATED).entity(criado).build();
    }

    @GET
    @Path("editais-sem-concurso")
    public List<EditalSemConcursoResponse> editaisSemConcurso(@PathParam("instituicao") Instituicao instituicao) {
        return consultar.editaisSemConcurso(instituicao);
    }

    @GET
    @Path("{id}")
    public ConcursoResponse buscar(@PathParam("instituicao") Instituicao instituicao, @PathParam("id") int id) {
        return consultar.buscar(instituicao, id);
    }

    @PUT
    @Path("{id}")
    public ConcursoResponse renomear(@PathParam("instituicao") Instituicao instituicao, @PathParam("id") int id,
                                     ConcursoRequest request) {
        return manter.renomear(instituicao, id, Parametros.corpo(request));
    }

    @DELETE
    @Path("{id}")
    public Response excluir(@PathParam("instituicao") Instituicao instituicao, @PathParam("id") int id) {
        manter.excluir(instituicao, id);
        return Response.noContent().build();
    }

    @PUT
    @Path("{id}/editais/{editalId}")
    public ConcursoResponse incluirEdital(@PathParam("instituicao") Instituicao instituicao, @PathParam("id") int id,
                                          @PathParam("editalId") int editalId) {
        return manter.incluirEdital(instituicao, id, editalId);
    }

    @DELETE
    @Path("{id}/editais/{editalId}")
    public ConcursoResponse retirarEdital(@PathParam("instituicao") Instituicao instituicao, @PathParam("id") int id,
                                          @PathParam("editalId") int editalId) {
        return manter.retirarEdital(instituicao, id, editalId);
    }

    /** 201 quando cria; 200 quando o concurso já tinha cronograma. Sem corpo: aceita qualquer Content-Type. */
    @POST
    @Path("{id}/cronograma")
    @Consumes(MediaType.WILDCARD)
    public Response abrirCronograma(@PathParam("instituicao") Instituicao instituicao, @PathParam("id") int id,
                                    @HeaderParam("X-Usuario") String usuario) {
        AbrirCronogramaResponse aberto = cronograma.abrir(instituicao, id, Parametros.autor(usuario));
        return Response.status(aberto.criado() ? Response.Status.CREATED : Response.Status.OK).entity(aberto).build();
    }
}

package br.bioregistro.fase.application.controllers;

import br.bioregistro.fase.application.dtos.CronogramaDtos.AbrirCronogramaResponse;
import br.bioregistro.fase.application.dtos.CronogramaDtos.SeguirCronogramaRequest;
import br.bioregistro.fase.application.use_cases.EditarCronograma;
import br.bioregistro.fase.application.use_cases.PonteCronograma;
import br.bioregistro.fase.domain.model.Instituicao;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * Qual cronograma o edital segue (spec 005): seguir o de outro edital (replicar), separar-se com
 * um cronograma próprio, e sincronizar a estrutura do edital com o cronograma.
 */
@Path("/v1/{instituicao}/editais/{editalId}/cronograma")
@Produces(MediaType.APPLICATION_JSON)
public class CronogramaDoEditalController {

    private final EditarCronograma editar;
    private final PonteCronograma ponte;

    public CronogramaDoEditalController(EditarCronograma editar, PonteCronograma ponte) {
        this.editar = editar;
        this.ponte = ponte;
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    public AbrirCronogramaResponse seguir(@PathParam("instituicao") Instituicao instituicao,
                                          @PathParam("editalId") int editalId,
                                          @HeaderParam("X-Usuario") String usuario,
                                          SeguirCronogramaRequest request) {
        return editar.seguir(instituicao, editalId, Parametros.corpo(request).cronogramaId(),
                Parametros.autor(usuario));
    }

    /** 201: o edital ganha cronograma próprio. Sem corpo: aceita qualquer Content-Type. */
    @POST
    @Path("separar")
    @Consumes(MediaType.WILDCARD)
    public Response separar(@PathParam("instituicao") Instituicao instituicao, @PathParam("editalId") int editalId,
                            @HeaderParam("X-Usuario") String usuario) {
        return Response.status(Response.Status.CREATED)
                .entity(editar.separar(instituicao, editalId, Parametros.autor(usuario))).build();
    }

    /** Refaz as linhas de leitura, as fases de execução e as publicações do edital (idempotente). */
    @POST
    @Path("sincronizar")
    @Consumes(MediaType.WILDCARD)
    public Response sincronizar(@PathParam("instituicao") Instituicao instituicao,
                                @PathParam("editalId") int editalId) {
        ponte.sincronizarEdital(instituicao, editalId);
        return Response.noContent().build();
    }
}

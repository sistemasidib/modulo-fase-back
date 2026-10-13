package br.bioregistro.fase.application.controllers;

import br.bioregistro.fase.application.use_cases.GruposDaFase;
import br.bioregistro.fase.domain.model.Instituicao;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.util.List;

/** Grupos de cargos que participam da fase do edital (spec 005). Respostas: ids dos grupos da fase. */
@Path("/v1/{instituicao}/fases/{faseId}/grupos")
@Produces(MediaType.APPLICATION_JSON)
public class GruposDaFaseController {

    private final GruposDaFase gruposDaFase;

    public GruposDaFaseController(GruposDaFase gruposDaFase) {
        this.gruposDaFase = gruposDaFase;
    }

    @GET
    public List<Integer> listar(@PathParam("instituicao") Instituicao instituicao, @PathParam("faseId") int faseId) {
        return gruposDaFase.listar(instituicao, faseId);
    }

    /** Sem corpo: aceita qualquer Content-Type. Incluir de novo não muda nada. */
    @PUT
    @Path("{grupoId}")
    @Consumes(MediaType.WILDCARD)
    public List<Integer> incluir(@PathParam("instituicao") Instituicao instituicao, @PathParam("faseId") int faseId,
                                 @PathParam("grupoId") int grupoId) {
        return gruposDaFase.incluir(instituicao, faseId, grupoId);
    }

    @DELETE
    @Path("{grupoId}")
    public List<Integer> retirar(@PathParam("instituicao") Instituicao instituicao, @PathParam("faseId") int faseId,
                                 @PathParam("grupoId") int grupoId) {
        return gruposDaFase.retirar(instituicao, faseId, grupoId);
    }
}

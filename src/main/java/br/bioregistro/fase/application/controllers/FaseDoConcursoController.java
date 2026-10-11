package br.bioregistro.fase.application.controllers;

import br.bioregistro.fase.application.dtos.FaseDoConcursoResponse;
import br.bioregistro.fase.application.use_cases.FasesDoConcurso;
import br.bioregistro.fase.domain.model.Instituicao;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.util.List;

/** Fases do concurso do edital, derivadas das fases dos grupos de cargos (só leitura). */
@Path("/v1/{instituicao}/editais/{editalId}/fases-do-concurso")
@Produces(MediaType.APPLICATION_JSON)
public class FaseDoConcursoController {

    private final FasesDoConcurso fasesDoConcurso;

    public FaseDoConcursoController(FasesDoConcurso fasesDoConcurso) {
        this.fasesDoConcurso = fasesDoConcurso;
    }

    @GET
    public List<FaseDoConcursoResponse> listar(@PathParam("instituicao") Instituicao instituicao,
                                               @PathParam("editalId") int editalId) {
        return fasesDoConcurso.listar(instituicao, editalId);
    }
}

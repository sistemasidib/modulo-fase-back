package br.bioregistro.fase.application.controllers;

import br.bioregistro.fase.application.dtos.FaseDoConcursoResponse;
import br.bioregistro.fase.application.dtos.PublicacoesDaFaseRequest;
import br.bioregistro.fase.application.use_cases.FasesDoConcurso;
import br.bioregistro.fase.application.use_cases.PublicacoesDaFaseDoConcurso;
import br.bioregistro.fase.domain.model.Instituicao;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.util.List;

/**
 * Fases do concurso do edital, derivadas das fases dos grupos de cargos, e o que é publicado em
 * cada uma (tipos de publicação das etapas dela em todos os grupos).
 */
@Path("/v1/{instituicao}/editais/{editalId}/fases-do-concurso")
@Produces(MediaType.APPLICATION_JSON)
public class FaseDoConcursoController {

    private final FasesDoConcurso fasesDoConcurso;
    private final PublicacoesDaFaseDoConcurso publicacoes;

    public FaseDoConcursoController(FasesDoConcurso fasesDoConcurso, PublicacoesDaFaseDoConcurso publicacoes) {
        this.fasesDoConcurso = fasesDoConcurso;
        this.publicacoes = publicacoes;
    }

    @GET
    public List<FaseDoConcursoResponse> listar(@PathParam("instituicao") Instituicao instituicao,
                                               @PathParam("editalId") int editalId) {
        return fasesDoConcurso.listar(instituicao, editalId);
    }

    @GET
    @Path("{tipoEtapaId}/publicacoes")
    public List<Integer> publicacoes(@PathParam("instituicao") Instituicao instituicao,
                                     @PathParam("editalId") int editalId,
                                     @PathParam("tipoEtapaId") int tipoEtapaId) {
        return publicacoes.listar(instituicao, editalId, tipoEtapaId);
    }

    @PUT
    @Path("{tipoEtapaId}/publicacoes")
    @Consumes(MediaType.APPLICATION_JSON)
    public List<Integer> definirPublicacoes(@PathParam("instituicao") Instituicao instituicao,
                                           @PathParam("editalId") int editalId,
                                           @PathParam("tipoEtapaId") int tipoEtapaId,
                                           PublicacoesDaFaseRequest request) {
        return publicacoes.definir(instituicao, editalId, tipoEtapaId, Parametros.corpo(request).tipoPublicacaoIds());
    }
}

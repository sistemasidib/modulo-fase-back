package br.bioregistro.fase.application.controllers;

import br.bioregistro.fase.application.dtos.EstruturaPorGrupoDtos.GrupoEstruturaResponse;
import br.bioregistro.fase.application.use_cases.EstruturaPorGrupo;
import br.bioregistro.fase.domain.model.Instituicao;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.util.List;

/** Leitura da estrutura por grupo (spec 006, RN01). */
@Path("/v1/{instituicao}/editais/{editalId}/estrutura-por-grupo")
@Produces(MediaType.APPLICATION_JSON)
public class EstruturaController {

    private final EstruturaPorGrupo estrutura;

    public EstruturaController(EstruturaPorGrupo estrutura) {
        this.estrutura = estrutura;
    }

    @GET
    public List<GrupoEstruturaResponse> listar(@PathParam("instituicao") Instituicao instituicao,
                                               @PathParam("editalId") int editalId) {
        return estrutura.executar(instituicao, editalId);
    }
}

package br.bioregistro.fase.application.controllers;

import br.bioregistro.fase.application.dtos.TipoEtapaResponse;
import br.bioregistro.fase.application.dtos.TipoPublicacaoResponse;
import br.bioregistro.fase.application.use_cases.ListarTipos;
import br.bioregistro.fase.domain.model.Instituicao;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.util.List;

/** Opções para as telas: tipos de etapa (múltipla escolha da geração) e tipos de publicação. */
@Path("/v1/{instituicao}/tipos")
@Produces(MediaType.APPLICATION_JSON)
public class TipoController {

    private final ListarTipos listarTipos;

    public TipoController(ListarTipos listarTipos) {
        this.listarTipos = listarTipos;
    }

    @GET
    @Path("etapa")
    public List<TipoEtapaResponse> tiposEtapa(@PathParam("instituicao") Instituicao instituicao) {
        return listarTipos.etapas(instituicao);
    }

    @GET
    @Path("publicacao")
    public List<TipoPublicacaoResponse> tiposPublicacao(@PathParam("instituicao") Instituicao instituicao) {
        return listarTipos.publicacoes(instituicao);
    }
}

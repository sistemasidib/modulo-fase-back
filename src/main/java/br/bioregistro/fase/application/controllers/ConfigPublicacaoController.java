package br.bioregistro.fase.application.controllers;

import br.bioregistro.fase.application.dtos.ConfigPublicacaoDtos.ConfigPublicacaoRequest;
import br.bioregistro.fase.application.dtos.ConfigPublicacaoDtos.ConfigPublicacaoResponse;
import br.bioregistro.fase.application.use_cases.ConfiguracoesDePublicacao;
import br.bioregistro.fase.domain.model.Instituicao;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.util.List;

@Path("/v1/{instituicao}")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ConfigPublicacaoController {

    private final ConfiguracoesDePublicacao configs;

    public ConfigPublicacaoController(ConfiguracoesDePublicacao configs) {
        this.configs = configs;
    }

    @GET
    @Path("tipos/publicacao/{tipoPublicacaoId}/config")
    public ConfigPublicacaoResponse padrao(@PathParam("instituicao") Instituicao instituicao,
                                           @PathParam("tipoPublicacaoId") int tipoPublicacaoId) {
        return ConfigPublicacaoResponse.de(configs.padrao(instituicao, tipoPublicacaoId));
    }

    @GET
    @Path("editais/{editalId}/config-publicacoes")
    public List<ConfigPublicacaoResponse> listarEfetivas(@PathParam("instituicao") Instituicao instituicao,
                                                         @PathParam("editalId") int editalId) {
        return configs.listarEfetivas(instituicao, editalId).stream()
                .map(ConfigPublicacaoResponse::de)
                .toList();
    }

    @GET
    @Path("editais/{editalId}/config-publicacoes/{tipoPublicacaoId}")
    public ConfigPublicacaoResponse efetiva(@PathParam("instituicao") Instituicao instituicao,
                                            @PathParam("editalId") int editalId,
                                            @PathParam("tipoPublicacaoId") int tipoPublicacaoId) {
        return ConfigPublicacaoResponse.de(configs.efetiva(instituicao, editalId, tipoPublicacaoId));
    }

    @PUT
    @Path("editais/{editalId}/config-publicacoes/{tipoPublicacaoId}")
    public ConfigPublicacaoResponse salvar(@PathParam("instituicao") Instituicao instituicao,
                                           @PathParam("editalId") int editalId,
                                           @PathParam("tipoPublicacaoId") int tipoPublicacaoId,
                                           ConfigPublicacaoRequest request) {
        return ConfigPublicacaoResponse.de(configs.salvarEdital(instituicao, editalId,
                Parametros.corpo(request).paraDominio(tipoPublicacaoId)));
    }
}

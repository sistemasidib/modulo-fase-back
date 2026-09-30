package br.bioregistro.fase.application.controllers;

import br.bioregistro.fase.application.dtos.CriarPublicacaoRequest;
import br.bioregistro.fase.application.dtos.PublicacaoDatasRequest;
import br.bioregistro.fase.application.dtos.PublicacaoResponse;
import br.bioregistro.fase.application.use_cases.AtualizarPublicacao;
import br.bioregistro.fase.application.use_cases.CriarPublicacao;
import br.bioregistro.fase.application.use_cases.ExcluirPublicacao;
import br.bioregistro.fase.application.use_cases.ListarPublicacoes;
import br.bioregistro.fase.application.use_cases.PublicarPublicacao;
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

/** Espaços de publicação das fases e etapas. */
@Path("/v1/{instituicao}/publicacoes")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PublicacaoController {

    private final ListarPublicacoes listarPublicacoes;
    private final CriarPublicacao criarPublicacao;
    private final AtualizarPublicacao atualizarPublicacao;
    private final PublicarPublicacao publicarPublicacao;
    private final ExcluirPublicacao excluirPublicacao;

    public PublicacaoController(ListarPublicacoes listarPublicacoes, CriarPublicacao criarPublicacao,
                                AtualizarPublicacao atualizarPublicacao, PublicarPublicacao publicarPublicacao,
                                ExcluirPublicacao excluirPublicacao) {
        this.listarPublicacoes = listarPublicacoes;
        this.criarPublicacao = criarPublicacao;
        this.atualizarPublicacao = atualizarPublicacao;
        this.publicarPublicacao = publicarPublicacao;
        this.excluirPublicacao = excluirPublicacao;
    }

    @GET
    public List<PublicacaoResponse> listar(@PathParam("instituicao") Instituicao instituicao,
                                           @QueryParam("faseId") Integer faseId) {
        return listarPublicacoes.executar(instituicao, Parametros.obrigatorio(faseId, "faseId"));
    }

    @POST
    public Response criar(@PathParam("instituicao") Instituicao instituicao,
                          @QueryParam("faseId") Integer faseId,
                          CriarPublicacaoRequest request) {
        PublicacaoResponse criada = criarPublicacao.executar(
                instituicao, Parametros.obrigatorio(faseId, "faseId"), Parametros.corpo(request));
        return Response.status(Response.Status.CREATED).entity(criada).build();
    }

    @PUT
    @Path("{id}")
    public PublicacaoResponse atualizar(@PathParam("instituicao") Instituicao instituicao, @PathParam("id") int id,
                                        PublicacaoDatasRequest request) {
        return atualizarPublicacao.executar(instituicao, id, Parametros.corpo(request));
    }

    @POST
    @Path("{id}/publicar")
    public PublicacaoResponse publicar(@PathParam("instituicao") Instituicao instituicao, @PathParam("id") int id,
                                       PublicacaoDatasRequest request) {
        return publicarPublicacao.executar(instituicao, id, Parametros.corpo(request));
    }

    @DELETE
    @Path("{id}")
    public Response excluir(@PathParam("instituicao") Instituicao instituicao, @PathParam("id") int id) {
        excluirPublicacao.executar(instituicao, id);
        return Response.noContent().build();
    }
}

package br.bioregistro.fase.application.controllers;

import br.bioregistro.fase.application.dtos.CronogramaDtos.CronogramaResponse;
import br.bioregistro.fase.application.dtos.CronogramaDtos.ModeloResponse;
import br.bioregistro.fase.application.dtos.CronogramaDtos.ParticularidadeRequest;
import br.bioregistro.fase.application.dtos.CronogramaDtos.PreviaRequest;
import br.bioregistro.fase.application.dtos.CronogramaDtos.SalvarCronogramaRequest;
import br.bioregistro.fase.application.dtos.CronogramaDtos.StatusRequest;
import br.bioregistro.fase.application.use_cases.ConsultarCronograma;
import br.bioregistro.fase.application.use_cases.EditarCronograma;
import br.bioregistro.fase.domain.model.Instituicao;
import br.bioregistro.fase.domain.model.cronograma.Acao;
import br.bioregistro.fase.domain.model.cronograma.CronogramaCalculado;
import br.bioregistro.fase.domain.model.cronograma.Edicao;
import br.bioregistro.fase.domain.model.cronograma.Previa;
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

import java.util.List;

/** Cronograma do concurso: modelo, cálculo, salvamento, histórico, particularidades e status (spec 003-B). */
@Path("/v1/{instituicao}")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CronogramaController {

    private final ConsultarCronograma consultar;
    private final EditarCronograma editar;

    public CronogramaController(ConsultarCronograma consultar, EditarCronograma editar) {
        this.consultar = consultar;
        this.editar = editar;
    }

    @GET
    @Path("cronograma/modelo")
    public ModeloResponse modelo(@PathParam("instituicao") Instituicao instituicao) {
        return consultar.modelo(instituicao);
    }

    @GET
    @Path("cronogramas/{id}")
    public CronogramaResponse buscar(@PathParam("instituicao") Instituicao instituicao, @PathParam("id") int id) {
        return consultar.buscar(instituicao, id);
    }

    /** Calcula sem gravar (geração ao vivo da aba Fases). */
    @POST
    @Path("cronogramas/{id}/simular")
    public CronogramaCalculado simular(@PathParam("instituicao") Instituicao instituicao, @PathParam("id") int id,
                                       Edicao edicao) {
        return consultar.simular(instituicao, id, edicao);
    }

    /** Efeito de uma edição de célula, sem gravar (barra de confirmação). */
    @POST
    @Path("cronogramas/{id}/previa")
    public Previa previa(@PathParam("instituicao") Instituicao instituicao, @PathParam("id") int id,
                         PreviaRequest request) {
        PreviaRequest corpo = Parametros.corpo(request);
        return consultar.prever(instituicao, id, corpo.edicao(), corpo.mudanca());
    }

    @PUT
    @Path("cronogramas/{id}")
    public CronogramaResponse salvar(@PathParam("instituicao") Instituicao instituicao, @PathParam("id") int id,
                                     @HeaderParam("X-Usuario") String usuario, SalvarCronogramaRequest request) {
        return editar.salvar(instituicao, id, Parametros.corpo(request), Parametros.autor(usuario));
    }

    /** Só leitura: o histórico não tem edição nem exclusão (RN27). */
    @GET
    @Path("cronogramas/{id}/historico")
    public List<Acao> historico(@PathParam("instituicao") Instituicao instituicao, @PathParam("id") int id) {
        return consultar.historico(instituicao, id);
    }

    @POST
    @Path("cronogramas/{id}/particularidades")
    public CronogramaResponse adicionarParticularidade(@PathParam("instituicao") Instituicao instituicao,
                                                       @PathParam("id") int id,
                                                       @HeaderParam("X-Usuario") String usuario,
                                                       ParticularidadeRequest request) {
        return editar.adicionarParticularidade(instituicao, id, Parametros.corpo(request), Parametros.autor(usuario));
    }

    @DELETE
    @Path("cronogramas/{id}/particularidades/{particularidadeId}")
    public CronogramaResponse removerParticularidade(@PathParam("instituicao") Instituicao instituicao,
                                                     @PathParam("id") int id,
                                                     @PathParam("particularidadeId") int particularidadeId,
                                                     @HeaderParam("X-Usuario") String usuario) {
        return editar.removerParticularidade(instituicao, id, particularidadeId, Parametros.autor(usuario));
    }

    @PUT
    @Path("cronogramas/{id}/etapas/{codigo}/status")
    public CronogramaResponse marcarStatus(@PathParam("instituicao") Instituicao instituicao, @PathParam("id") int id,
                                           @PathParam("codigo") String codigo,
                                           @HeaderParam("X-Usuario") String usuario, StatusRequest request) {
        return editar.marcarStatus(instituicao, id, codigo, Parametros.corpo(request).status(),
                Parametros.autor(usuario));
    }

    /** Gatilho sem corpo: aceita qualquer Content-Type. */
    @POST
    @Path("cronogramas/{id}/etapas/{codigo}/primeiro-recurso")
    @Consumes(MediaType.WILDCARD)
    public CronogramaResponse registrarPrimeiroRecurso(@PathParam("instituicao") Instituicao instituicao,
                                                       @PathParam("id") int id, @PathParam("codigo") String codigo,
                                                       @HeaderParam("X-Usuario") String usuario) {
        return editar.registrarPrimeiroRecurso(instituicao, id, codigo, Parametros.autor(usuario));
    }
}

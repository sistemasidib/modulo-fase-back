package br.bioregistro.fase.application.controllers;

import br.bioregistro.fase.application.dtos.CronogramaPublicoDtos.ItemRequest;
import br.bioregistro.fase.application.dtos.CronogramaPublicoDtos.ItemResponse;
import br.bioregistro.fase.application.dtos.CronogramaPublicoDtos.SalvarRequest;
import br.bioregistro.fase.application.use_cases.ItensDoCronogramaPublico;
import br.bioregistro.fase.domain.model.Instituicao;
import br.bioregistro.fase.domain.model.cronograma.ItemCronogramaPublico;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.ArrayList;
import java.util.List;

/**
 * Cronograma Público do edital no formato Anexo II (datas absolutas + PDF).
 */
@Path("/v1/{instituicao}/editais/{editalId}/cronograma-publico")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CronogramaPublicoController {

    private final ItensDoCronogramaPublico casos;

    public CronogramaPublicoController(ItensDoCronogramaPublico casos) {
        this.casos = casos;
    }

    @GET
    public List<ItemResponse> listar(@PathParam("instituicao") Instituicao instituicao,
                                     @PathParam("editalId") int editalId) {
        return casos.listar(instituicao, editalId).stream().map(ItemResponse::de).toList();
    }

    @PUT
    public List<ItemResponse> salvar(@PathParam("instituicao") Instituicao instituicao,
                                     @PathParam("editalId") int editalId,
                                     SalvarRequest request) {
        SalvarRequest corpo = Parametros.corpo(request);
        List<ItemRequest> pedidos = corpo.itens() == null ? List.of() : corpo.itens();
        List<ItemCronogramaPublico> dominio = new ArrayList<>();
        int ordem = 1;
        for (ItemRequest item : pedidos) {
            dominio.add(item.paraDominio(ordem++));
        }
        return casos.salvar(instituicao, editalId, dominio).stream().map(ItemResponse::de).toList();
    }

    @GET
    @Path("pdf")
    @Produces("application/pdf")
    public Response pdf(@PathParam("instituicao") Instituicao instituicao,
                        @PathParam("editalId") int editalId) {
        byte[] bytes = casos.pdf(instituicao, editalId);
        return Response.ok(bytes)
                .header("Content-Disposition", "attachment; filename=\"cronograma-publico-" + editalId + ".pdf\"")
                .build();
    }
}

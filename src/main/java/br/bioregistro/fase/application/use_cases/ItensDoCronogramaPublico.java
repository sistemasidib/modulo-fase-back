package br.bioregistro.fase.application.use_cases;

import br.bioregistro.fase.domain.exception.RegraDeNegocioException;
import br.bioregistro.fase.domain.model.Instituicao;
import br.bioregistro.fase.domain.model.cronograma.CronogramaPublico;
import br.bioregistro.fase.domain.model.cronograma.ItemCronogramaPublico;
import br.bioregistro.fase.domain.model.cronograma.PdfCronogramaPublico;
import br.bioregistro.fase.domain.port.CronogramaPublicoItemRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Cronograma público do edital (Anexo II): lista de atividades com datas absolutas + PDF.
 * Sem ponte com fase (RN09c). Pode ser alterado e salvo novamente a qualquer momento
 * ({@link #salvar} substitui a lista completa).
 */
@ApplicationScoped
public class ItensDoCronogramaPublico {

    private final CronogramaPublicoItemRepository itens;
    private final Clock relogio;

    public ItensDoCronogramaPublico(CronogramaPublicoItemRepository itens, Clock relogio) {
        this.itens = itens;
        this.relogio = relogio;
    }

    public List<ItemCronogramaPublico> listar(Instituicao instituicao, int editalId) {
        validarEdital(editalId);
        return itens.listar(instituicao, editalId);
    }

    public List<ItemCronogramaPublico> salvar(Instituicao instituicao, int editalId,
                                              List<ItemCronogramaPublico> novos) {
        validarEdital(editalId);
        List<ItemCronogramaPublico> lista = novos == null ? List.of() : novos;
        int ordem = 1;
        List<ItemCronogramaPublico> normalizados = new ArrayList<>();
        for (ItemCronogramaPublico item : lista) {
            normalizados.add(new ItemCronogramaPublico(
                    ordem++, item.descricao(), item.inicio(), item.termino(), item.destaque()));
        }
        return itens.substituir(instituicao, editalId, normalizados);
    }

    public CronogramaPublico retrato(Instituicao instituicao, int editalId) {
        validarEdital(editalId);
        List<ItemCronogramaPublico> lista = itens.listar(instituicao, editalId);
        return CronogramaPublico.deItens(
                "Edital " + editalId,
                List.of("Edital " + editalId),
                OffsetDateTime.now(relogio),
                lista);
    }

    public byte[] pdf(Instituicao instituicao, int editalId) {
        return PdfCronogramaPublico.gerar(retrato(instituicao, editalId));
    }

    private static void validarEdital(int editalId) {
        if (editalId <= 0) {
            throw new RegraDeNegocioException("Informe o edital do cronograma público.");
        }
    }
}

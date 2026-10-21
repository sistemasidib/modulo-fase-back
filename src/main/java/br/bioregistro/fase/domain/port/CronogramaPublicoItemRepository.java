package br.bioregistro.fase.domain.port;

import br.bioregistro.fase.domain.model.Instituicao;
import br.bioregistro.fase.domain.model.cronograma.ItemCronogramaPublico;

import java.util.List;

public interface CronogramaPublicoItemRepository {

    List<ItemCronogramaPublico> listar(Instituicao instituicao, int editalId);

    /** Substitui todos os itens do edital pela lista informada (ordem = posição na lista). */
    List<ItemCronogramaPublico> substituir(Instituicao instituicao, int editalId, List<ItemCronogramaPublico> itens);
}

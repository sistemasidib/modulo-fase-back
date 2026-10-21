package br.bioregistro.fase.application.use_cases;

import br.bioregistro.fase.domain.model.Instituicao;
import br.bioregistro.fase.domain.model.cronograma.ItemCronogramaPublico;
import br.bioregistro.fase.domain.port.CronogramaPublicoItemRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CronogramaPublicoItensEmMemoria implements CronogramaPublicoItemRepository {

    private final Map<String, List<ItemCronogramaPublico>> porEdital = new HashMap<>();

    @Override
    public List<ItemCronogramaPublico> listar(Instituicao instituicao, int editalId) {
        return List.copyOf(porEdital.getOrDefault(chave(instituicao, editalId), List.of()));
    }

    @Override
    public List<ItemCronogramaPublico> substituir(Instituicao instituicao, int editalId,
                                                  List<ItemCronogramaPublico> itens) {
        int ordem = 1;
        List<ItemCronogramaPublico> gravados = new ArrayList<>();
        for (ItemCronogramaPublico item : itens) {
            gravados.add(new ItemCronogramaPublico(
                    ordem++, item.descricao(), item.inicio(), item.termino(), item.destaque()));
        }
        porEdital.put(chave(instituicao, editalId), List.copyOf(gravados));
        return List.copyOf(gravados);
    }

    private static String chave(Instituicao instituicao, int editalId) {
        return instituicao.name() + ":" + editalId;
    }
}

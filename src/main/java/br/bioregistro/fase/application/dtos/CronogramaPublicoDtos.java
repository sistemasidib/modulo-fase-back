package br.bioregistro.fase.application.dtos;

import br.bioregistro.fase.domain.model.cronograma.ItemCronogramaPublico;

import java.time.LocalDate;
import java.util.List;

public final class CronogramaPublicoDtos {

    private CronogramaPublicoDtos() {
    }

    public record ItemRequest(String descricao, LocalDate inicio, LocalDate termino, Boolean destaque) {
        public ItemCronogramaPublico paraDominio(int ordem) {
            return new ItemCronogramaPublico(ordem, descricao, inicio, termino, Boolean.TRUE.equals(destaque));
        }
    }

    public record SalvarRequest(List<ItemRequest> itens) {
    }

    public record ItemResponse(int ordem, String descricao, LocalDate inicio, LocalDate termino, boolean destaque) {
        public static ItemResponse de(ItemCronogramaPublico item) {
            return new ItemResponse(item.ordem(), item.descricao(), item.inicio(), item.termino(), item.destaque());
        }
    }
}

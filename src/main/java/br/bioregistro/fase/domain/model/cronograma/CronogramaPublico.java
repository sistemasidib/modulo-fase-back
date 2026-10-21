package br.bioregistro.fase.domain.model.cronograma;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Retrato do cronograma público (spec 006, RN07–RN09) no formato Anexo II: itens com
 * descrição e datas absolutas. PDF via {@link PdfCronogramaPublico}.
 */
public record CronogramaPublico(
        String nomeConcurso,
        List<String> editais,
        OffsetDateTime geradoEm,
        List<Item> itens
) {

    public record Item(int ordem, String descricao, LocalDate inicio, LocalDate fim, boolean destaque) {
    }

    public CronogramaPublico {
        editais = editais == null ? List.of() : List.copyOf(editais);
        itens = itens == null ? List.of() : List.copyOf(itens);
    }

    public static CronogramaPublico deItens(String nomeConcurso, List<String> editais, OffsetDateTime geradoEm,
                                           List<ItemCronogramaPublico> itens) {
        List<Item> linhas = new ArrayList<>();
        for (ItemCronogramaPublico i : itens) {
            linhas.add(new Item(i.ordem(), i.descricao(), i.inicio(), i.termino(), i.destaque()));
        }
        return new CronogramaPublico(nomeConcurso, editais, geradoEm, linhas);
    }

    /**
     * Compatibilidade: achata etapas ativas do cálculo antigo em itens numerados (sem destaque).
     */
    public static CronogramaPublico de(String nomeConcurso, List<String> editais, OffsetDateTime geradoEm,
                                       CronogramaCalculado calculado) {
        List<Item> itens = new ArrayList<>();
        int ordem = 1;
        for (CronogramaCalculado.Grupo grupo : calculado.grupos()) {
            if (!grupo.ativo()) {
                continue;
            }
            for (EtapaCalculada e : grupo.etapas()) {
                if (!e.ativa() || e.personalizada()) {
                    continue;
                }
                itens.add(new Item(ordem++, e.descricao(), e.inicio(), e.fim(), false));
            }
        }
        return new CronogramaPublico(nomeConcurso, editais, geradoEm, itens);
    }
}

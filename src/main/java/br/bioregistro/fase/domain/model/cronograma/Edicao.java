package br.bioregistro.fase.domain.model.cronograma;

import br.bioregistro.fase.domain.exception.RegraDeNegocioException;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

/**
 * O que o usuário escolhe e ajusta no cronograma. Guarda ajustes, nunca datas: as datas são
 * sempre derivadas pelo {@link CalculoCronograma} (P4).
 *
 * @param modulos    módulos ligados (S12…S05); o núcleo é sempre ligado
 * @param dataEdital marco zero (E01); {@code null} = rascunho
 * @param ajustes    código da etapa → dias/duração diferentes do modelo
 * @param removidas  etapas riscadas (RN22)
 * @param excecoes   prazos padrão liberados (RN19), pela chave do parâmetro
 * @param itens      linhas acrescentadas pelo usuário, fora do esqueleto
 * @param dataProva  data da prova objetiva (E15), informada pelo usuário em qualquer dia da semana;
 *                   {@code null} = a E15 é uma sugestão provisória (fim das inscrições + intervalo)
 */
public record Edicao(Set<String> modulos, LocalDate dataEdital, Map<String, Ajuste> ajustes,
                     Set<String> removidas, Set<String> excecoes, List<ItemPersonalizado> itens,
                     LocalDate dataProva) {

    public record Ajuste(Integer dias, Integer duracao) {
    }

    public Edicao {
        modulos = Set.copyOf(modulos == null ? Set.of() : modulos);
        ajustes = Map.copyOf(ajustes == null ? Map.of() : ajustes);
        removidas = Set.copyOf(removidas == null ? Set.of() : removidas);
        excecoes = Set.copyOf(excecoes == null ? Set.of() : excecoes);
        itens = List.copyOf(itens == null ? List.of() : itens);
    }

    public Edicao(Set<String> modulos, LocalDate dataEdital, Map<String, Ajuste> ajustes, Set<String> removidas,
                  Set<String> excecoes, List<ItemPersonalizado> itens) {
        this(modulos, dataEdital, ajustes, removidas, excecoes, itens, null);
    }

    public Edicao(Set<String> modulos, LocalDate dataEdital, Map<String, Ajuste> ajustes, Set<String> removidas,
                  Set<String> excecoes) {
        this(modulos, dataEdital, ajustes, removidas, excecoes, List.of(), null);
    }

    public static Edicao inicial() {
        return new Edicao(Set.of(), null, Map.of(), Set.of(), Set.of(), List.of(), null);
    }

    /** Estrutura válida contra o esqueleto: módulos, códigos, âncoras e durações. */
    public void validar(Esqueleto esqueleto) {
        for (String modulo : modulos) {
            if (!esqueleto.modulos().containsKey(modulo)) {
                throw new RegraDeNegocioException("Módulo desconhecido: " + modulo + ".");
            }
        }
        for (String codigo : removidas) {
            if (!esqueleto.etapa(codigo).removivel()) {
                throw new RegraDeNegocioException("A etapa " + codigo + " não pode ser removida.");
            }
        }
        for (String chave : excecoes) {
            if (!Parametros.PRAZOS_PADRAO.contains(chave)) {
                throw new RegraDeNegocioException("Prazo padrão desconhecido: " + chave + ".");
            }
        }
        ajustes.forEach((codigo, ajuste) -> {
            EtapaModelo etapa = esqueleto.etapa(codigo);
            if (ajuste.dias() != null && !etapa.temDias()) {
                throw new RegraDeNegocioException("A etapa " + codigo + " é o marco zero e não tem dias.");
            }
            if (ajuste.duracao() != null && ajuste.duracao() < 0) {
                throw new RegraDeNegocioException("A duração da etapa " + codigo + " não pode ser negativa.");
            }
        });
        if (dataProva != null && dataEdital == null) {
            throw new RegraDeNegocioException("Informe a data do edital antes da data da prova.");
        }
        if (dataProva != null && !dataProva.isAfter(dataEdital)) {
            throw new RegraDeNegocioException("A data da prova precisa ser depois da data do edital.");
        }
        if (itens.size() > ItemPersonalizado.MAXIMO) {
            throw new RegraDeNegocioException("O cronograma aceita até " + ItemPersonalizado.MAXIMO + " itens próprios.");
        }
        Set<String> codigos = new HashSet<>();
        for (ItemPersonalizado item : itens) {
            item.validar(esqueleto);
            if (!codigos.add(item.codigo())) {
                throw new RegraDeNegocioException("O código " + item.codigo() + " aparece em mais de um item.");
            }
        }
    }

    public Optional<ItemPersonalizado> item(String codigo) {
        return itens.stream().filter(i -> i.codigo().equals(codigo)).findFirst();
    }

    public boolean provaInformada() {
        return dataProva != null;
    }

    public Ajuste ajuste(String codigo) {
        return ajustes.get(codigo);
    }

    public Edicao comAjuste(String codigo, Ajuste ajuste) {
        Map<String, Ajuste> novos = new HashMap<>(ajustes);
        if (ajuste == null || (ajuste.dias() == null && ajuste.duracao() == null)) {
            novos.remove(codigo);
        } else {
            novos.put(codigo, ajuste);
        }
        return new Edicao(modulos, dataEdital, novos, removidas, excecoes, itens, dataProva);
    }

    /** Ordenado, para gravar e comparar sem depender da ordem do Set. */
    public String modulosTexto() {
        return String.join(",", new TreeSet<>(modulos));
    }

    public Map<String, Ajuste> ajustesOrdenados() {
        Map<String, Ajuste> ordem = new LinkedHashMap<>();
        new TreeSet<>(ajustes.keySet()).forEach(c -> ordem.put(c, ajustes.get(c)));
        return ordem;
    }
}

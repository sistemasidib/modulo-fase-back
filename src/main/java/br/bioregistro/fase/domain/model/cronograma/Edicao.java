package br.bioregistro.fase.domain.model.cronograma;

import br.bioregistro.fase.domain.exception.RegraDeNegocioException;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
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
 */
public record Edicao(Set<String> modulos, LocalDate dataEdital, Map<String, Ajuste> ajustes,
                     Set<String> removidas, Set<String> excecoes) {

    public record Ajuste(Integer dias, Integer duracao) {
    }

    public Edicao {
        modulos = Set.copyOf(modulos == null ? Set.of() : modulos);
        ajustes = Map.copyOf(ajustes == null ? Map.of() : ajustes);
        removidas = Set.copyOf(removidas == null ? Set.of() : removidas);
        excecoes = Set.copyOf(excecoes == null ? Set.of() : excecoes);
    }

    public static Edicao inicial() {
        return new Edicao(Set.of(), null, Map.of(), Set.of(), Set.of());
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
    }

    /** RN11: só vale para uma data nova; um edital já publicado continua válido. */
    public void exigirEditalNoPrazo(LocalDate hoje) {
        if (dataEdital != null && dataEdital.isBefore(hoje)) {
            throw new RegraDeNegocioException("A data do edital não pode ser anterior a hoje.");
        }
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
        return new Edicao(modulos, dataEdital, novos, removidas, excecoes);
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

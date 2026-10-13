package br.bioregistro.fase.domain.model.cronograma;

import br.bioregistro.fase.domain.exception.RegraDeNegocioException;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/**
 * O que um edital muda no cronograma que ele segue (spec 005): só o que difere. Mudar o
 * compartilhado vale para todos os editais, menos no que cada um sobrepôs; mudar um edital mexe só
 * nele. Ausente/nulo = herda do compartilhado.
 *
 * <p>Exceções de prazo e itens próprios não se sobrepõem: valem para todos os editais do cronograma.
 *
 * @param ajustes   por código; {@code Ajuste(null, null)} = volta ao valor do modelo neste edital
 * @param removidas por código; {@code true} = removida neste edital, {@code false} = restaurada
 */
public record Sobreposicao(LocalDate dataEdital, LocalDate dataProva, Set<String> modulos,
                           Map<String, Edicao.Ajuste> ajustes, Map<String, Boolean> removidas,
                           Map<String, StatusEtapa> status) {

    public Sobreposicao {
        modulos = modulos == null ? null : Set.copyOf(modulos);
        ajustes = Map.copyOf(ajustes == null ? Map.of() : ajustes);
        removidas = Map.copyOf(removidas == null ? Map.of() : removidas);
        status = Map.copyOf(status == null ? Map.of() : status);
    }

    public static Sobreposicao vazia() {
        return new Sobreposicao(null, null, null, Map.of(), Map.of(), Map.of());
    }

    public boolean isVazia() {
        return dataEdital == null && dataProva == null && modulos == null && ajustes.isEmpty() && removidas.isEmpty()
                && status.isEmpty();
    }

    /** Edição efetiva do edital: a do compartilhado com a sobreposição por cima. */
    public Edicao aplicar(Edicao base) {
        Map<String, Edicao.Ajuste> efetivos = new HashMap<>(base.ajustes());
        ajustes.forEach((codigo, ajuste) -> {
            if (ajuste.dias() == null && ajuste.duracao() == null) {
                efetivos.remove(codigo);
            } else {
                efetivos.put(codigo, ajuste);
            }
        });
        Set<String> removidasEfetivas = new HashSet<>(base.removidas());
        removidas.forEach((codigo, removida) -> {
            if (removida) {
                removidasEfetivas.add(codigo);
            } else {
                removidasEfetivas.remove(codigo);
            }
        });
        return new Edicao(modulos != null ? modulos : base.modulos(),
                dataEdital != null ? dataEdital : base.dataEdital(), efetivos, removidasEfetivas, base.excecoes(),
                base.itens(), dataProva != null ? dataProva : base.dataProva());
    }

    public Map<String, StatusEtapa> aplicarStatus(Map<String, StatusEtapa> base) {
        Map<String, StatusEtapa> efetivo = new HashMap<>(base);
        efetivo.putAll(status);
        return efetivo;
    }

    /**
     * Guarda só o que a edição do edital tem de diferente do compartilhado. O front manda a edição
     * completa do edital, como no compartilhado; os status sobrepostos continuam os mesmos.
     */
    public static Sobreposicao diferenca(Edicao base, Edicao efetiva, Map<String, StatusEtapa> status) {
        if (!efetiva.excecoes().equals(base.excecoes()) || !efetiva.itens().equals(base.itens())) {
            throw new RegraDeNegocioException("Exceções de prazo e itens próprios valem para todos os editais do "
                    + "cronograma: mude no cronograma compartilhado ou separe o edital.");
        }
        if (efetiva.dataEdital() == null && base.dataEdital() != null) {
            throw new RegraDeNegocioException("O cronograma compartilhado já tem data do edital. "
                    + "Para voltar a ela, restaure o edital.");
        }
        if (efetiva.dataProva() == null && base.dataProva() != null) {
            throw new RegraDeNegocioException("O cronograma compartilhado já tem data da prova. "
                    + "Para voltar a ela, restaure o edital.");
        }
        Map<String, Edicao.Ajuste> ajustes = new HashMap<>();
        Set<String> codigos = new TreeSet<>(base.ajustes().keySet());
        codigos.addAll(efetiva.ajustes().keySet());
        for (String codigo : codigos) {
            Edicao.Ajuste doEdital = efetiva.ajuste(codigo);
            if (!Objects.equals(base.ajuste(codigo), doEdital)) {
                ajustes.put(codigo, doEdital == null ? new Edicao.Ajuste(null, null) : doEdital);
            }
        }
        Map<String, Boolean> removidas = new HashMap<>();
        Set<String> riscadas = new TreeSet<>(base.removidas());
        riscadas.addAll(efetiva.removidas());
        for (String codigo : riscadas) {
            boolean noEdital = efetiva.removidas().contains(codigo);
            if (base.removidas().contains(codigo) != noEdital) {
                removidas.put(codigo, noEdital);
            }
        }
        return new Sobreposicao(
                Objects.equals(base.dataEdital(), efetiva.dataEdital()) ? null : efetiva.dataEdital(),
                Objects.equals(base.dataProva(), efetiva.dataProva()) ? null : efetiva.dataProva(),
                base.modulos().equals(efetiva.modulos()) ? null : efetiva.modulos(),
                ajustes, removidas, status);
    }

    public Sobreposicao comStatus(String codigo, StatusEtapa novo) {
        Map<String, StatusEtapa> novos = new HashMap<>(status);
        novos.put(codigo, novo);
        return new Sobreposicao(dataEdital, dataProva, modulos, ajustes, removidas, novos);
    }

    /** O que este edital sobrepõe, para a tela mostrar onde ele diverge do compartilhado. */
    public Set<String> codigos() {
        Set<String> codigos = new TreeSet<>(ajustes.keySet());
        codigos.addAll(removidas.keySet());
        codigos.addAll(status.keySet());
        if (dataEdital != null) {
            codigos.add(EtapaModelo.EDITAL);
        }
        if (dataProva != null) {
            codigos.add(EtapaModelo.PROVA);
        }
        return codigos;
    }
}

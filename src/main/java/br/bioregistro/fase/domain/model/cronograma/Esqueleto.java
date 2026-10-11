package br.bioregistro.fase.domain.model.cronograma;

import br.bioregistro.fase.domain.exception.NaoEncontradoException;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Modelo do cronograma: grupos na ordem de exibição e etapas na ordem de cálculo (toda
 * referência aponta para uma etapa anterior na lista).
 *
 * @param modulos código → nome curto (S12 → Discursiva)
 * @param atalhos nome → módulos ligados
 */
public record Esqueleto(String nome, List<GrupoModelo> grupos, List<EtapaModelo> etapas,
                        Map<String, String> modulos, Map<String, List<String>> atalhos) {

    public Esqueleto {
        grupos = List.copyOf(grupos);
        etapas = List.copyOf(etapas);
    }

    public EtapaModelo etapa(String codigo) {
        EtapaModelo etapa = porCodigo().get(codigo);
        if (etapa == null) {
            throw new NaoEncontradoException("Etapa do cronograma", codigo);
        }
        return etapa;
    }

    public boolean existe(String codigo) {
        return porCodigo().containsKey(codigo);
    }

    public GrupoModelo grupo(String codigo) {
        return grupos.stream().filter(g -> g.codigo().equals(codigo)).findFirst()
                .orElseThrow(() -> new NaoEncontradoException("Grupo do cronograma", codigo));
    }

    public List<EtapaModelo> etapasDoGrupo(String codigoGrupo) {
        return etapas.stream().filter(e -> e.grupo().equals(codigoGrupo)).toList();
    }

    /** Quantas etapas cada módulo acrescenta (botões da aba Fases). */
    public long etapasDoModulo(String modulo) {
        return grupos.stream().filter(g -> modulo.equals(g.modulo()))
                .mapToLong(g -> etapasDoGrupo(g.codigo()).size()).sum();
    }

    private Map<String, EtapaModelo> porCodigo() {
        return etapas.stream().collect(Collectors.toMap(EtapaModelo::codigo, Function.identity()));
    }
}

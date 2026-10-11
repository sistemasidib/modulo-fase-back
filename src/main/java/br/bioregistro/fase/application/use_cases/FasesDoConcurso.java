package br.bioregistro.fase.application.use_cases;

import br.bioregistro.fase.application.dtos.FaseDoConcursoResponse;
import br.bioregistro.fase.domain.exception.NaoEncontradoException;
import br.bioregistro.fase.domain.model.Etapa;
import br.bioregistro.fase.domain.model.Fase;
import br.bioregistro.fase.domain.model.GrupoCargo;
import br.bioregistro.fase.domain.model.Instituicao;
import br.bioregistro.fase.domain.model.TipoEtapa;
import br.bioregistro.fase.domain.port.EtapaRepository;
import br.bioregistro.fase.domain.port.FaseRepository;
import br.bioregistro.fase.domain.port.GrupoCargoRepository;
import br.bioregistro.fase.domain.port.TipoRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Fases do concurso, derivadas das fases dos grupos de cargos (sem tabela própria). Cada grupo tem
 * a sua fase; a fase do concurso é o tipo de etapa de execução que aparece nas fases dos grupos.
 * A ordem segue a menor ordem de fase em que o tipo aparece no edital.
 */
@ApplicationScoped
public class FasesDoConcurso {

    private final GrupoCargoRepository grupos;
    private final FaseRepository fases;
    private final EtapaRepository etapas;
    private final TipoRepository tipos;

    public FasesDoConcurso(GrupoCargoRepository grupos, FaseRepository fases, EtapaRepository etapas,
                           TipoRepository tipos) {
        this.grupos = grupos;
        this.fases = fases;
        this.etapas = etapas;
        this.tipos = tipos;
    }

    public List<FaseDoConcursoResponse> listar(Instituicao instituicao, int editalId) {
        if (!fases.editalExiste(instituicao, editalId)) {
            throw new NaoEncontradoException("Edital", editalId);
        }
        Map<Integer, TipoEtapa> execucao = tipos.tiposEtapa(instituicao).stream()
                .filter(t -> t.categoria() == TipoEtapa.Categoria.EXECUCAO)
                .collect(Collectors.toMap(TipoEtapa::id, Function.identity()));

        Map<Integer, Acumulado> porTipo = new LinkedHashMap<>();
        for (GrupoCargo grupo : grupos.listarPorEdital(instituicao, editalId)) {
            for (Fase fase : fases.listarPorGrupo(instituicao, grupo.id())) {
                for (Etapa etapa : etapas.listarPorFase(instituicao, fase.id())) {
                    TipoEtapa tipo = execucao.get(etapa.tipoId());
                    if (tipo == null) {
                        continue;
                    }
                    Acumulado a = porTipo.computeIfAbsent(tipo.id(), id -> new Acumulado(tipo));
                    a.menorOrdem = Math.min(a.menorOrdem, fase.ordem());
                    a.grupoIds.add(grupo.id());
                }
            }
        }

        List<Acumulado> ordenados = new ArrayList<>(porTipo.values());
        ordenados.sort(Comparator.comparingInt((Acumulado a) -> a.menorOrdem).thenComparing(a -> a.tipo.id()));
        List<FaseDoConcursoResponse> resposta = new ArrayList<>();
        for (int i = 0; i < ordenados.size(); i++) {
            Acumulado a = ordenados.get(i);
            resposta.add(FaseDoConcursoResponse.de(i + 1, a.tipo, List.copyOf(a.grupoIds)));
        }
        return resposta;
    }

    private static final class Acumulado {
        final TipoEtapa tipo;
        int menorOrdem = Integer.MAX_VALUE;
        final Set<Integer> grupoIds = new TreeSet<>();

        Acumulado(TipoEtapa tipo) {
            this.tipo = tipo;
        }
    }
}

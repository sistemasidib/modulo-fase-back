package br.bioregistro.fase.application.use_cases;

import br.bioregistro.fase.domain.model.Cargo;
import br.bioregistro.fase.domain.model.GrupoCargo;
import br.bioregistro.fase.domain.model.Instituicao;
import br.bioregistro.fase.domain.port.GrupoCargoRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

/** Repositório falso de grupos. Usa uma única instituição por teste, para simplificar. */
public class GruposEmMemoria implements GrupoCargoRepository {

    private final FasesEmMemoria fases;
    final Map<Integer, GrupoCargo> grupos = new HashMap<>();
    final Map<Integer, List<Cargo>> cargosPorEdital = new HashMap<>();
    /** cargoId → grupoId */
    final Map<Integer, Integer> grupoPorCargo = new HashMap<>();
    private int proximoId = 1;

    public GruposEmMemoria(FasesEmMemoria fases) {
        this.fases = fases;
    }

    public GruposEmMemoria comCargos(int editalId, Integer... cargoIds) {
        List<Cargo> cargos = new ArrayList<>();
        for (Integer id : cargoIds) {
            cargos.add(new Cargo(id, "Cargo " + id));
        }
        cargosPorEdital.put(editalId, cargos);
        return this;
    }

    @Override
    public Optional<GrupoCargo> buscar(Instituicao instituicao, int id) {
        return Optional.ofNullable(grupos.get(id));
    }

    @Override
    public List<GrupoCargo> listarPorEdital(Instituicao instituicao, int editalId) {
        return grupos.values().stream().filter(g -> g.editalId() == editalId).toList();
    }

    @Override
    public boolean nomeEmUso(Instituicao instituicao, int editalId, String nome, Integer ignorarGrupoId) {
        return grupos.values().stream().anyMatch(g -> g.editalId() == editalId && g.nome().equals(nome)
                && !Objects.equals(g.id(), ignorarGrupoId));
    }

    @Override
    public GrupoCargo salvar(Instituicao instituicao, GrupoCargo grupo) {
        GrupoCargo salvo = grupo.id() == null ? grupo.comId(proximoId++) : grupo;
        grupos.put(salvo.id(), salvo);
        return salvo;
    }

    @Override
    public void excluir(Instituicao instituicao, int id) {
        grupos.remove(id);
    }

    @Override
    public List<Cargo> cargosDoEdital(Instituicao instituicao, int editalId) {
        return cargosPorEdital.getOrDefault(editalId, List.of());
    }

    @Override
    public List<Integer> cargosDoGrupo(Instituicao instituicao, int grupoId) {
        return grupoPorCargo.entrySet().stream()
                .filter(e -> e.getValue() == grupoId)
                .map(Map.Entry::getKey)
                .sorted()
                .toList();
    }

    @Override
    public Optional<Integer> grupoDoCargo(Instituicao instituicao, int cargoId) {
        return Optional.ofNullable(grupoPorCargo.get(cargoId));
    }

    @Override
    public void adicionarCargo(Instituicao instituicao, int grupoId, int cargoId) {
        grupoPorCargo.put(cargoId, grupoId);
    }

    @Override
    public void removerCargo(Instituicao instituicao, int grupoId, int cargoId) {
        grupoPorCargo.remove(cargoId, grupoId);
    }

    @Override
    public void vincularFase(Instituicao instituicao, int faseId, int grupoId) {
        fases.gruposDaFase.computeIfAbsent(instituicao + ":" + faseId, k -> new TreeSet<>()).add(grupoId);
    }

    @Override
    public void desvincularFase(Instituicao instituicao, int faseId) {
        fases.gruposDaFase.remove(instituicao + ":" + faseId);
    }

    @Override
    public void desvincularFase(Instituicao instituicao, int faseId, int grupoId) {
        fases.gruposDaFase.getOrDefault(instituicao + ":" + faseId, new TreeSet<>()).remove(grupoId);
    }

    @Override
    public List<Integer> gruposDaFase(Instituicao instituicao, int faseId) {
        return List.copyOf(fases.gruposDaFase.getOrDefault(instituicao + ":" + faseId, Set.of()));
    }

    @Override
    public boolean possuiFases(Instituicao instituicao, int grupoId) {
        return fases.gruposDaFase.values().stream().anyMatch(g -> g.contains(grupoId));
    }
}

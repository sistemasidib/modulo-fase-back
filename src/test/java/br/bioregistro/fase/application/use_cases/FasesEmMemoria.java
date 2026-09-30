package br.bioregistro.fase.application.use_cases;

import br.bioregistro.fase.domain.model.Fase;
import br.bioregistro.fase.domain.model.Instituicao;
import br.bioregistro.fase.domain.port.FaseRepository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/** Repositório falso para testar os use cases sem banco. */
class FasesEmMemoria implements FaseRepository {

    final Map<Instituicao, Map<Integer, Fase>> fases = new HashMap<>();
    /** faseId → grupoId, preenchido por {@link GruposEmMemoria#vincularFase}. */
    final Map<String, Integer> grupoDaFase = new HashMap<>();
    final Set<String> editais = new HashSet<>();
    private int proximoId = 1;

    FasesEmMemoria comEdital(Instituicao instituicao, int editalId) {
        editais.add(instituicao + ":" + editalId);
        return this;
    }

    private Map<Integer, Fase> de(Instituicao instituicao) {
        return fases.computeIfAbsent(instituicao, i -> new HashMap<>());
    }

    @Override
    public Optional<Fase> buscar(Instituicao instituicao, int id) {
        return Optional.ofNullable(de(instituicao).get(id));
    }

    @Override
    public List<Fase> listarPorEdital(Instituicao instituicao, int editalId) {
        return new ArrayList<>(de(instituicao).values()).stream()
                .filter(f -> f.editalId() == editalId)
                .sorted(Comparator.comparing(Fase::ordem))
                .toList();
    }

    @Override
    public List<Fase> listarPorGrupo(Instituicao instituicao, int grupoId) {
        return de(instituicao).values().stream()
                .filter(f -> Objects.equals(grupoDaFase.get(instituicao + ":" + f.id()), grupoId))
                .sorted(Comparator.comparing(Fase::ordem))
                .toList();
    }

    @Override
    public short maiorOrdem(Instituicao instituicao, int editalId) {
        return de(instituicao).values().stream()
                .filter(f -> f.editalId() == editalId)
                .map(Fase::ordem)
                .max(Short::compare)
                .orElse((short) 0);
    }

    @Override
    public boolean editalExiste(Instituicao instituicao, int editalId) {
        return editais.contains(instituicao + ":" + editalId);
    }

    @Override
    public boolean ordemEmUso(Instituicao instituicao, int editalId, short ordem, Integer ignorarFaseId) {
        return de(instituicao).values().stream()
                .anyMatch(f -> f.editalId() == editalId && f.ordem() == ordem && !Objects.equals(f.id(), ignorarFaseId));
    }

    @Override
    public Fase salvar(Instituicao instituicao, Fase fase) {
        Fase salva = fase.id() == null ? fase.comId(proximoId++) : fase;
        de(instituicao).put(salva.id(), salva);
        return salva;
    }

    @Override
    public void excluir(Instituicao instituicao, int id) {
        de(instituicao).remove(id);
    }
}

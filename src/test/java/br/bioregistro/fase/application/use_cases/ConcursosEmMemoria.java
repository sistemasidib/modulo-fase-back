package br.bioregistro.fase.application.use_cases;

import br.bioregistro.fase.domain.model.Concurso;
import br.bioregistro.fase.domain.model.EditalResumo;
import br.bioregistro.fase.domain.model.Instituicao;
import br.bioregistro.fase.domain.port.ConcursoRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Um banco por instituição, como no serviço real. */
public class ConcursosEmMemoria implements ConcursoRepository {

    private final Map<Instituicao, Map<Integer, Concurso>> concursos = new HashMap<>();
    private final Map<Instituicao, Map<Integer, Integer>> concursoDoEdital = new HashMap<>();
    private final Map<Instituicao, Map<Integer, String>> editais = new HashMap<>();
    private int sequencia = 1;

    public ConcursosEmMemoria comEdital(Instituicao instituicao, int editalId, String nome) {
        editais.computeIfAbsent(instituicao, i -> new LinkedHashMap<>()).put(editalId, nome);
        return this;
    }

    @Override
    public List<Concurso> listar(Instituicao instituicao) {
        return List.copyOf(banco(instituicao).values());
    }

    @Override
    public Optional<Concurso> buscar(Instituicao instituicao, int id) {
        return Optional.ofNullable(banco(instituicao).get(id));
    }

    @Override
    public boolean nomeEmUso(Instituicao instituicao, String nome, Integer ignorar) {
        return banco(instituicao).values().stream().anyMatch(c -> c.nome().equals(nome) && !c.id().equals(ignorar));
    }

    @Override
    public Concurso salvar(Instituicao instituicao, Concurso concurso) {
        Concurso salvo = concurso.id() == null ? concurso.comId(sequencia++) : concurso;
        banco(instituicao).put(salvo.id(), salvo);
        return salvo;
    }

    @Override
    public void excluir(Instituicao instituicao, int id) {
        vinculos(instituicao).values().removeIf(c -> c == id);
        banco(instituicao).remove(id);
    }

    @Override
    public List<EditalResumo> editais(Instituicao instituicao, int concursoId) {
        List<EditalResumo> lista = new ArrayList<>();
        vinculos(instituicao).forEach((edital, concurso) -> {
            if (concurso == concursoId) {
                lista.add(new EditalResumo(edital, editais.getOrDefault(instituicao, Map.of()).get(edital)));
            }
        });
        return lista;
    }

    @Override
    public Optional<Concurso> concursoDoEdital(Instituicao instituicao, int editalId) {
        return Optional.ofNullable(vinculos(instituicao).get(editalId)).map(id -> banco(instituicao).get(id));
    }

    @Override
    public void incluirEdital(Instituicao instituicao, int concursoId, int editalId) {
        vinculos(instituicao).put(editalId, concursoId);
    }

    @Override
    public void retirarEdital(Instituicao instituicao, int concursoId, int editalId) {
        vinculos(instituicao).remove(editalId, concursoId);
    }

    @Override
    public List<EditalResumo> editaisSemConcurso(Instituicao instituicao) {
        return editais.getOrDefault(instituicao, Map.of()).entrySet().stream()
                .filter(e -> !vinculos(instituicao).containsKey(e.getKey()))
                .map(e -> new EditalResumo(e.getKey(), e.getValue()))
                .toList();
    }

    private Map<Integer, Concurso> banco(Instituicao instituicao) {
        return concursos.computeIfAbsent(instituicao, i -> new LinkedHashMap<>());
    }

    private Map<Integer, Integer> vinculos(Instituicao instituicao) {
        return concursoDoEdital.computeIfAbsent(instituicao, i -> new LinkedHashMap<>());
    }
}

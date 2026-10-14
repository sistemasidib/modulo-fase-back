package br.bioregistro.fase.application.use_cases;

import br.bioregistro.fase.domain.model.EditalResumo;
import br.bioregistro.fase.domain.model.Entidade;
import br.bioregistro.fase.domain.model.Instituicao;
import br.bioregistro.fase.domain.port.EntidadeRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Um banco por instituição, como no serviço real. */
public class EntidadesEmMemoria implements EntidadeRepository {

    private final Map<Instituicao, Map<Integer, Entidade>> entidades = new HashMap<>();
    private final Map<Instituicao, Map<Integer, Integer>> entidadeDoEdital = new HashMap<>();
    private final Map<Instituicao, Map<Integer, String>> editais = new HashMap<>();
    private int sequencia = 1;

    public EntidadesEmMemoria comEdital(Instituicao instituicao, int editalId, String nome) {
        editais.computeIfAbsent(instituicao, i -> new LinkedHashMap<>()).put(editalId, nome);
        return this;
    }

    @Override
    public List<Entidade> listar(Instituicao instituicao) {
        return List.copyOf(banco(instituicao).values());
    }

    @Override
    public Optional<Entidade> buscar(Instituicao instituicao, int id) {
        return Optional.ofNullable(banco(instituicao).get(id));
    }

    @Override
    public boolean nomeEmUso(Instituicao instituicao, String nome, Integer ignorar) {
        return banco(instituicao).values().stream().anyMatch(e -> e.nome().equals(nome) && !e.id().equals(ignorar));
    }

    @Override
    public boolean cnpjEmUso(Instituicao instituicao, String cnpj, Integer ignorar) {
        return banco(instituicao).values().stream().anyMatch(e -> cnpj.equals(e.cnpj()) && !e.id().equals(ignorar));
    }

    @Override
    public Entidade salvar(Instituicao instituicao, Entidade entidade) {
        Entidade salva = entidade.id() == null ? entidade.comId(sequencia++) : entidade;
        banco(instituicao).put(salva.id(), salva);
        return salva;
    }

    @Override
    public void excluir(Instituicao instituicao, int id) {
        vinculos(instituicao).values().removeIf(e -> e == id);
        banco(instituicao).remove(id);
    }

    @Override
    public List<EditalResumo> editais(Instituicao instituicao, int entidadeId) {
        List<EditalResumo> lista = new ArrayList<>();
        vinculos(instituicao).forEach((edital, entidade) -> {
            if (entidade == entidadeId) {
                lista.add(new EditalResumo(edital, editais.getOrDefault(instituicao, Map.of()).get(edital)));
            }
        });
        return lista;
    }

    @Override
    public Optional<Entidade> entidadeDoEdital(Instituicao instituicao, int editalId) {
        return Optional.ofNullable(vinculos(instituicao).get(editalId)).map(id -> banco(instituicao).get(id));
    }

    @Override
    public void incluirEdital(Instituicao instituicao, int entidadeId, int editalId) {
        vinculos(instituicao).put(editalId, entidadeId);
    }

    @Override
    public void retirarEdital(Instituicao instituicao, int entidadeId, int editalId) {
        vinculos(instituicao).remove(editalId, entidadeId);
    }

    @Override
    public List<EditalResumo> editaisSemEntidade(Instituicao instituicao) {
        return editais.getOrDefault(instituicao, Map.of()).entrySet().stream()
                .filter(e -> !vinculos(instituicao).containsKey(e.getKey()))
                .map(e -> new EditalResumo(e.getKey(), e.getValue()))
                .toList();
    }

    private Map<Integer, Entidade> banco(Instituicao instituicao) {
        return entidades.computeIfAbsent(instituicao, i -> new LinkedHashMap<>());
    }

    private Map<Integer, Integer> vinculos(Instituicao instituicao) {
        return entidadeDoEdital.computeIfAbsent(instituicao, i -> new LinkedHashMap<>());
    }
}

package br.bioregistro.fase.application.use_cases;

import br.bioregistro.fase.domain.model.Etapa;
import br.bioregistro.fase.domain.model.Instituicao;
import br.bioregistro.fase.domain.port.EtapaRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Repositório falso para testar os use cases sem banco. */
class EtapasEmMemoria implements EtapaRepository {

    final Map<Instituicao, Map<Integer, Etapa>> etapas = new HashMap<>();
    private int proximoId = 1;

    private Map<Integer, Etapa> de(Instituicao instituicao) {
        return etapas.computeIfAbsent(instituicao, i -> new HashMap<>());
    }

    @Override
    public Optional<Etapa> buscar(Instituicao instituicao, int id) {
        return Optional.ofNullable(de(instituicao).get(id));
    }

    @Override
    public List<Etapa> listarPorFase(Instituicao instituicao, int faseId) {
        return de(instituicao).values().stream().filter(e -> e.faseId() == faseId).toList();
    }

    @Override
    public Etapa salvar(Instituicao instituicao, Etapa etapa) {
        Etapa salva = etapa.id() == null ? etapa.comId(proximoId++) : etapa;
        de(instituicao).put(salva.id(), salva);
        return salva;
    }

    @Override
    public void excluir(Instituicao instituicao, int id) {
        de(instituicao).remove(id);
    }
}

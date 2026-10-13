package br.bioregistro.fase.application.use_cases;

import br.bioregistro.fase.domain.model.CriterioCorrecao;
import br.bioregistro.fase.domain.model.Instituicao;
import br.bioregistro.fase.domain.port.CriterioCorrecaoRepository;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Critérios de correção falsos. Usa uma única instituição por teste, para simplificar. */
public class CriteriosCorrecaoEmMemoria implements CriterioCorrecaoRepository {

    private final Map<Integer, CriterioCorrecao> criterios = new LinkedHashMap<>();
    private int proximoId = 1;

    @Override
    public List<CriterioCorrecao> daEtapa(Instituicao instituicao, int etapaId) {
        return criterios.values().stream()
                .filter(c -> c.etapaId() == etapaId)
                .sorted(Comparator.comparingInt(CriterioCorrecao::ordem).thenComparing(CriterioCorrecao::id))
                .toList();
    }

    @Override
    public Optional<CriterioCorrecao> buscar(Instituicao instituicao, int id) {
        return Optional.ofNullable(criterios.get(id));
    }

    @Override
    public CriterioCorrecao salvar(Instituicao instituicao, CriterioCorrecao criterio) {
        CriterioCorrecao salvo = criterio.id() == null ? criterio.comId(proximoId++) : criterio;
        criterios.put(salvo.id(), salvo);
        return salvo;
    }

    @Override
    public void excluir(Instituicao instituicao, int id) {
        criterios.remove(id);
    }

    @Override
    public void excluirDaEtapa(Instituicao instituicao, int etapaId) {
        criterios.values().removeIf(c -> c.etapaId() == etapaId);
    }
}

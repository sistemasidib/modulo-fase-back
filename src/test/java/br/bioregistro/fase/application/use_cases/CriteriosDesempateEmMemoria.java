package br.bioregistro.fase.application.use_cases;

import br.bioregistro.fase.domain.model.CriterioDesempate;
import br.bioregistro.fase.domain.model.Instituicao;
import br.bioregistro.fase.domain.port.CriterioDesempateRepository;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Critérios falsos. Usa uma única instituição por teste, para simplificar. */
public class CriteriosDesempateEmMemoria implements CriterioDesempateRepository {

    private final Map<Integer, CriterioDesempate> criterios = new LinkedHashMap<>();
    private int proximoId = 1;

    @Override
    public List<CriterioDesempate> daFase(Instituicao instituicao, int faseId) {
        return criterios.values().stream()
                .filter(c -> c.faseId() == faseId)
                .sorted(Comparator.comparingInt(CriterioDesempate::ordem).thenComparing(CriterioDesempate::id))
                .toList();
    }

    @Override
    public Optional<CriterioDesempate> buscar(Instituicao instituicao, int id) {
        return Optional.ofNullable(criterios.get(id));
    }

    @Override
    public CriterioDesempate salvar(Instituicao instituicao, CriterioDesempate criterio) {
        CriterioDesempate salvo = criterio.id() == null ? criterio.comId(proximoId++) : criterio;
        criterios.put(salvo.id(), salvo);
        return salvo;
    }

    @Override
    public void excluir(Instituicao instituicao, int id) {
        criterios.remove(id);
    }

    @Override
    public void excluirDaFase(Instituicao instituicao, int faseId) {
        criterios.values().removeIf(c -> c.faseId() == faseId);
    }
}

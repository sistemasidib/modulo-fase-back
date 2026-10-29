package br.bioregistro.fase.application.use_cases;

import br.bioregistro.fase.domain.model.CriterioDesempate;
import br.bioregistro.fase.domain.model.Etapa;
import br.bioregistro.fase.domain.model.Instituicao;
import br.bioregistro.fase.domain.port.CriterioDesempateRepository;
import br.bioregistro.fase.domain.port.EtapaRepository;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/** Critérios falsos. Usa uma única instituição por teste, para simplificar. */
public class CriteriosDesempateEmMemoria implements CriterioDesempateRepository {

    private final Map<Integer, CriterioDesempate> criterios = new LinkedHashMap<>();
    private int proximoId = 1;
    private EtapaRepository etapas;

    /** Opcional: necessário para {@link #daFase} / {@link #excluirDaFase} nos testes. */
    public void comEtapas(EtapaRepository etapas) {
        this.etapas = etapas;
    }

    @Override
    public List<CriterioDesempate> daEtapa(Instituicao instituicao, int etapaId) {
        return criterios.values().stream()
                .filter(c -> c.etapaId() == etapaId)
                .sorted(Comparator.comparingInt(CriterioDesempate::ordem).thenComparing(CriterioDesempate::id))
                .toList();
    }

    @Override
    public List<CriterioDesempate> daFase(Instituicao instituicao, int faseId) {
        Set<Integer> ids = idsDasEtapas(instituicao, faseId);
        return criterios.values().stream()
                .filter(c -> ids.contains(c.etapaId()))
                .sorted(Comparator.comparingInt(CriterioDesempate::etapaId)
                        .thenComparingInt(CriterioDesempate::ordem)
                        .thenComparing(CriterioDesempate::id))
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
    public void excluirDaEtapa(Instituicao instituicao, int etapaId) {
        criterios.values().removeIf(c -> c.etapaId() == etapaId);
    }

    @Override
    public void excluirDaFase(Instituicao instituicao, int faseId) {
        Set<Integer> ids = idsDasEtapas(instituicao, faseId);
        criterios.values().removeIf(c -> ids.contains(c.etapaId()));
    }

    private Set<Integer> idsDasEtapas(Instituicao instituicao, int faseId) {
        if (etapas == null) {
            return Set.of();
        }
        return etapas.listarPorFase(instituicao, faseId).stream()
                .map(Etapa::id)
                .collect(Collectors.toSet());
    }
}

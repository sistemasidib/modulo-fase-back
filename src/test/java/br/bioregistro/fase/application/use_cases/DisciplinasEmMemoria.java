package br.bioregistro.fase.application.use_cases;

import br.bioregistro.fase.domain.model.DisciplinaDaProva;
import br.bioregistro.fase.domain.model.Instituicao;
import br.bioregistro.fase.domain.port.DisciplinaRepository;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Faixas de questões falsas. Usa uma única instituição por teste, para simplificar. */
public class DisciplinasEmMemoria implements DisciplinaRepository {

    private final Map<Integer, DisciplinaDaProva> faixas = new LinkedHashMap<>();
    private final Map<Integer, Integer> editalDaFaixa = new LinkedHashMap<>();
    /** "edital:nome em minúsculas" → disciplinaEditalId */
    private final Map<String, Integer> disciplinasDoEdital = new LinkedHashMap<>();
    private final Map<Integer, String> nomeDaDisciplina = new LinkedHashMap<>();
    private int proximaFaixa = 1000;
    private int proximaDisciplina = 1000;

    /** Faixa antiga, sem etapa. Uma disciplina por faixa ({@code disciplinaEditalId = faixaId}). */
    public DisciplinasEmMemoria com(int editalId, int faixaId, String nome, int inicio, int fim, Integer cargoId) {
        return comEtapa(editalId, faixaId, nome, inicio, fim, cargoId, null);
    }

    public DisciplinasEmMemoria comEtapa(int editalId, int faixaId, String nome, int inicio, int fim, Integer cargoId,
                                         Integer etapaId) {
        faixas.put(faixaId, new DisciplinaDaProva(faixaId, faixaId, faixaId, nome, inicio, fim, cargoId,
                cargoId == null ? null : "Cargo " + cargoId, etapaId, null));
        editalDaFaixa.put(faixaId, editalId);
        return this;
    }

    @Override
    public List<DisciplinaDaProva> daEtapa(Instituicao instituicao, int editalId, int etapaId, boolean incluirSemEtapa) {
        return faixas.values().stream()
                .filter(f -> editalDaFaixa.get(f.faixaId()) == editalId)
                .filter(f -> Objects.equals(f.etapaId(), etapaId) || (incluirSemEtapa && f.etapaId() == null))
                .sorted(Comparator.comparingInt(DisciplinaDaProva::questaoInicio))
                .toList();
    }

    @Override
    public Optional<DisciplinaDaProva> buscar(Instituicao instituicao, int faixaId) {
        return Optional.ofNullable(faixas.get(faixaId));
    }

    @Override
    public void definirPeso(Instituicao instituicao, int faixaId, BigDecimal peso) {
        faixas.computeIfPresent(faixaId, (id, f) -> f.comPeso(peso));
    }

    @Override
    public int disciplinaDoEdital(Instituicao instituicao, int editalId, String nome) {
        int id = disciplinasDoEdital.computeIfAbsent(editalId + ":" + nome.toLowerCase(), k -> proximaDisciplina++);
        nomeDaDisciplina.putIfAbsent(id, nome);
        return id;
    }

    @Override
    public int inserirFaixa(Instituicao instituicao, int disciplinaEditalId, int etapaId, int inicio, int fim, int cargoId) {
        String nome = Objects.requireNonNull(nomeDaDisciplina.get(disciplinaEditalId));
        int editalId = Integer.parseInt(disciplinasDoEdital.entrySet().stream()
                .filter(e -> e.getValue() == disciplinaEditalId)
                .map(e -> e.getKey().substring(0, e.getKey().indexOf(':')))
                .findFirst().orElseThrow());
        int id = proximaFaixa++;
        faixas.put(id, new DisciplinaDaProva(id, disciplinaEditalId, disciplinaEditalId, nome, inicio, fim, cargoId,
                "Cargo " + cargoId, etapaId, null));
        editalDaFaixa.put(id, editalId);
        return id;
    }

    @Override
    public void removerFaixa(Instituicao instituicao, int faixaId) {
        faixas.remove(faixaId);
    }

    @Override
    public void removerDaEtapa(Instituicao instituicao, int etapaId) {
        faixas.values().removeIf(f -> Objects.equals(f.etapaId(), etapaId));
    }
}

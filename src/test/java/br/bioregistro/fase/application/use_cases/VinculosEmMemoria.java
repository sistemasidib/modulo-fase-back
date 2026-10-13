package br.bioregistro.fase.application.use_cases;

import br.bioregistro.fase.domain.model.Instituicao;
import br.bioregistro.fase.domain.model.cronograma.LinhaDoCronograma;
import br.bioregistro.fase.domain.model.cronograma.ModuloDeExecucao;
import br.bioregistro.fase.domain.port.VinculoCronogramaRepository;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** {@code fase_cronograma} e as FKs da publicação, lendo as linhas do {@link CronogramasEmMemoria}. */
public class VinculosEmMemoria implements VinculoCronogramaRepository {

    record DaFase(int editalDoCronogramaId, ModuloDeExecucao modulo) {
    }

    final Map<Integer, DaFase> fases = new HashMap<>();
    /** publicação → {linha, recurso (ou nulo)} */
    final Map<Integer, Integer[]> publicacoes = new HashMap<>();
    private final CronogramasEmMemoria cronogramas;
    private final PublicacoesEmMemoria espacos;

    public VinculosEmMemoria(CronogramasEmMemoria cronogramas, PublicacoesEmMemoria espacos) {
        this.cronogramas = cronogramas;
        this.espacos = espacos;
    }

    @Override
    public Optional<Integer> faseDoModulo(Instituicao instituicao, int editalDoCronogramaId, ModuloDeExecucao modulo) {
        return fases.entrySet().stream()
                .filter(e -> e.getValue().equals(new DaFase(editalDoCronogramaId, modulo)))
                .map(Map.Entry::getKey)
                .findFirst();
    }

    @Override
    public Optional<ModuloDeExecucao> moduloDaFase(Instituicao instituicao, int faseId) {
        return Optional.ofNullable(fases.get(faseId)).map(DaFase::modulo);
    }

    @Override
    public void vincularFase(Instituicao instituicao, int faseId, int editalDoCronogramaId, ModuloDeExecucao modulo) {
        fases.put(faseId, new DaFase(editalDoCronogramaId, modulo));
    }

    @Override
    public void desvincularFase(Instituicao instituicao, int faseId) {
        fases.remove(faseId);
    }

    @Override
    public void vincularPublicacao(Instituicao instituicao, int publicacaoId, int linhaId, Integer recursoLinhaId) {
        publicacoes.put(publicacaoId, new Integer[]{linhaId, recursoLinhaId});
    }

    @Override
    public Map<Integer, LinhaDoCronograma.DaPublicacao> publicacoesDaFase(Instituicao instituicao, int faseId) {
        Map<Integer, LinhaDoCronograma.DaPublicacao> vinculadas = new HashMap<>();
        espacos.listarPorFase(instituicao, faseId)
                .forEach(p -> daPublicacao(instituicao, p.id()).ifPresent(v -> vinculadas.put(p.id(), v)));
        return vinculadas;
    }

    @Override
    public Optional<LinhaDoCronograma.DaPublicacao> daPublicacao(Instituicao instituicao, int publicacaoId) {
        Integer[] ids = publicacoes.get(publicacaoId);
        if (ids == null) {
            return Optional.empty();
        }
        LinhaDoCronograma recurso = ids[1] == null ? null : cronogramas.linhas.get(ids[1]).valor();
        return Optional.of(new LinhaDoCronograma.DaPublicacao(cronogramas.linhas.get(ids[0]).valor(), recurso));
    }
}

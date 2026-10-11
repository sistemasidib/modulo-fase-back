package br.bioregistro.fase.application.use_cases;

import br.bioregistro.fase.domain.exception.ConflitoException;
import br.bioregistro.fase.domain.model.Instituicao;
import br.bioregistro.fase.domain.model.cronograma.Acao;
import br.bioregistro.fase.domain.model.cronograma.Cronograma;
import br.bioregistro.fase.domain.model.cronograma.CronogramaCalculado;
import br.bioregistro.fase.domain.model.cronograma.Parametros;
import br.bioregistro.fase.domain.model.cronograma.Particularidade;
import br.bioregistro.fase.domain.model.cronograma.StatusEtapa;
import br.bioregistro.fase.domain.port.CronogramaRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Só IDECAN importa nestes testes; a instituição é ignorada. */
public class CronogramasEmMemoria implements CronogramaRepository {

    public Parametros padrao = Parametros.padroes();
    final Map<Integer, Cronograma> salvos = new HashMap<>();
    final Map<Integer, List<Acao>> acoes = new HashMap<>();
    final Map<Integer, List<Particularidade>> particularidades = new HashMap<>();
    private int sequencia = 1;

    @Override
    public Parametros parametrosPadrao(Instituicao instituicao) {
        return padrao;
    }

    @Override
    public Optional<Integer> idDoConcurso(Instituicao instituicao, int concursoId) {
        return salvos.values().stream().filter(c -> c.concursoId() == concursoId).map(Cronograma::id).findFirst();
    }

    @Override
    public int criar(Instituicao instituicao, Cronograma c, CronogramaCalculado calculado) {
        int id = sequencia++;
        salvos.put(id, new Cronograma(id, c.concursoId(), c.esqueleto(), c.parametros(), c.edicao(), c.status(),
                c.justificativa(), 0));
        return id;
    }

    @Override
    public Optional<Cronograma> buscar(Instituicao instituicao, int id) {
        return Optional.ofNullable(salvos.get(id));
    }

    @Override
    public void salvar(Instituicao instituicao, Cronograma c, CronogramaCalculado calculado) {
        if (salvos.get(c.id()).versao() != c.versao()) {
            throw new ConflitoException("versão desatualizada");
        }
        salvos.put(c.id(), new Cronograma(c.id(), c.concursoId(), c.esqueleto(), c.parametros(), c.edicao(),
                salvos.get(c.id()).status(), c.justificativa(), c.versao() + 1));
    }

    @Override
    public void mudarStatus(Instituicao instituicao, int id, String codigo, StatusEtapa status) {
        Cronograma c = salvos.get(id);
        Map<String, StatusEtapa> novo = new HashMap<>(c.status());
        novo.put(codigo, status);
        salvos.put(id, new Cronograma(id, c.concursoId(), c.esqueleto(), c.parametros(), c.edicao(), novo,
                c.justificativa(), c.versao()));
    }

    @Override
    public void registrar(Instituicao instituicao, int cronogramaId, Acao acao) {
        acoes.computeIfAbsent(cronogramaId, k -> new ArrayList<>()).add(0, acao);
    }

    @Override
    public List<Acao> historico(Instituicao instituicao, int cronogramaId) {
        return List.copyOf(acoes.getOrDefault(cronogramaId, List.of()));
    }

    @Override
    public List<Particularidade> particularidades(Instituicao instituicao, int cronogramaId) {
        return List.copyOf(particularidades.getOrDefault(cronogramaId, List.of()));
    }

    @Override
    public Particularidade adicionarParticularidade(Instituicao instituicao, int cronogramaId, Particularidade p) {
        List<Particularidade> lista = particularidades.computeIfAbsent(cronogramaId, k -> new ArrayList<>());
        Particularidade salva = new Particularidade(lista.size() + 1, p.texto(), p.assunto(), p.etapaCodigo(),
                p.autor(), null);
        lista.add(salva);
        return salva;
    }

    @Override
    public Optional<Particularidade> particularidade(Instituicao instituicao, int cronogramaId, int id) {
        return particularidades.getOrDefault(cronogramaId, List.of()).stream().filter(p -> p.id() == id).findFirst();
    }

    @Override
    public void removerParticularidade(Instituicao instituicao, int cronogramaId, int id) {
        particularidades.getOrDefault(cronogramaId, new ArrayList<>()).removeIf(p -> p.id() == id);
    }
}

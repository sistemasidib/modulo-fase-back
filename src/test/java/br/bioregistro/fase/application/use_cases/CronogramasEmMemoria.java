package br.bioregistro.fase.application.use_cases;

import br.bioregistro.fase.domain.exception.ConflitoException;
import br.bioregistro.fase.domain.model.Instituicao;
import br.bioregistro.fase.domain.model.cronograma.Acao;
import br.bioregistro.fase.domain.model.cronograma.Cronograma;
import br.bioregistro.fase.domain.model.cronograma.CronogramaCalculado;
import br.bioregistro.fase.domain.model.cronograma.EditalDoCronograma;
import br.bioregistro.fase.domain.model.cronograma.EtapaCalculada;
import br.bioregistro.fase.domain.model.cronograma.LinhaDoCronograma;
import br.bioregistro.fase.domain.model.cronograma.Parametros;
import br.bioregistro.fase.domain.model.cronograma.Particularidade;
import br.bioregistro.fase.domain.model.cronograma.Sobreposicao;
import br.bioregistro.fase.domain.model.cronograma.StatusEtapa;
import br.bioregistro.fase.domain.port.CronogramaRepository;
import br.bioregistro.fase.domain.port.EntidadeRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/** Só IDECAN importa nestes testes; a instituição é ignorada. */
public class CronogramasEmMemoria implements CronogramaRepository {

    /** Linha de leitura de um edital, como em {@code cronograma_edital_etapa}. */
    record Linha(int editalDoCronogramaId, LinhaDoCronograma valor) {
    }

    public Parametros padrao = Parametros.padroes();
    final Map<Integer, Cronograma> salvos = new HashMap<>();
    /** editalId → vínculo. */
    final Map<Integer, EditalDoCronograma> editais = new TreeMap<>();
    final Map<Integer, Linha> linhas = new HashMap<>();
    final Map<Integer, List<Acao>> acoes = new HashMap<>();
    final Map<Integer, List<Particularidade>> particularidades = new HashMap<>();
    private final EntidadeRepository entidades;
    private int sequencia = 1;
    private int sequenciaEdital = 1;
    private int sequenciaLinha = 1;

    public CronogramasEmMemoria() {
        this(null);
    }

    /** @param entidades para saber os editais de cada entidade ({@link #cronogramasDosEditais}) */
    public CronogramasEmMemoria(EntidadeRepository entidades) {
        this.entidades = entidades;
    }

    @Override
    public Parametros parametrosPadrao(Instituicao instituicao) {
        return padrao;
    }

    @Override
    public Map<Integer, Integer> cronogramasDosEditais(Instituicao instituicao, int entidadeId) {
        Map<Integer, Integer> ids = new LinkedHashMap<>();
        entidades.editais(instituicao, entidadeId).forEach(e -> {
            EditalDoCronograma vinculo = editais.get(e.id());
            if (vinculo != null) {
                ids.put(e.id(), vinculo.cronogramaId());
            }
        });
        return ids;
    }

    @Override
    public int criar(Instituicao instituicao, Cronograma c, CronogramaCalculado calculado) {
        int id = sequencia++;
        salvos.put(id, new Cronograma(id, c.esqueleto(), c.parametros(), c.edicao(), c.status(),
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
        salvos.put(c.id(), new Cronograma(c.id(), c.esqueleto(), c.parametros(), c.edicao(),
                salvos.get(c.id()).status(), c.justificativa(), c.versao() + 1));
    }

    @Override
    public void mudarStatus(Instituicao instituicao, int id, String codigo, StatusEtapa status) {
        Cronograma c = salvos.get(id);
        Map<String, StatusEtapa> novo = new HashMap<>(c.status());
        novo.put(codigo, status);
        salvos.put(id, new Cronograma(id, c.esqueleto(), c.parametros(), c.edicao(), novo,
                c.justificativa(), c.versao()));
    }

    @Override
    public Optional<EditalDoCronograma> doEdital(Instituicao instituicao, int editalId) {
        return Optional.ofNullable(editais.get(editalId));
    }

    @Override
    public List<EditalDoCronograma> editais(Instituicao instituicao, int cronogramaId) {
        return editais.values().stream().filter(e -> e.cronogramaId() == cronogramaId).toList();
    }

    @Override
    public EditalDoCronograma ligarEdital(Instituicao instituicao, int cronogramaId, int editalId) {
        EditalDoCronograma atual = editais.get(editalId);
        EditalDoCronograma ligado = atual == null
                ? new EditalDoCronograma(sequenciaEdital++, cronogramaId, editalId, Sobreposicao.vazia(), 0)
                : new EditalDoCronograma(atual.id(), cronogramaId, editalId, Sobreposicao.vazia(), atual.versao() + 1);
        editais.put(editalId, ligado);
        return ligado;
    }

    @Override
    public void salvarSobreposicao(Instituicao instituicao, EditalDoCronograma edital) {
        EditalDoCronograma atual = editais.get(edital.editalId());
        if (atual.versao() != edital.versao()) {
            throw new ConflitoException("versão desatualizada");
        }
        editais.put(edital.editalId(), new EditalDoCronograma(edital.id(), edital.cronogramaId(), edital.editalId(),
                edital.sobreposicao(), edital.versao() + 1));
    }

    @Override
    public void materializar(Instituicao instituicao, EditalDoCronograma edital, CronogramaCalculado calculado,
                             Map<String, StatusEtapa> status) {
        Map<String, Integer> atuais = linhasDoEdital(instituicao, edital.id());
        List<String> codigos = new ArrayList<>();
        for (EtapaCalculada etapa : calculado.etapas()) {
            codigos.add(etapa.codigo());
            int id = atuais.containsKey(etapa.codigo()) ? atuais.get(etapa.codigo()) : sequenciaLinha++;
            linhas.put(id, new Linha(edital.id(), new LinhaDoCronograma(etapa.codigo(), etapa.ativa(), etapa.inicio(),
                    etapa.fim(), status.getOrDefault(etapa.codigo(), StatusEtapa.A_FAZER))));
        }
        atuais.forEach((codigo, id) -> {
            if (!codigos.contains(codigo)) {
                linhas.remove(id);
            }
        });
    }

    @Override
    public Map<String, Integer> linhasDoEdital(Instituicao instituicao, int editalDoCronogramaId) {
        Map<String, Integer> doEdital = new HashMap<>();
        linhas.forEach((id, l) -> {
            if (l.editalDoCronogramaId() == editalDoCronogramaId) {
                doEdital.put(l.valor().codigo(), id);
            }
        });
        return doEdital;
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

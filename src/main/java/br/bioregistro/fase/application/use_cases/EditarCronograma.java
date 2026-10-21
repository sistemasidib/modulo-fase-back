package br.bioregistro.fase.application.use_cases;

import br.bioregistro.fase.application.dtos.CronogramaDtos.AbrirCronogramaResponse;
import br.bioregistro.fase.application.dtos.CronogramaDtos.AcaoRequest;
import br.bioregistro.fase.application.dtos.CronogramaDtos.CronogramaResponse;
import br.bioregistro.fase.application.dtos.CronogramaDtos.ParticularidadeRequest;
import br.bioregistro.fase.application.dtos.CronogramaDtos.SalvarCronogramaRequest;
import br.bioregistro.fase.application.use_cases.ConsultarCronograma.Alvo;
import br.bioregistro.fase.domain.exception.ConflitoException;
import br.bioregistro.fase.domain.exception.NaoEncontradoException;
import br.bioregistro.fase.domain.exception.RegraDeNegocioException;
import br.bioregistro.fase.domain.model.Instituicao;
import br.bioregistro.fase.domain.model.cronograma.Acao;
import br.bioregistro.fase.domain.model.cronograma.Cronograma;
import br.bioregistro.fase.domain.model.cronograma.CronogramaCalculado;
import br.bioregistro.fase.domain.model.cronograma.Edicao;
import br.bioregistro.fase.domain.model.cronograma.EditalDoCronograma;
import br.bioregistro.fase.domain.model.cronograma.Esqueleto;
import br.bioregistro.fase.domain.model.cronograma.Esqueletos;
import br.bioregistro.fase.domain.model.cronograma.Particularidade;
import br.bioregistro.fase.domain.model.cronograma.Previa;
import br.bioregistro.fase.domain.model.cronograma.Sobreposicao;
import br.bioregistro.fase.domain.model.cronograma.StatusEtapa;
import br.bioregistro.fase.domain.port.CronogramaRepository;
import br.bioregistro.fase.domain.port.FaseRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.Map;

/**
 * Escrita do cronograma (RB12–RB19). Toda escrita gera ação no histórico, que é só de inclusão.
 * A edição de etapas acontece em sessão no front; {@link #salvar} recebe o estado completo e
 * recalcula tudo, sem confiar em datas vindas de fora.
 *
 * <p>Um cronograma é seguido por um ou mais editais (spec 005). Sem {@code editalId}, a escrita
 * vale para todos; com {@code editalId}, vira sobreposição só daquele edital. Spec 006 RN09c: a
 * escrita <strong>não</strong> dispara a {@link PonteCronograma}.
 */
@ApplicationScoped
public class EditarCronograma {

    private final CronogramaRepository cronogramas;
    private final FaseRepository fases;
    private final ConsultarCronograma consulta;

    public EditarCronograma(CronogramaRepository cronogramas, FaseRepository fases,
                            ConsultarCronograma consulta, PonteCronograma ponte) {
        this.cronogramas = cronogramas;
        this.fases = fases;
        this.consulta = consulta;
        // ponte mantida no construtor por compatibilidade CDI; RN09c não a usa na escrita
    }

    /**
     * RB12: o cronograma nasce do edital. Se o edital já segue um, devolve esse; senão, gera um novo
     * a partir do esqueleto e o edital passa a segui-lo. Outros editais do mesmo concurso o seguem
     * com {@link #seguir}.
     */
    @Transactional
    public AbrirCronogramaResponse abrir(Instituicao instituicao, int editalId, String autor) {
        if (!fases.editalExiste(instituicao, editalId)) {
            throw new NaoEncontradoException("Edital", editalId);
        }
        var segue = cronogramas.doEdital(instituicao, editalId);
        if (segue.isPresent()) {
            return new AbrirCronogramaResponse(segue.get().cronogramaId(), false);
        }
        int id = gerar(instituicao, autor);
        ligar(instituicao, id, editalId, autor, "Edital " + editalId + " passou a seguir o cronograma");
        return new AbrirCronogramaResponse(id, true);
    }

    /**
     * Replicar = passar a seguir o cronograma de outro edital (spec 005, RN14). A sobreposição que o
     * edital tinha é descartada; as linhas de leitura (e as publicações que apontam para elas) ficam.
     */
    @Transactional
    public AbrirCronogramaResponse seguir(Instituicao instituicao, int editalId, Integer cronogramaId, String autor) {
        if (cronogramaId == null) {
            throw new RegraDeNegocioException("Informe o cronograma que o edital vai seguir.");
        }
        ConsultarCronograma.exigir(cronogramas, instituicao, cronogramaId);
        if (!fases.editalExiste(instituicao, editalId)) {
            throw new NaoEncontradoException("Edital", editalId);
        }
        var atual = cronogramas.doEdital(instituicao, editalId);
        if (atual.isPresent() && atual.get().cronogramaId() == cronogramaId) {
            return new AbrirCronogramaResponse(cronogramaId, false);
        }
        atual.ifPresent(a -> cronogramas.registrar(instituicao, a.cronogramaId(), Acao.de(autor,
                "Edital " + editalId + " deixou de seguir este cronograma",
                "passou a seguir o cronograma " + cronogramaId, Acao.EDICAO, List.of()).doEdital(editalId)));
        ligar(instituicao, cronogramaId, editalId, autor, "Edital " + editalId + " passou a seguir o cronograma");
        return new AbrirCronogramaResponse(cronogramaId, true);
    }

    /**
     * O edital ganha cronograma próprio, com a edição e os status que ele tem hoje (compartilhado +
     * sobreposição). Daí em diante, mudanças no compartilhado não chegam a ele.
     */
    @Transactional
    public AbrirCronogramaResponse separar(Instituicao instituicao, int editalId, String autor) {
        EditalDoCronograma edital = cronogramas.doEdital(instituicao, editalId)
                .orElseThrow(() -> new NaoEncontradoException("Cronograma do edital", editalId));
        Cronograma origem = ConsultarCronograma.exigir(cronogramas, instituicao, edital.cronogramaId());
        Cronograma proprio = origem.separado(edital);
        int id = cronogramas.criar(instituicao, proprio, consulta.calcular(proprio, proprio.edicao()));
        cronogramas.particularidades(instituicao, origem.id())
                .forEach(p -> cronogramas.adicionarParticularidade(instituicao, id, p));
        cronogramas.registrar(instituicao, origem.id(), Acao.de(autor, "Edital " + editalId + " se separou",
                "ganhou o cronograma próprio " + id, Acao.EDICAO, List.of()).doEdital(editalId));
        cronogramas.registrar(instituicao, id, Acao.de(autor, "Criou o cronograma próprio do edital " + editalId,
                "cópia do cronograma " + origem.id() + " (versão " + origem.versao() + ") com a sobreposição do edital",
                Acao.GERACAO, List.of()));
        cronogramas.ligarEdital(instituicao, id, editalId);
        return new AbrirCronogramaResponse(id, true);
    }

    /** O edital volta a seguir o compartilhado em tudo: a sobreposição é apagada. */
    @Transactional
    public CronogramaResponse restaurar(Instituicao instituicao, int id, int editalId, String autor) {
        Alvo alvo = consulta.alvo(instituicao, id, editalId);
        if (alvo.edital().sobreposicao().isVazia()) {
            return consulta.buscar(instituicao, id, editalId);
        }
        cronogramas.salvarSobreposicao(instituicao, alvo.edital().com(Sobreposicao.vazia()));
        cronogramas.registrar(instituicao, id, Acao.de(autor, "Restaurou o edital " + editalId,
                "voltou a seguir o cronograma compartilhado em tudo · desfeito: "
                        + String.join(", ", alvo.edital().sobreposicao().codigos()),
                Acao.EDICAO, List.of()).doEdital(editalId));
        return consulta.buscar(instituicao, id, editalId);
    }

    private int gerar(Instituicao instituicao, String autor) {
        Esqueleto esqueleto = Esqueletos.ATUAL;
        Cronograma novo = new Cronograma(null, esqueleto.nome(),
                cronogramas.parametrosPadrao(instituicao), Edicao.inicial(), Map.of(), null, 0);
        CronogramaCalculado calculado = consulta.calcular(novo, novo.edicao());
        int id = cronogramas.criar(instituicao, novo, calculado);
        cronogramas.registrar(instituicao, id, Acao.de(autor, "Gerou o cronograma a partir do esqueleto",
                esqueleto.nome() + " · só o núcleo · " + calculado.indicadores().etapasAtivas() + " etapas",
                Acao.GERACAO, List.of()));
        return id;
    }

    private void ligar(Instituicao instituicao, int cronogramaId, int editalId, String autor, String acao) {
        cronogramas.ligarEdital(instituicao, cronogramaId, editalId);
        cronogramas.registrar(instituicao, cronogramaId,
                Acao.de(autor, acao, null, Acao.EDICAO, List.of()).doEdital(editalId));
    }

    /** @param editalId nulo = salva o compartilhado; preenchido = a sobreposição do edital */
    @Transactional
    public CronogramaResponse salvar(Instituicao instituicao, int id, Integer editalId,
                                     SalvarCronogramaRequest request, String autor) {
        Alvo alvo = consulta.alvo(instituicao, id, editalId);
        Cronograma atual = alvo.visto();
        if (request.versao() == null || request.versao() != atual.versao()) {
            throw new ConflitoException("Outra pessoa salvou este cronograma depois que você o abriu. "
                    + "Recarregue para ver a versão atual.");
        }
        Edicao edicao = ConsultarCronograma.edicaoValida(request.edicao());
        edicao.validar(Esqueletos.porNome(atual.esqueleto()));

        CronogramaCalculado calculado = consulta.calcular(atual, edicao);
        String justificativa = request.justificativa() == null || request.justificativa().isBlank()
                ? null : request.justificativa().strip();
        if (calculado.checagens().viola() && justificativa == null) {
            throw new RegraDeNegocioException(
                    "O cronograma viola uma restrição. Escreva a justificativa para salvar.");
        }

        if (alvo.edital() == null) {
            cronogramas.salvar(instituicao, atual.comEdicao(edicao, justificativa), calculado);
        } else {
            EditalDoCronograma edital = alvo.edital();
            cronogramas.salvarSobreposicao(instituicao, edital.com(Sobreposicao.diferenca(alvo.base().edicao(),
                    edicao, edital.sobreposicao().status())));
        }
        for (AcaoRequest acao : request.acoes() == null ? List.<AcaoRequest>of() : request.acoes()) {
            cronogramas.registrar(instituicao, id,
                    Acao.de(autor, acao.acao(), acao.detalhe(), acao.origem(), acao.itens()).doEdital(editalId));
        }
        cronogramas.registrar(instituicao, id, new Acao(null, autor, null,
                editalId == null ? "Salvou o cronograma" : "Salvou o cronograma do edital " + editalId,
                (calculado.materializado() ? "Materializado" : "Rascunho") + " · "
                        + calculado.indicadores().etapasAtivas() + " etapas ativas"
                        + (calculado.checagens().viola() ? " · viola restrição" : ""),
                Acao.SALVAMENTO, justificativa, List.of(), editalId));
        return consulta.buscar(instituicao, id, editalId);
    }

    @Transactional
    public CronogramaResponse adicionarParticularidade(Instituicao instituicao, int id,
                                                       ParticularidadeRequest request, String autor) {
        Cronograma cronograma = ConsultarCronograma.exigir(cronogramas, instituicao, id);
        Particularidade nova = new Particularidade(null, request.texto(), request.assunto(), request.etapaCodigo(),
                autor, null);
        if (nova.etapaCodigo() != null) {
            Esqueletos.porNome(cronograma.esqueleto()).etapa(nova.etapaCodigo());
        }
        cronogramas.adicionarParticularidade(instituicao, id, nova);
        cronogramas.registrar(instituicao, id, Acao.de(autor, "Adicionou particularidade · " + nova.alvo(),
                nova.assunto() + ": " + nova.texto(), Acao.EDICAO, List.of()));
        return consulta.buscar(instituicao, id);
    }

    @Transactional
    public CronogramaResponse removerParticularidade(Instituicao instituicao, int id, int particularidadeId,
                                                     String autor) {
        ConsultarCronograma.exigir(cronogramas, instituicao, id);
        Particularidade p = cronogramas.particularidade(instituicao, id, particularidadeId)
                .orElseThrow(() -> new NaoEncontradoException("Particularidade", particularidadeId));
        cronogramas.removerParticularidade(instituicao, id, particularidadeId);
        cronogramas.registrar(instituicao, id, Acao.de(autor, "Removeu particularidade · " + p.alvo(),
                p.assunto() + ": " + p.texto(), Acao.EDICAO, List.of()));
        return consulta.buscar(instituicao, id);
    }

    /** Spec 006 RN09b: o cronograma público só seleciona datas — sem funções de publicação. */
    @Transactional
    public CronogramaResponse marcarStatus(Instituicao instituicao, int id, Integer editalId, String codigo,
                                           StatusEtapa novo, String autor) {
        throw new ConflitoException(
                "O cronograma público não marca publicação. Use só a seleção de datas.");
    }

    /** Spec 006 RN09b: gatilho de publicação/recurso não faz parte do cronograma público. */
    @Transactional
    public CronogramaResponse registrarPrimeiroRecurso(Instituicao instituicao, int id, Integer editalId,
                                                       String codigo, String autor) {
        throw new ConflitoException(
                "O cronograma público não registra publicação nem recurso. Use só a seleção de datas.");
    }
}

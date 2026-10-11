package br.bioregistro.fase.application.use_cases;

import br.bioregistro.fase.application.dtos.CronogramaDtos.AbrirCronogramaResponse;
import br.bioregistro.fase.application.dtos.CronogramaDtos.AcaoRequest;
import br.bioregistro.fase.application.dtos.CronogramaDtos.CronogramaResponse;
import br.bioregistro.fase.application.dtos.CronogramaDtos.ParticularidadeRequest;
import br.bioregistro.fase.application.dtos.CronogramaDtos.SalvarCronogramaRequest;
import br.bioregistro.fase.domain.exception.ConflitoException;
import br.bioregistro.fase.domain.exception.NaoEncontradoException;
import br.bioregistro.fase.domain.exception.RegraDeNegocioException;
import br.bioregistro.fase.domain.model.Instituicao;
import br.bioregistro.fase.domain.model.cronograma.Acao;
import br.bioregistro.fase.domain.model.cronograma.Cronograma;
import br.bioregistro.fase.domain.model.cronograma.CronogramaCalculado;
import br.bioregistro.fase.domain.model.cronograma.Edicao;
import br.bioregistro.fase.domain.model.cronograma.EtapaCalculada;
import br.bioregistro.fase.domain.model.cronograma.Esqueleto;
import br.bioregistro.fase.domain.model.cronograma.Esqueletos;
import br.bioregistro.fase.domain.model.cronograma.Particularidade;
import br.bioregistro.fase.domain.model.cronograma.Previa;
import br.bioregistro.fase.domain.model.cronograma.StatusEtapa;
import br.bioregistro.fase.domain.port.ConcursoRepository;
import br.bioregistro.fase.domain.port.CronogramaRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Escrita do cronograma (RB12–RB19). Toda escrita gera ação no histórico, que é só de inclusão.
 * A edição de etapas acontece em sessão no front; {@link #salvar} recebe o estado completo e
 * recalcula tudo, sem confiar em datas vindas de fora.
 */
@ApplicationScoped
public class EditarCronograma {

    private final CronogramaRepository cronogramas;
    private final ConcursoRepository concursos;
    private final ConsultarCronograma consulta;

    public EditarCronograma(CronogramaRepository cronogramas, ConcursoRepository concursos,
                            ConsultarCronograma consulta) {
        this.cronogramas = cronogramas;
        this.concursos = concursos;
        this.consulta = consulta;
    }

    /** RB12: um por concurso; abrir de novo devolve o existente. */
    @Transactional
    public AbrirCronogramaResponse abrir(Instituicao instituicao, int concursoId, String autor) {
        ConsultarConcursos.exigir(concursos, instituicao, concursoId);
        var existente = cronogramas.idDoConcurso(instituicao, concursoId);
        if (existente.isPresent()) {
            return new AbrirCronogramaResponse(existente.get(), false);
        }
        Esqueleto esqueleto = Esqueletos.ATUAL;
        Cronograma novo = new Cronograma(null, concursoId, esqueleto.nome(),
                cronogramas.parametrosPadrao(instituicao), Edicao.inicial(), Map.of(), null, 0);
        CronogramaCalculado calculado = consulta.calcular(novo, novo.edicao());
        int id = cronogramas.criar(instituicao, novo, calculado);
        cronogramas.registrar(instituicao, id, Acao.de(autor, "Gerou o cronograma a partir do esqueleto",
                esqueleto.nome() + " · só o núcleo · " + calculado.indicadores().etapasAtivas() + " etapas",
                Acao.GERACAO, List.of()));
        return new AbrirCronogramaResponse(id, true);
    }

    @Transactional
    public CronogramaResponse salvar(Instituicao instituicao, int id, SalvarCronogramaRequest request, String autor) {
        Cronograma atual = ConsultarCronograma.exigir(cronogramas, instituicao, id);
        if (request.versao() == null || request.versao() != atual.versao()) {
            throw new ConflitoException("Outra pessoa salvou este cronograma depois que você o abriu. "
                    + "Recarregue para ver a versão atual.");
        }
        Edicao edicao = ConsultarCronograma.edicaoValida(request.edicao());
        edicao.validar(Esqueletos.porNome(atual.esqueleto()));
        if (!Objects.equals(edicao.dataEdital(), atual.edicao().dataEdital())) {
            edicao.exigirEditalNoPrazo(consulta.hoje());
        }

        CronogramaCalculado calculado = consulta.calcular(atual, edicao);
        String justificativa = request.justificativa() == null || request.justificativa().isBlank()
                ? null : request.justificativa().strip();
        if (calculado.checagens().viola() && justificativa == null) {
            throw new RegraDeNegocioException(
                    "O cronograma viola uma restrição. Escreva a justificativa para salvar.");
        }

        cronogramas.salvar(instituicao, atual.comEdicao(edicao, justificativa), calculado);
        for (AcaoRequest acao : request.acoes() == null ? List.<AcaoRequest>of() : request.acoes()) {
            cronogramas.registrar(instituicao, id,
                    Acao.de(autor, acao.acao(), acao.detalhe(), acao.origem(), acao.itens()));
        }
        cronogramas.registrar(instituicao, id, new Acao(null, autor, null, "Salvou o cronograma",
                (calculado.materializado() ? "Materializado" : "Rascunho") + " · "
                        + calculado.indicadores().etapasAtivas() + " etapas ativas"
                        + (calculado.checagens().viola() ? " · viola restrição" : ""),
                Acao.SALVAMENTO, justificativa, List.of()));
        return consulta.buscar(instituicao, id);
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

    /** RN30: marcação à mão conforme o fluxo do tipo da etapa. */
    @Transactional
    public CronogramaResponse marcarStatus(Instituicao instituicao, int id, String codigo, StatusEtapa novo,
                                           String autor) {
        if (novo == null) {
            throw new RegraDeNegocioException("Informe o status.");
        }
        Cronograma cronograma = ConsultarCronograma.exigir(cronogramas, instituicao, id);
        EtapaCalculada etapa = etapa(cronograma, codigo);
        StatusEtapa atual = cronograma.status(codigo);
        StatusEtapa.exigirMarcacao(etapa.tipoStatus(), atual, novo);
        gravarStatus(instituicao, id, codigo, atual, novo, "Mudou o status · " + codigo, autor);
        return consulta.buscar(instituicao, id);
    }

    /** RN30: gatilho "Registrar 1º recurso", liberado quando o prazo abre. */
    @Transactional
    public CronogramaResponse registrarPrimeiroRecurso(Instituicao instituicao, int id, String codigo, String autor) {
        Cronograma cronograma = ConsultarCronograma.exigir(cronogramas, instituicao, id);
        EtapaCalculada etapa = etapa(cronograma, codigo);
        StatusEtapa atual = cronograma.status(codigo);
        StatusEtapa novo = StatusEtapa.registrarPrimeiroRecurso(etapa, atual, consulta.hoje());
        gravarStatus(instituicao, id, codigo, atual, novo, "Registrou o 1º recurso · " + codigo, autor);
        return consulta.buscar(instituicao, id);
    }

    private EtapaCalculada etapa(Cronograma cronograma, String codigo) {
        return consulta.calcular(cronograma, cronograma.edicao()).etapa(codigo)
                .orElseThrow(() -> new NaoEncontradoException("Etapa do cronograma", codigo));
    }

    private void gravarStatus(Instituicao instituicao, int id, String codigo, StatusEtapa atual, StatusEtapa novo,
                              String acao, String autor) {
        cronogramas.mudarStatus(instituicao, id, codigo, novo);
        cronogramas.registrar(instituicao, id, Acao.de(autor, acao, atual + " → " + novo, Acao.EDICAO,
                List.of(new Previa.Item(codigo, "status", atual.name(), novo.name()))));
    }
}

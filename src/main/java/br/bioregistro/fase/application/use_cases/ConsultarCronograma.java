package br.bioregistro.fase.application.use_cases;

import br.bioregistro.fase.application.dtos.CronogramaDtos.CronogramaResponse;
import br.bioregistro.fase.application.dtos.CronogramaDtos.EditalDoCronogramaResponse;
import br.bioregistro.fase.domain.model.cronograma.EditalDoCronograma;
import br.bioregistro.fase.application.dtos.CronogramaDtos.ModeloResponse;
import br.bioregistro.fase.application.dtos.CronogramaDtos.ModuloResponse;
import br.bioregistro.fase.application.dtos.CronogramaDtos.ParametroResponse;
import br.bioregistro.fase.application.dtos.CronogramaDtos.StatusEtapaResponse;
import br.bioregistro.fase.domain.exception.NaoEncontradoException;
import br.bioregistro.fase.domain.exception.RegraDeNegocioException;
import br.bioregistro.fase.domain.model.Instituicao;
import br.bioregistro.fase.domain.model.cronograma.Acao;
import br.bioregistro.fase.domain.model.cronograma.CalculoCronograma;
import br.bioregistro.fase.domain.model.cronograma.Cronograma;
import br.bioregistro.fase.domain.model.cronograma.CronogramaCalculado;
import br.bioregistro.fase.domain.model.cronograma.Edicao;
import br.bioregistro.fase.domain.model.cronograma.EtapaCalculada;
import br.bioregistro.fase.domain.model.cronograma.Esqueleto;
import br.bioregistro.fase.domain.model.cronograma.Esqueletos;
import br.bioregistro.fase.domain.model.cronograma.Mudanca;
import br.bioregistro.fase.domain.model.cronograma.Parametros;
import br.bioregistro.fase.domain.model.cronograma.Previa;
import br.bioregistro.fase.domain.model.cronograma.StatusEtapa;
import br.bioregistro.fase.domain.model.cronograma.TipoStatus;
import br.bioregistro.fase.domain.port.CronogramaRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

/**
 * Leitura e cálculo sem gravar: modelo, cronograma salvo, simulação e prévia (RB04–RB11,
 * RB20, RB21). O front nunca calcula datas: tudo passa por aqui.
 */
@ApplicationScoped
public class ConsultarCronograma {

    private final CronogramaRepository cronogramas;
    private final Clock relogio;

    public ConsultarCronograma(CronogramaRepository cronogramas, Clock relogio) {
        this.cronogramas = cronogramas;
        this.relogio = relogio;
    }

    public ModeloResponse modelo(Instituicao instituicao) {
        Esqueleto esqueleto = Esqueletos.ATUAL;
        List<ModuloResponse> modulos = esqueleto.modulos().entrySet().stream()
                .map(m -> new ModuloResponse(m.getKey(), m.getValue(), esqueleto.etapasDoModulo(m.getKey())))
                .toList();
        return new ModeloResponse(esqueleto.nome(), esqueleto.grupos(), esqueleto.etapas(), modulos,
                esqueleto.atalhos(), parametros(cronogramas.parametrosPadrao(instituicao)));
    }

    public CronogramaResponse buscar(Instituicao instituicao, int id) {
        return buscar(instituicao, id, null);
    }

    /**
     * @param editalId nulo = o cronograma compartilhado; preenchido = a visão do edital, com a
     *                 sobreposição dele (versão = a da sobreposição)
     */
    public CronogramaResponse buscar(Instituicao instituicao, int id, Integer editalId) {
        Alvo alvo = alvo(instituicao, id, editalId);
        Cronograma cronograma = alvo.visto();
        LocalDate hoje = hoje();
        CronogramaCalculado calculado = calcular(cronograma, cronograma.edicao());
        List<StatusEtapaResponse> status = calculado.etapas().stream()
                .map(e -> status(e, cronograma, hoje))
                .toList();
        List<EditalDoCronogramaResponse> editais = cronogramas.editais(instituicao, id).stream()
                .map(EditalDoCronogramaResponse::de)
                .toList();
        return new CronogramaResponse(id, editalId, cronograma.esqueleto(),
                cronograma.versao(),
                calculado.materializado() ? "MATERIALIZADO" : "RASCUNHO", cronograma.justificativa(),
                cronograma.edicao(), parametros(cronograma.parametros()), calculado, status,
                cronogramas.particularidades(instituicao, id),
                alvo.edital() == null ? null : alvo.edital().sobreposicao().codigos(), editais);
    }

    /** O cronograma e, quando há {@code editalId}, o edital que o segue. */
    record Alvo(Cronograma base, EditalDoCronograma edital) {

        Cronograma visto() {
            return edital == null ? base : base.doEdital(edital);
        }

        Integer editalId() {
            return edital == null ? null : edital.editalId();
        }
    }

    Alvo alvo(Instituicao instituicao, int id, Integer editalId) {
        Cronograma base = exigir(cronogramas, instituicao, id);
        if (editalId == null) {
            return new Alvo(base, null);
        }
        EditalDoCronograma edital = cronogramas.doEdital(instituicao, editalId)
                .filter(e -> e.cronogramaId() == id)
                .orElseThrow(() -> new NaoEncontradoException("Edital no cronograma " + id + ":", editalId));
        return new Alvo(base, edital);
    }

    public CronogramaCalculado simular(Instituicao instituicao, int id, Edicao edicao) {
        Cronograma cronograma = exigir(cronogramas, instituicao, id);
        Edicao valida = edicaoValida(edicao);
        valida.validar(Esqueletos.porNome(cronograma.esqueleto()));
        return calcular(cronograma, valida);
    }

    public Previa prever(Instituicao instituicao, int id, Edicao edicao, Mudanca mudanca) {
        if (mudanca == null || mudanca.codigo() == null || mudanca.campo() == null) {
            throw new RegraDeNegocioException("Informe a mudança: etapa, campo e valor.");
        }
        Cronograma cronograma = exigir(cronogramas, instituicao, id);
        Esqueleto esqueleto = Esqueletos.porNome(cronograma.esqueleto());
        Edicao valida = edicaoValida(edicao);
        valida.validar(esqueleto);
        return CalculoCronograma.prever(esqueleto, cronograma.parametros(), valida, mudanca);
    }

    public List<Acao> historico(Instituicao instituicao, int id) {
        exigir(cronogramas, instituicao, id);
        return cronogramas.historico(instituicao, id);
    }

    CronogramaCalculado calcular(Cronograma cronograma, Edicao edicao) {
        return CalculoCronograma.calcular(Esqueletos.porNome(cronograma.esqueleto()), cronograma.parametros(),
                edicao, hoje());
    }

    LocalDate hoje() {
        return LocalDate.now(relogio);
    }

    static Cronograma exigir(CronogramaRepository cronogramas, Instituicao instituicao, int id) {
        return cronogramas.buscar(instituicao, id).orElseThrow(() -> new NaoEncontradoException("Cronograma", id));
    }

    static Edicao edicaoValida(Edicao edicao) {
        return edicao == null ? Edicao.inicial() : edicao;
    }

    private static StatusEtapaResponse status(EtapaCalculada etapa, Cronograma cronograma, LocalDate hoje) {
        StatusEtapa manual = cronograma.status(etapa.codigo());
        boolean podeRegistrar = etapa.tipoStatus() == TipoStatus.RECURSO && etapa.ativa()
                && manual == StatusEtapa.A_FAZER && etapa.inicio() != null && !hoje.isBefore(etapa.inicio());
        return new StatusEtapaResponse(etapa.codigo(), manual,
                StatusEtapa.automatico(etapa, cronograma.status(), hoje),
                StatusEtapa.fluxo(etapa.tipoStatus()), podeRegistrar);
    }

    private static List<ParametroResponse> parametros(Parametros parametros) {
        return parametros.ordenados().entrySet().stream()
                .map(p -> new ParametroResponse(p.getKey(), Parametros.DESCRICOES.get(p.getKey()), p.getValue(),
                        Parametros.PRAZOS_PADRAO.contains(p.getKey())))
                .toList();
    }
}

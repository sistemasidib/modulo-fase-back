package br.bioregistro.fase.application.dtos;

import br.bioregistro.fase.domain.model.cronograma.CronogramaCalculado;
import br.bioregistro.fase.domain.model.cronograma.Edicao;
import br.bioregistro.fase.domain.model.cronograma.EtapaModelo;
import br.bioregistro.fase.domain.model.cronograma.GrupoModelo;
import br.bioregistro.fase.domain.model.cronograma.Mudanca;
import br.bioregistro.fase.domain.model.cronograma.Particularidade;
import br.bioregistro.fase.domain.model.cronograma.Previa;
import br.bioregistro.fase.domain.model.cronograma.StatusEtapa;

import java.util.List;
import java.util.Map;

/**
 * Contrato HTTP do cronograma (spec 003-B, plano §5). {@link Edicao}, {@link Mudanca},
 * {@link CronogramaCalculado} e {@link Previa} são valores do domínio e vão direto no JSON.
 */
public final class CronogramaDtos {

    private CronogramaDtos() {
    }

    // ── modelo ────────────────────────────────────────────────────────────────

    public record ModeloResponse(String nome, List<GrupoModelo> grupos, List<EtapaModelo> etapas,
                                 List<ModuloResponse> modulos, Map<String, List<String>> atalhos,
                                 List<ParametroResponse> parametros) {
    }

    public record ModuloResponse(String codigo, String nome, long etapas) {
    }

    /** @param prazoPadrao travado até liberar exceção (RN19) */
    public record ParametroResponse(String chave, String descricao, int valor, boolean prazoPadrao) {
    }

    // ── cronograma ────────────────────────────────────────────────────────────

    public record AbrirCronogramaResponse(int id, boolean criado) {
    }

    public record CronogramaResponse(int id, int concursoId, String esqueleto, int versao, String situacao,
                                     String justificativa, Edicao edicao, List<ParametroResponse> parametros,
                                     CronogramaCalculado calculado, List<StatusEtapaResponse> status,
                                     List<Particularidade> particularidades) {
    }

    /**
     * @param automatico    alerta (Atrasado, Bloqueado, publicação vencida); não impede nada
     * @param podeRegistrar "Registrar 1º recurso" liberado (prazo aberto e ainda A fazer)
     */
    public record StatusEtapaResponse(String codigo, StatusEtapa manual, StatusEtapa.Automatico automatico,
                                      List<StatusEtapa> fluxo, boolean podeRegistrar) {
    }

    public record PreviaRequest(Edicao edicao, Mudanca mudanca) {
    }

    /** Ação da sessão de edição, montada no front e gravada no salvamento (RN27). */
    public record AcaoRequest(String acao, String detalhe, String origem, List<Previa.Item> itens) {
    }

    /** @param versao a versão lida; outra pessoa salvou antes = 409 */
    public record SalvarCronogramaRequest(Integer versao, Edicao edicao, List<AcaoRequest> acoes,
                                          String justificativa) {
    }

    public record StatusRequest(StatusEtapa status) {
    }

    public record ParticularidadeRequest(String texto, String assunto, String etapaCodigo) {
    }
}

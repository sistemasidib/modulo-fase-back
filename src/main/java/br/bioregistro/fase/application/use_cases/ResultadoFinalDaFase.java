package br.bioregistro.fase.application.use_cases;

import br.bioregistro.fase.domain.model.Etapa;
import br.bioregistro.fase.domain.model.Instituicao;
import br.bioregistro.fase.domain.model.TipoEtapa;
import br.bioregistro.fase.domain.port.EtapaRepository;
import br.bioregistro.fase.domain.port.TipoRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Garante a etapa obrigatória &ldquo;Resultado Final&rdquo; em toda fase. Não é opção de fase do
 * grupo (tipo {@link TipoEtapa#RESULTADO_FINAL} fica fora de EXECUCAO). Sempre a última etapa.
 */
@ApplicationScoped
public class ResultadoFinalDaFase {

    private static final String NOME = "Resultado Final";

    private final TipoRepository tipos;
    private final EtapaRepository etapas;

    public ResultadoFinalDaFase(TipoRepository tipos, EtapaRepository etapas) {
        this.tipos = tipos;
        this.etapas = etapas;
    }

    /**
     * Cria Resultado Final se a fase ainda não tiver. Chamado após criar/gerar etapas de execução
     * para que, em fases novas, o id fique depois das demais; listagens usam {@link #comFinalPorUltimo}.
     */
    public void garantir(Instituicao instituicao, int faseId) {
        TipoEtapa tipo = tipoResultadoFinal(instituicao);
        Optional<Etapa> existente = etapas.listarPorFase(instituicao, faseId).stream()
                .filter(e -> tipo.id().equals(e.tipoId()))
                .findFirst();
        if (existente.isPresent()) {
            Etapa atual = existente.get();
            if (!NOME.equals(atual.nome())) {
                etapas.salvar(instituicao, CamposDaEtapa.aplicar(tipos, instituicao,
                        new Etapa(atual.id(), atual.faseId(), NOME, atual.tipoId(), atual.descricao(),
                                atual.dataAbertura(), atual.dataFechamento(), atual.statusId(),
                                atual.reprovavel(), atual.obrigatoriaParaFase(), atual.todosCandidatos(),
                                atual.aceitaDocumentos(), atual.aceitaRecurso(), atual.preliminar(),
                                atual.elegibilidadeEtapaId(), atual.elegibilidadeStatusId(), atual.responsavelId(),
                                atual.publicadaEm(), atual.notaZeroElimina(), atual.notaMinimaPercentual(),
                                atual.modalidade()),
                        false));
            }
            return;
        }
        etapas.salvar(instituicao, CamposDaEtapa.aplicar(tipos, instituicao,
                new Etapa(null, faseId, NOME, tipo.id(), null, null, null, null,
                        false, true, true, false, null, null, null, null, null, null, null, null, null),
                true));
    }

    public boolean ehObrigatoria(Instituicao instituicao, Etapa etapa) {
        return tipos.tipoEtapa(instituicao, etapa.tipoId()).map(TipoEtapa::ehResultadoFinal).orElse(false);
    }

    /** Demais etapas por id; Resultado Final sempre no fim. */
    public List<Etapa> comFinalPorUltimo(Instituicao instituicao, List<Etapa> lista) {
        Integer tipoId = tipoResultadoFinal(instituicao).id();
        List<Etapa> ordenada = new ArrayList<>(lista);
        ordenada.sort(Comparator
                .comparing((Etapa e) -> tipoId.equals(e.tipoId()))
                .thenComparing(e -> e.id() == null ? Integer.MAX_VALUE : e.id()));
        return ordenada;
    }

    private TipoEtapa tipoResultadoFinal(Instituicao instituicao) {
        return tipos.tiposEtapa(instituicao).stream()
                .filter(TipoEtapa::ehResultadoFinal)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "dom_tipo_etapa sem o valor '" + TipoEtapa.RESULTADO_FINAL + "' em " + instituicao + "."));
    }
}

package br.bioregistro.fase.application.use_cases;

import br.bioregistro.fase.domain.model.Etapa;
import br.bioregistro.fase.domain.model.Instituicao;
import br.bioregistro.fase.domain.model.TipoEtapa;
import br.bioregistro.fase.domain.port.EtapaRepository;
import br.bioregistro.fase.domain.port.TipoRepository;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Garante a etapa obrigatória &ldquo;Resultado Final&rdquo; em toda fase. Não é opção de fase do
 * grupo (tipo {@link TipoEtapa#RESULTADO_FINAL} fica fora de EXECUCAO).
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

    public void garantir(Instituicao instituicao, int faseId) {
        TipoEtapa tipo = tipos.tiposEtapa(instituicao).stream()
                .filter(TipoEtapa::ehResultadoFinal)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "dom_tipo_etapa sem o valor '" + TipoEtapa.RESULTADO_FINAL + "' em " + instituicao + "."));
        boolean jaTem = etapas.listarPorFase(instituicao, faseId).stream()
                .anyMatch(e -> tipo.id().equals(e.tipoId()));
        if (jaTem) {
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
}

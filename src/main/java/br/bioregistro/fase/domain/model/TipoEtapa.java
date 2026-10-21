package br.bioregistro.fase.domain.model;

import java.util.Set;

/** Linha de {@code dom_tipo_etapa} (prova objetiva, TAF, análise de PcD...). */
public record TipoEtapa(Integer id, String valor, String descricao) {

    /**
     * Etapa obrigatória de toda fase: não entra nas opções de fase do grupo (não é EXECUCAO).
     * Valor em {@code dom_tipo_etapa}.
     */
    public static final String RESULTADO_FINAL = "resultado";

    /** Fases de execução: o candidato é avaliado (RN05 da spec 003). */
    private static final Set<String> EXECUCAO = Set.of(
            "prova_objetiva", "prova_discursiva", "taf", "the", "heteroidentificacao", "biopsicossocial",
            "prova_titulos");

    public enum Categoria { EXECUCAO, ANALISE, LEGADO }

    /** Análise de solicitações não é fase; tipos antigos ({@code inscricao}, {@code resultado}...) são legado. */
    public Categoria categoria() {
        if (EXECUCAO.contains(valor)) {
            return Categoria.EXECUCAO;
        }
        return valor != null && valor.startsWith("analise_") ? Categoria.ANALISE : Categoria.LEGADO;
    }

    public boolean ehResultadoFinal() {
        return RESULTADO_FINAL.equals(valor);
    }
}

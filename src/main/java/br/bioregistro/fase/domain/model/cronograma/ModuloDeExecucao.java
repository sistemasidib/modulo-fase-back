package br.bioregistro.fase.domain.model.cronograma;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Ponte cronograma → estrutura (spec 005, RN02): os módulos do cronograma que viram fase de
 * execução no edital, o tipo de etapa de cada um e de que linha do cronograma vem cada
 * publicação. Os demais módulos (títulos, prática, saúde, heteroidentificação) só têm datas no
 * cronograma; THE espera a P-12.
 *
 * @param grupo       grupo do esqueleto que liga o módulo; {@code null} = núcleo (sempre ligado)
 * @param tipoEtapa   {@code dom_tipo_etapa.valor}
 * @param abertura    linha cujo início abre a fase
 * @param fechamento  linha cujo fim fecha a fase
 */
public enum ModuloDeExecucao {

    NUCLEO(null, "prova_objetiva", "Prova objetiva", "E14", "E20", List.of(
            new PublicacaoDoModulo("local_prova", "E14", null),
            new PublicacaoDoModulo("gabarito_preliminar", "E16", "E17"),
            new PublicacaoDoModulo("gabarito_definitivo", "E18", null),
            new PublicacaoDoModulo("resultado_preliminar", "E18", "E19"),
            new PublicacaoDoModulo("resultado_oficial", "E20", null))),
    S12("S12", "prova_discursiva", "Prova discursiva", "E15", "E23", List.of(
            new PublicacaoDoModulo("resultado_preliminar", "E21", "E22"),
            new PublicacaoDoModulo("resultado_oficial", "E23", null))),
    S14("S14", "taf", "Teste de aptidão física (TAF)", "E29", "E33", List.of(
            new PublicacaoDoModulo("resultado_preliminar", "E31", "E32"),
            new PublicacaoDoModulo("resultado_oficial", "E33", null)));

    /**
     * @param tipo    {@code dom_tipo_publicacao.valor}
     * @param codigo  linha do cronograma com a data da publicação
     * @param recurso linha do recurso contra ela; {@code null} = não abre recurso
     */
    public record PublicacaoDoModulo(String tipo, String codigo, String recurso) {
    }

    private final String grupo;
    private final String tipoEtapa;
    private final String nome;
    private final String abertura;
    private final String fechamento;
    private final List<PublicacaoDoModulo> publicacoes;

    ModuloDeExecucao(String grupo, String tipoEtapa, String nome, String abertura, String fechamento,
                     List<PublicacaoDoModulo> publicacoes) {
        this.grupo = grupo;
        this.tipoEtapa = tipoEtapa;
        this.nome = nome;
        this.abertura = abertura;
        this.fechamento = fechamento;
        this.publicacoes = publicacoes;
    }

    public String tipoEtapa() {
        return tipoEtapa;
    }

    public String nome() {
        return nome;
    }

    public String abertura() {
        return abertura;
    }

    public String fechamento() {
        return fechamento;
    }

    public List<PublicacaoDoModulo> publicacoes() {
        return publicacoes;
    }

    /** Ligado no cronograma calculado: o núcleo sempre; os demais quando o grupo do módulo está ativo. */
    public boolean ligado(CronogramaCalculado calculado) {
        return grupo == null || calculado.grupos().stream().anyMatch(g -> g.codigo().equals(grupo) && g.ativo());
    }

    public static Optional<ModuloDeExecucao> doTipoEtapa(String valor) {
        return Arrays.stream(values()).filter(m -> m.tipoEtapa.equals(valor)).findFirst();
    }
}

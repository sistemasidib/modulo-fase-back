package br.bioregistro.fase.domain.model.cronograma;

import br.bioregistro.fase.domain.exception.RegraDeNegocioException;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Números do modelo (RN13–RN17, RN19, RN24). Os valores vêm do banco; os padrões abaixo só
 * valem para chaves que o banco não tiver.
 */
public record Parametros(Map<String, Integer> valores) {

    public static final String INICIO_INSCRICOES = "inicio_inscricoes";
    public static final String DURACAO_INSCRICOES = "duracao_inscricoes";
    public static final String ISENCAO = "isencao";
    public static final String PAGAMENTO = "pagamento";
    public static final String INTERVALO_PROVA = "intervalo_prova";
    public static final String GABARITO_APOS_PROVA = "gabarito_apos_prova";
    public static final String RECURSO_GABARITO = "recurso_gabarito";
    public static final String RECURSO_DEMAIS = "recurso_demais";
    public static final String RM01_MINIMO = "rm01_minimo";

    /** Ordem de exibição e valores de fábrica. */
    public static final Map<String, Integer> PADRAO = padrao();

    /** RN19: prazos travados; mudar exige exceção. */
    public static final Set<String> PRAZOS_PADRAO = Set.of(
            ISENCAO, PAGAMENTO, INTERVALO_PROVA, GABARITO_APOS_PROVA, RECURSO_GABARITO, RECURSO_DEMAIS);

    public static final Map<String, String> DESCRICOES = Map.of(
            INICIO_INSCRICOES, "Início das inscrições (dias depois do edital)",
            DURACAO_INSCRICOES, "Duração das inscrições",
            ISENCAO, "Pedido de isenção da taxa (duração)",
            PAGAMENTO, "Último dia de pagamento (dias depois do fim das inscrições)",
            INTERVALO_PROVA, "Fim das inscrições → prova objetiva (sugestão até a data da prova ser informada)",
            GABARITO_APOS_PROVA, "Gabarito preliminar (dias depois da prova)",
            RECURSO_GABARITO, "Recurso contra o gabarito (duração)",
            RECURSO_DEMAIS, "Demais recursos (duração)",
            RM01_MINIMO, "RM-01: mínimo entre o fim das inscrições e a prova");

    public Parametros {
        Map<String, Integer> completos = new LinkedHashMap<>(PADRAO);
        if (valores != null) {
            valores.forEach((chave, valor) -> {
                if (!PADRAO.containsKey(chave)) {
                    throw new RegraDeNegocioException("Parâmetro desconhecido: " + chave + ".");
                }
                if (valor == null || valor < 0) {
                    throw new RegraDeNegocioException("O parâmetro " + chave + " precisa ser um número de dias ≥ 0.");
                }
                completos.put(chave, valor);
            });
        }
        valores = Map.copyOf(completos);
    }

    public static Parametros padroes() {
        return new Parametros(Map.of());
    }

    public int valor(String chave) {
        Integer valor = valores.get(chave);
        if (valor == null) {
            throw new IllegalArgumentException("Parâmetro desconhecido: " + chave);
        }
        return valor;
    }

    /** Mantém a ordem de {@link #PADRAO} para exibição. */
    public Map<String, Integer> ordenados() {
        Map<String, Integer> ordem = new LinkedHashMap<>();
        PADRAO.keySet().forEach(chave -> ordem.put(chave, valores.get(chave)));
        return ordem;
    }

    private static Map<String, Integer> padrao() {
        Map<String, Integer> p = new LinkedHashMap<>();
        p.put(INICIO_INSCRICOES, 1);
        p.put(DURACAO_INSCRICOES, 41);
        p.put(ISENCAO, 1);
        p.put(PAGAMENTO, 1);
        p.put(INTERVALO_PROVA, 34);
        p.put(GABARITO_APOS_PROVA, 1);
        p.put(RECURSO_GABARITO, 2);
        p.put(RECURSO_DEMAIS, 1);
        p.put(RM01_MINIMO, 30);
        return java.util.Collections.unmodifiableMap(p);
    }
}

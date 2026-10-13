package br.bioregistro.fase.domain.model.cronograma;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static br.bioregistro.fase.domain.model.cronograma.Parametros.DURACAO_INSCRICOES;
import static br.bioregistro.fase.domain.model.cronograma.Parametros.GABARITO_APOS_PROVA;
import static br.bioregistro.fase.domain.model.cronograma.Parametros.INICIO_INSCRICOES;
import static br.bioregistro.fase.domain.model.cronograma.Parametros.INTERVALO_PROVA;
import static br.bioregistro.fase.domain.model.cronograma.Parametros.ISENCAO;
import static br.bioregistro.fase.domain.model.cronograma.Parametros.PAGAMENTO;
import static br.bioregistro.fase.domain.model.cronograma.Parametros.RECURSO_DEMAIS;
import static br.bioregistro.fase.domain.model.cronograma.Parametros.RECURSO_GABARITO;

/**
 * Esqueleto IDECAN v1 (protótipo de 08/10/2026, {@code .speckit/nova regra/}), com os ajustes da
 * spec 003: Fase 1 = prova objetiva (RN06), gabarito = prova + parâmetro (RN15) e recurso do
 * gabarito aberto no dia da publicação por 2 dias (RN16). E16 e E17 ficam sem faixa histórica.
 *
 * <p>E14 e E15 recalibradas com 127 editais IDECAN de 2018–2026 (out/2026): locais de prova 9 dias
 * antes da prova; fim das inscrições → prova com P25–P75 de 27–40 dias.
 */
public final class EsqueletoIdecanV1 {

    public static final String NOME = "Esqueleto IDECAN v1";

    public static final String EDITAL_INSCRICOES = "EDITAL_INSCRICOES";
    public static final String F1_PROVA_OBJETIVA = "F1_PROVA_OBJETIVA";
    public static final String ENCERRAMENTO = "ENCERRAMENTO";

    private static final Pattern RECURSO = Pattern.compile(
            "^(Período para interposição|Prazo para interposição|Interposição|Período para impugnação)");
    private static final Pattern PUBLICACAO = Pattern.compile("^(Publicação|Divulgação|Edital|Convocação|Resultado)");
    private static final Pattern FAIXA = Pattern.compile("(-?\\d+)-(-?\\d+)");

    public static final Esqueleto ESQUELETO = criar();

    private EsqueletoIdecanV1() {
    }

    private static Esqueleto criar() {
        List<GrupoModelo> grupos = List.of(
                new GrupoModelo(EDITAL_INSCRICOES, "Edital e inscrições", false, null,
                        "Abertura, impugnação, inscrições, isenção, pagamento e atendimento especial. Não é fase."),
                new GrupoModelo(F1_PROVA_OBJETIVA, "Fase 1 · Prova objetiva", true, null, null),
                new GrupoModelo("S12", "Prova discursiva", true, "S12", null),
                new GrupoModelo("S13", "Prova de títulos", true, "S13", null),
                new GrupoModelo("S14", "Exame de capacidade física", true, "S14", null),
                new GrupoModelo("S15", "Prova prática · desempenho didático", true, "S15",
                        "E34 chama de \"Prova Prática\"; E35–E38 de \"desempenho didático\""),
                new GrupoModelo("S16", "Exame de saúde", true, "S16",
                        "E42–E43 citam \"Avaliação Psicológica\" num bloco de saúde"),
                new GrupoModelo("S05", "Heteroidentificação e biopsicossocial", true, "S05", null),
                new GrupoModelo(GrupoModelo.S05_DESLIGADO, "Biopsicossocial só para PcD", true,
                        GrupoModelo.S05_DESLIGADO, "Entra no lugar de E44–E48 quando S05 está desligado"),
                new GrupoModelo(ENCERRAMENTO, "Encerramento", false, null, null));

        List<Construcao> e = new ArrayList<>();
        String g = EDITAL_INSCRICOES;
        e.add(etapa("E01", "Publicação do edital de abertura", g, Referencia.MARCO_ZERO, null, null, 0, 69));
        e.add(etapa("E02", "Período para impugnação ao edital", g, Referencia.inicio("E01"), 0, "0-1", 1, 49));
        e.add(etapa("E03", "Publicação das respostas às impugnações ao Edital, caso existentes", g, Referencia.fim("E02"), 7, "7-11", 0, 51));
        e.add(etapa("E04", "Período de inscrições", g, Referencia.inicio("E01"), 1, "0-27", 31, 68)
                .comParametros(INICIO_INSCRICOES, DURACAO_INSCRICOES));
        e.add(etapa("E05", "Período de inscrições para os candidatos que desejam requerer a isenção da taxa de inscrição", g, Referencia.inicio("E04"), 0, "0-0", 1, 55)
                .comParametros(null, ISENCAO));
        e.add(etapa("E06", "Publicação do resultado preliminar dos pedidos de isenção", g, Referencia.fim("E05"), 13, "11-17", 0, 72));
        e.add(etapa("E07", "Período para interposição de recurso contra o resultado preliminar do pedido de isenção do pagamento da taxa de inscrição", g, Referencia.inicio("E06"), 1, "1-2", 1, 65));
        e.add(etapa("E08", "Publicação do resultado definitivo dos pedidos de isenção", g, Referencia.fim("E07"), 8, "6-11", 0, 64));
        e.add(etapa("E09", "Último dia para envio da documentação comprobatória do candidato inscrito na condição de Pessoa com Deficiência e solicitação de atendimento especial", g, Referencia.fim("E04"), 1, "0-1", 0, 45));
        e.add(etapa("E10", "Último dia para geração e pagamento da taxa de inscrição", g, Referencia.fim("E04"), 1, "1-1", 0, 61)
                .comParametros(PAGAMENTO, null));
        e.add(etapa("E11", "Divulgação do resultado preliminar de inscrição (Atendimento Especial) na área do candidato", g, Referencia.fim("E04"), 11, "8-16", 0, 76));
        e.add(etapa("E12", "Período para interposição de recurso contra o resultado preliminar de inscrição (Atendimento Especial)", g, Referencia.inicio("E11"), 1, "1-2", 1, 66));
        e.add(etapa("E13", "Divulgação do resultado definitivo de inscrição (Atendimento Especial)", g, Referencia.fim("E12"), 8, "6-12", 0, 64));

        g = F1_PROVA_OBJETIVA;
        e.add(etapa("E14", "Divulgação dos locais de prova", g, Referencia.inicio("E15"), -9, "-9--9", 0, 114));
        e.add(etapa("E15", "Realização das Provas Objetivas", g, Referencia.fim("E04"), 34, "27-40", 0, 124)
                .comParametros(INTERVALO_PROVA, null));
        e.add(etapa("E16", "Publicação dos gabaritos preliminares das provas objetivas", g, Referencia.inicio("E15"), 15, null, 0, 63)
                .comParametros(GABARITO_APOS_PROVA, null));
        e.add(etapa("E17", "Prazo para interposição de recurso contra os gabaritos preliminares das provas objetivas", g, Referencia.inicio("E16"), 0, null, 2, 53)
                .comParametros(null, RECURSO_GABARITO));
        e.add(etapa("E18", "Publicação dos gabaritos definitivos das provas objetivas + resultado preliminar da prova objetiva + espelho do cartão resposta na área do candidato", g, Referencia.fim("E17"), 21, "19-26", 0, 47));
        e.add(etapa("E19", "Interposição de recurso contra o resultado preliminar da prova objetiva", g, Referencia.inicio("E18"), 1, "1-2", 1, 47));
        e.add(etapa("E20", "Publicação do resultado definitivo das provas objetivas", g, Referencia.fim("E19"), 16, "9-18", 0, 43).resultadoFinal());

        g = "S12";
        e.add(etapa("E21", "Publicação do resultado preliminar da prova discursiva", g, Referencia.inicio("E20"), 15, "14-19", 0, 7));
        e.add(etapa("E22", "Prazo para interposição de recurso contra o resultado preliminar das Provas discursivas", g, Referencia.inicio("E21"), 1, "1-3", 1, 9));
        e.add(etapa("E23", "Publicação do resultado definitivo da prova discursiva", g, Referencia.fim("E22"), 15, "13-21", 0, 11).resultadoFinal());

        g = "S13";
        e.add(etapa("E24", "Edital de convocação para a prova de títulos", g, Referencia.inicio("E20"), 3, "1-35", 0, 18));
        e.add(etapa("E25", "Período para envio da prova de títulos", g, Referencia.inicio("E24"), 1, "1-4", 2, 25));
        e.add(etapa("E26", "Publicação do resultado preliminar da prova de títulos na área do candidato", g, Referencia.fim("E25"), 12, "11-13", 0, 24));
        e.add(etapa("E27", "Interposição de recurso contra o resultado preliminar da prova de títulos", g, Referencia.inicio("E26"), 1, "1-2", 1, 24));
        e.add(etapa("E28", "Publicação do resultado definitivo da prova de títulos", g, Referencia.fim("E27"), 12, "7-13", 0, 20).resultadoFinal());

        g = "S14";
        e.add(etapa("E29", "Publicação da convocação dos candidatos para realizarem o Exame de Capacidade Física", g, Referencia.inicio("E20"), 1, "0-40", 0, 9));
        e.add(etapa("E30", "Realização do Exame de Capacidade Física", g, Referencia.inicio("E29"), 5, "4-8", 3, 12));
        e.add(etapa("E31", "Publicação do resultado preliminar do Exame de Capacidade Física", g, Referencia.fim("E30"), 3, "3-10", 0, 11));
        e.add(etapa("E32", "Prazo para interposição de recurso contra o resultado preliminar da Prova de Capacidade Física", g, Referencia.inicio("E31"), 1, "1-1", 1, 8));
        e.add(etapa("E33", "Publicação do resultado definitivo do Exame de Capacidade Física", g, Referencia.fim("E32"), 6, "5-10", 0, 9).resultadoFinal());

        g = "S15";
        e.add(etapa("E34", "Convocação para a Prova Prática", g, Referencia.inicio("E20"), 3, null, 0, 3));
        e.add(etapa("E35", "Realização das provas de desempenho didático", g, Referencia.inicio("E34"), 7, "4-10", 1, 4));
        e.add(etapa("E36", "Divulgação do resultado preliminar da prova de desempenho didático", g, Referencia.fim("E35"), 6, "4-12", 0, 4));
        e.add(etapa("E37", "Interposição de recurso contra o resultado preliminar da prova de desempenho didático", g, Referencia.inicio("E36"), 1, null, 1, 3));
        e.add(etapa("E38", "Resultado definitivo da prova de desempenho didático", g, Referencia.fim("E37"), 10, "7-25", 0, 4).resultadoFinal());

        g = "S16";
        e.add(etapa("E39", "Publicação da convocação dos candidatos para realizarem o Exame de Saúde", g, Referencia.inicio("E20"), 33, "1-110", 0, 10));
        e.add(etapa("E40", "Realização do Exame de Saúde", g, Referencia.inicio("E39"), 24, "6-27", 2, 13));
        e.add(etapa("E41", "Divulgação do resultado preliminar da Inspeção de Saúde – Dos exames médicos, biométricos e complementares, testes toxicológicos e exames odontológicos na área do candidato", g, Referencia.fim("E40"), 15, "3-20", 0, 11));
        e.add(etapa("E42", "Prazo para interposição de recurso contra o resultado preliminar da Avaliação Psicológica", g, Referencia.inicio("E41"), 5, "2-7", 1, 10));
        e.add(etapa("E43", "Publicação do resultado definitivo da Avaliação Psicológica", g, Referencia.fim("E42"), 8, "7-23", 0, 10).resultadoFinal());

        g = "S05";
        e.add(etapa("E44", "Edital de convocação para heteroidentificação e avaliação biopsicossocial", g, Referencia.inicio("E20"), 32, "1-53", 0, 19));
        e.add(etapa("E45", "Realização da heteroidentificação e biopsicossocial", g, Referencia.inicio("E44"), 8, "5-10", 1, 23));
        e.add(etapa("E46", "Publicação do resultado preliminar da heteroidentificação e biopsicossocial", g, Referencia.fim("E45"), 3, "3-6", 0, 17));
        e.add(etapa("E47", "Interposição de recurso contra o resultado preliminar da heteroidentificação e biopsicossocial", g, Referencia.inicio("E46"), 1, "1-2", 1, 16));
        e.add(etapa("E48", "Publicação do resultado definitivo da heteroidentificação e biopsicossocial", g, Referencia.fim("E47"), 7, "5-10", 0, 16).resultadoFinal());

        g = GrupoModelo.S05_DESLIGADO;
        e.add(etapa("E49", "Convocação dos candidatos para a avaliação biopsicossocial", g, Referencia.inicio("E20"), 32, "1-53", 0, 19));
        e.add(etapa("E50", "Realização da avaliação biopsicossocial (candidatos com deficiência)", g, Referencia.inicio("E49"), 8, "5-10", 1, 23));
        e.add(etapa("E51", "Publicação do resultado preliminar da Avaliação Perícia Médica", g, Referencia.fim("E50"), 3, "3-6", 0, 17));
        e.add(etapa("E52", "Interposição de recurso contra o resultado preliminar da avaliação biopsicossocial", g, Referencia.inicio("E51"), 1, "1-2", 1, 16));
        e.add(etapa("E53", "Publicação do resultado definitivo da avaliação biopsicossocial", g, Referencia.fim("E52"), 7, "5-10", 0, 16).resultadoFinal());

        e.add(etapa("E60", "Resultado Final", ENCERRAMENTO, Referencia.ULTIMO_RESULTADO, 4, "1-40", 0, 36));

        Map<String, String> modulos = new LinkedHashMap<>();
        modulos.put("S12", "Discursiva");
        modulos.put("S13", "Títulos");
        modulos.put("S14", "TAF");
        modulos.put("S15", "Prática");
        modulos.put("S16", "Saúde");
        modulos.put("S05", "Heteroidentificação");

        Map<String, List<String>> atalhos = new LinkedHashMap<>();
        atalhos.put("Básico", List.of("S13", "S05"));
        atalhos.put("Segurança pública", List.of("S12", "S14", "S16", "S05"));
        atalhos.put("Magistério", List.of("S13", "S15", "S05"));
        atalhos.put("Completo", List.copyOf(modulos.keySet()));

        return new Esqueleto(NOME, grupos, e.stream().map(Construcao::construir).toList(), modulos, atalhos);
    }

    private static Construcao etapa(String codigo, String descricao, String grupo, Referencia referencia,
                                    Integer dias, String faixa, int duracao, int base) {
        return new Construcao(codigo, descricao, grupo, referencia, dias, faixa, duracao, base);
    }

    /** Auxiliar de montagem: os campos derivados (tipo de status, parâmetro do recurso) saem da descrição. */
    private static final class Construcao {
        private final String codigo;
        private final String descricao;
        private final String grupo;
        private final Referencia referencia;
        private final Integer dias;
        private final String faixa;
        private final int duracao;
        private final int base;
        private String parametroDias;
        private String parametroDuracao;
        private boolean resultadoFinal;

        Construcao(String codigo, String descricao, String grupo, Referencia referencia, Integer dias,
                   String faixa, int duracao, int base) {
            this.codigo = codigo;
            this.descricao = descricao;
            this.grupo = grupo;
            this.referencia = referencia;
            this.dias = dias;
            this.faixa = faixa;
            this.duracao = duracao;
            this.base = base;
        }

        Construcao comParametros(String dias, String duracao) {
            this.parametroDias = dias;
            this.parametroDuracao = duracao;
            return this;
        }

        Construcao resultadoFinal() {
            this.resultadoFinal = true;
            return this;
        }

        EtapaModelo construir() {
            TipoStatus tipo = RECURSO.matcher(descricao).find() ? TipoStatus.RECURSO
                    : PUBLICACAO.matcher(descricao).find() ? TipoStatus.PUBLICACAO
                    : TipoStatus.ATIVIDADE;
            String duracaoParametro = parametroDuracao == null && tipo == TipoStatus.RECURSO
                    ? RECURSO_DEMAIS : parametroDuracao;
            Integer p25 = null;
            Integer p75 = null;
            if (faixa != null) {
                Matcher m = FAIXA.matcher(faixa);
                if (!m.matches()) {
                    throw new IllegalArgumentException("Faixa inválida em " + codigo + ": " + faixa);
                }
                p25 = Integer.valueOf(m.group(1));
                p75 = Integer.valueOf(m.group(2));
            }
            return new EtapaModelo(codigo, descricao, grupo, referencia, dias, p25, p75, duracao, base, tipo,
                    parametroDias, duracaoParametro, resultadoFinal);
        }
    }
}

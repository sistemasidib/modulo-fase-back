package br.bioregistro.fase.application.controllers;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

/**
 * Endpoints do cronograma (spec 003-B): modelo, leitura, simular, prévia, salvar, histórico,
 * particularidades e status. "Hoje" = 10/10/2026.
 */
@QuarkusTest
@TestProfile(SemBanco.class)
class CronogramaControllerTest {

    private static final String API = "/v1/idecan";
    /** Edital 03/12/2026: prova sugerida 17/02/2027 (qua), RM-01 = 34 dias, sem violação. */
    private static final String EDICAO_OK =
            "{\"modulos\": [], \"dataEdital\": \"2026-12-03\", \"ajustes\": {}, \"removidas\": [], \"excecoes\": []}";
    /** Edital 01/12/2026 com a prova 20 dias depois das inscrições (exceção): RM-01 = 26 dias, viola. */
    private static final String EDICAO_VIOLA =
            "{\"modulos\": [], \"dataEdital\": \"2026-12-01\", \"ajustes\": {\"E15\": {\"dias\": 20, \"duracao\": null}},"
                    + " \"removidas\": [], \"excecoes\": [\"intervalo_prova\"]}";

    private int id;

    @BeforeEach
    void cronogramaNovo() {
        ApiEmMemoria.instalar();
        id = given().when().post(API + "/editais/10/cronograma")
                .then().statusCode(201).extract().path("id");
    }

    private static String salvar(int versao, String edicao, String justificativa) {
        return "{\"versao\": " + versao + ", \"edicao\": " + edicao + ", \"acoes\": [], \"justificativa\": "
                + (justificativa == null ? "null" : "\"" + justificativa + "\"") + "}";
    }

    private void salvarOk() {
        given().contentType(ContentType.JSON).body(salvar(0, EDICAO_OK, null))
                .when().put(API + "/cronogramas/" + id).then().statusCode(200);
    }

    // ── modelo e leitura ──────────────────────────────────────────────────────

    @Test
    void modeloTraz54EtapasModulosEParametros() {
        given().when().get(API + "/cronograma/modelo")
                .then().statusCode(200)
                .body("nome", is("Esqueleto IDECAN v1"))
                .body("etapas", hasSize(54))
                .body("modulos", hasSize(6))
                .body("modulos.find { it.codigo == 'S12' }.etapas", is(3))
                .body("parametros.find { it.chave == 'gabarito_apos_prova' }.valor", is(1))
                .body("parametros.find { it.chave == 'recurso_gabarito' }.valor", is(2))
                .body("atalhos.'Segurança pública'", hasItem("S14"));
    }

    @Test
    void cronogramaNovoEhRascunhoSoComONucleo() {
        given().when().get(API + "/cronogramas/" + id)
                .then().statusCode(200)
                .body("situacao", is("RASCUNHO"))
                .body("versao", is(0))
                .body("calculado.indicadores.etapasAtivas", is(26))
                .body("calculado.grupos[0].codigo", is("EDITAL_INSCRICOES"))
                .body("calculado.grupos[1].codigo", is("F1_PROVA_OBJETIVA"))
                .body("calculado.grupos[1].etapas.codigo", is(java.util.List.of("E14", "E15", "E16", "E17", "E18", "E19", "E20")))
                .body("status", hasSize(54));
    }

    @Test
    void cronogramaInexistenteEh404() {
        given().when().get(API + "/cronogramas/999").then().statusCode(404);
        given().when().get(API + "/cronogramas/999/historico").then().statusCode(404);
    }

    // ── simular e prévia ──────────────────────────────────────────────────────

    @Test
    void simularCalculaGabaritoERecursoSemGravar() {
        given().contentType(ContentType.JSON).body(EDICAO_OK)
                .when().post(API + "/cronogramas/" + id + "/simular")
                .then().statusCode(200)
                .body("materializado", is(true))
                .body("provaInformada", is(false))
                .body("indicadores.prova", is("2027-02-17"))
                .body("grupos.etapas.flatten().find { it.codigo == 'E14' }.inicio", is("2027-02-08"))
                .body("grupos.etapas.flatten().find { it.codigo == 'E16' }.inicio", is("2027-02-18"))
                .body("grupos.etapas.flatten().find { it.codigo == 'E17' }.inicio", is("2027-02-18"))
                .body("grupos.etapas.flatten().find { it.codigo == 'E17' }.fim", is("2027-02-20"))
                .body("checagens.viola", is(false))
                .body("checagens.alertas.find { it.codigo == 'PROVA_INFORMADA' }.cumprida", is(false));

        given().when().get(API + "/cronogramas/" + id).then().body("situacao", is("RASCUNHO")).body("versao", is(0));
    }

    /** Spec 005, RN09: a data da prova informada vale como está, mesmo no sábado. */
    @Test
    void simularComDataDaProvaInformada() {
        given().contentType(ContentType.JSON).body(EDICAO_OK.replace("\"excecoes\": []}", "\"excecoes\": [], \"dataProva\": \"2027-02-20\"}"))
                .when().post(API + "/cronogramas/" + id + "/simular")
                .then().statusCode(200)
                .body("provaInformada", is(true))
                .body("indicadores.prova", is("2027-02-20"))
                .body("grupos.etapas.flatten().find { it.codigo == 'E16' }.inicio", is("2027-02-21"));
    }

    @Test
    void simularComModuloInvalidoEh400() {
        given().contentType(ContentType.JSON)
                .body("{\"modulos\": [\"S99\"], \"dataEdital\": null, \"ajustes\": {}, \"removidas\": [], \"excecoes\": []}")
                .when().post(API + "/cronogramas/" + id + "/simular")
                .then().statusCode(400).body("message", containsString("S99"));
    }

    @Test
    void previaEmCascata() {
        String mudanca = "{\"codigo\": \"E04\", \"campo\": \"DURACAO\", \"valor\": 25}";
        given().contentType(ContentType.JSON)
                .body("{\"edicao\": " + EDICAO_VIOLA + ", \"mudanca\": " + mudanca + "}")
                .when().post(API + "/cronogramas/" + id + "/previa")
                .then().statusCode(200)
                .body("etapasMovidas", greaterThan(0))
                .body("itens[0].etapaCodigo", is("E04"))
                .body("itens[0].antes", is("41"))
                .body("edicao.ajustes.E04.duracao", is(25));
    }

    @Test
    void previaDePrazoPadraoTravadoEh400() {
        given().contentType(ContentType.JSON)
                .body("{\"edicao\": " + EDICAO_OK
                        + ", \"mudanca\": {\"codigo\": \"E05\", \"campo\": \"DURACAO\", \"valor\": 3}}")
                .when().post(API + "/cronogramas/" + id + "/previa")
                .then().statusCode(400).body("message", containsString("exceção"));
    }

    @Test
    void previaSemMudancaEh400() {
        given().contentType(ContentType.JSON).body("{\"edicao\": " + EDICAO_OK + "}")
                .when().post(API + "/cronogramas/" + id + "/previa")
                .then().statusCode(400);
    }

    // ── salvar ────────────────────────────────────────────────────────────────

    @Test
    void salvarMaterializaESobeAVersao() {
        given().contentType(ContentType.JSON).header("X-Usuario", "Yuri").body(salvar(0, EDICAO_OK, null))
                .when().put(API + "/cronogramas/" + id)
                .then().statusCode(200)
                .body("situacao", is("MATERIALIZADO"))
                .body("versao", is(1))
                .body("edicao.dataEdital", is("2026-12-03"));

        given().when().get(API + "/cronogramas/" + id + "/historico")
                .then().statusCode(200)
                .body("[0].acao", is("Salvou o cronograma"))
                .body("[0].autor", is("Yuri"))
                .body("[0].origem", is("salvamento"));
    }

    @Test
    void violacaoExigeJustificativa() {
        given().contentType(ContentType.JSON).body(salvar(0, EDICAO_VIOLA, null))
                .when().put(API + "/cronogramas/" + id)
                .then().statusCode(400).body("message", containsString("justificativa"));

        given().contentType(ContentType.JSON).body(salvar(0, EDICAO_VIOLA, "Pedido do órgão"))
                .when().put(API + "/cronogramas/" + id)
                .then().statusCode(200)
                .body("justificativa", is("Pedido do órgão"))
                .body("calculado.checagens.viola", is(true));
    }

    /** Spec 005, RN08: edital no passado salva; só alerta. */
    @Test
    void editalNoPassadoSalvaComAlerta() {
        given().contentType(ContentType.JSON)
                .body(salvar(0, EDICAO_OK.replace("2026-12-03", "2026-10-09"), null))
                .when().put(API + "/cronogramas/" + id)
                .then().statusCode(200)
                .body("calculado.checagens.viola", is(false))
                .body("calculado.checagens.alertas.find { it.codigo == 'EDITAL_NO_PRAZO' }.cumprida", is(false));
    }

    @Test
    void ancoraRemovidaEh400() {
        given().contentType(ContentType.JSON)
                .body(salvar(0, EDICAO_OK.replace("\"removidas\": []", "\"removidas\": [\"E15\"]"), null))
                .when().put(API + "/cronogramas/" + id)
                .then().statusCode(400).body("message", containsString("E15"));
    }

    @Test
    void versaoDesatualizadaEh409() {
        salvarOk();
        given().contentType(ContentType.JSON).body(salvar(0, EDICAO_OK, null))
                .when().put(API + "/cronogramas/" + id)
                .then().statusCode(409).body("message", containsString("Recarregue"));
    }

    @Test
    void acoesDaSessaoVaoParaOHistoricoComItens() {
        String corpo = "{\"versao\": 0, \"edicao\": " + EDICAO_OK + ", \"justificativa\": null, \"acoes\": [{"
                + "\"acao\": \"Alterou com cascata · E04\", \"detalhe\": \"duração: 31 → 25\","
                + "\"origem\": \"edição manual + recálculo\","
                + "\"itens\": [{\"etapaCodigo\": \"E14\", \"campo\": \"data\", \"antes\": \"a\", \"depois\": \"b\"},"
                + "{\"etapaCodigo\": \"E15\", \"campo\": \"data\", \"antes\": \"c\", \"depois\": \"d\"}]}]}";
        given().contentType(ContentType.JSON).body(corpo).when().put(API + "/cronogramas/" + id).then().statusCode(200);

        given().when().get(API + "/cronogramas/" + id + "/historico")
                .then().statusCode(200)
                .body("find { it.acao == 'Alterou com cascata · E04' }.itens", hasSize(2));
    }

    // ── itens personalizados ──────────────────────────────────────────────────

    private static final String ITEM_P01 = "{\"codigo\": \"P01\", \"descricao\": \"Reunião com a comissão\","
            + " \"grupo\": \"EDITAL_INSCRICOES\", \"referencia\": {\"tipo\": \"FIM\", \"codigo\": \"E04\"},"
            + " \"dias\": 2, \"duracao\": 1, \"tipoStatus\": \"ATIVIDADE\"}";

    private static String comItens(String... itens) {
        return EDICAO_OK.substring(0, EDICAO_OK.lastIndexOf('}')) + ", \"itens\": [" + String.join(",", itens) + "]}";
    }

    @Test
    void adicionaEditaEExcluiItemPersonalizado() {
        given().contentType(ContentType.JSON).body(salvar(0, comItens(ITEM_P01), null))
                .when().put(API + "/cronogramas/" + id)
                .then().statusCode(200)
                .body("edicao.itens", hasSize(1))
                .body("edicao.itens[0].referencia.codigo", is("E04"))
                .body("calculado.indicadores.etapasTotal", is(55))
                .body("calculado.grupos[0].etapas.find { it.codigo == 'P01' }.personalizada", is(true))
                .body("calculado.grupos[0].etapas.find { it.codigo == 'P01' }.referencia", is("fim de E04"))
                .body("status", hasSize(55));

        // Spec 006 RN09b: status de publicação não faz parte do cronograma público
        given().contentType(ContentType.JSON).body("{\"status\": \"FEITO\"}")
                .when().put(API + "/cronogramas/" + id + "/etapas/P01/status").then().statusCode(409);

        String editado = ITEM_P01.replace("Reunião com a comissão", "Reunião com o órgão").replace("\"dias\": 2", "\"dias\": 5");
        given().contentType(ContentType.JSON).body(salvar(1, comItens(editado), null))
                .when().put(API + "/cronogramas/" + id)
                .then().statusCode(200)
                .body("edicao.itens[0].descricao", is("Reunião com o órgão"))
                .body("edicao.itens[0].dias", is(5));

        given().contentType(ContentType.JSON).body(salvar(2, comItens(), null))
                .when().put(API + "/cronogramas/" + id)
                .then().statusCode(200)
                .body("edicao.itens", hasSize(0))
                .body("calculado.indicadores.etapasTotal", is(54))
                .body("status", hasSize(54));
    }

    @Test
    void itemPersonalizadoInvalidoEh400() {
        given().contentType(ContentType.JSON)
                .body(salvar(0, comItens(ITEM_P01.replace("E04", "E99")), null))
                .when().put(API + "/cronogramas/" + id)
                .then().statusCode(400).body("message", containsString("E99"));
        given().contentType(ContentType.JSON)
                .body(salvar(0, comItens(ITEM_P01, ITEM_P01), null))
                .when().put(API + "/cronogramas/" + id)
                .then().statusCode(400).body("message", containsString("P01"));
    }

    @Test
    void origemInvalidaEh400() {
        String corpo = "{\"versao\": 0, \"edicao\": " + EDICAO_OK
                + ", \"acoes\": [{\"acao\": \"x\", \"origem\": \"inventada\", \"itens\": []}]}";
        given().contentType(ContentType.JSON).body(corpo).when().put(API + "/cronogramas/" + id).then().statusCode(400);
    }

    // ── particularidades ──────────────────────────────────────────────────────

    @Test
    void adicionaERemoveParticularidade() {
        int pid = given().contentType(ContentType.JSON)
                .body("{\"texto\": \"Faltosos do processo interno não são eliminados\", \"assunto\": \"Prova objetiva\", \"etapaCodigo\": \"E15\"}")
                .when().post(API + "/cronogramas/" + id + "/particularidades")
                .then().statusCode(200)
                .body("particularidades", hasSize(1))
                .body("particularidades[0].etapaCodigo", is("E15"))
                .extract().path("particularidades[0].id");

        given().when().delete(API + "/cronogramas/" + id + "/particularidades/" + pid)
                .then().statusCode(200).body("particularidades", hasSize(0));

        given().when().get(API + "/cronogramas/" + id + "/historico")
                .then().body("acao", hasItem(containsString("Removeu particularidade")));
    }

    @Test
    void particularidadeInvalidaOuInexistente() {
        given().contentType(ContentType.JSON).body("{\"texto\": \"x\", \"assunto\": \"Qualquer\"}")
                .when().post(API + "/cronogramas/" + id + "/particularidades").then().statusCode(400);
        given().contentType(ContentType.JSON).body("{\"texto\": \"x\", \"assunto\": \"Outro\", \"etapaCodigo\": \"E99\"}")
                .when().post(API + "/cronogramas/" + id + "/particularidades").then().statusCode(404);
        given().when().delete(API + "/cronogramas/" + id + "/particularidades/999").then().statusCode(404);
    }

    // ── status ────────────────────────────────────────────────────────────────

    @Test
    void marcaStatusPeloFluxo() {
        salvarOk();
        // Spec 006 RN09b: cronograma público não marca publicação
        given().contentType(ContentType.JSON).body("{\"status\": \"FEITO\"}")
                .when().put(API + "/cronogramas/" + id + "/etapas/E04/status")
                .then().statusCode(409).body("message", containsString("não marca publicação"));
        given().contentType(ContentType.JSON).body("{\"status\": \"PRONTO_PARA_PUBLICAR\"}")
                .when().put(API + "/cronogramas/" + id + "/etapas/E16/status")
                .then().statusCode(409);
    }

    @Test
    void recursoSoEntraEmAnalisePeloGatilhoDepoisDeAbrir() {
        salvarOk();
        given().contentType(ContentType.JSON).body("{\"status\": \"EM_ANALISE\"}")
                .when().put(API + "/cronogramas/" + id + "/etapas/E17/status").then().statusCode(409);
        given().when().post(API + "/cronogramas/" + id + "/etapas/E17/primeiro-recurso")
                .then().statusCode(409).body("message", containsString("não registra"));
    }

    @Test
    void statusDeEtapaInexistenteOuSemCorpo() {
        given().contentType(ContentType.JSON).body("{\"status\": \"FEITO\"}")
                .when().put(API + "/cronogramas/" + id + "/etapas/E99/status").then().statusCode(409);
        given().contentType(ContentType.JSON).body("{}")
                .when().put(API + "/cronogramas/" + id + "/etapas/E04/status").then().statusCode(409);
        given().when().post(API + "/cronogramas/" + id + "/etapas/E04/primeiro-recurso")
                .then().statusCode(409);
    }

    @Test
    void historicoNaoTemEdicaoNemExclusao() {
        given().when().delete(API + "/cronogramas/" + id + "/historico").then().statusCode(not(200));
        given().contentType(ContentType.JSON).body("{}").when().put(API + "/cronogramas/" + id + "/historico")
                .then().statusCode(not(200));
    }
}

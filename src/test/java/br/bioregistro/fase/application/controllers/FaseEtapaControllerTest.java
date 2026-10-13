package br.bioregistro.fase.application.controllers;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static br.bioregistro.fase.application.use_cases.TiposEmMemoria.ANALISE_PCD;
import static br.bioregistro.fase.application.use_cases.TiposEmMemoria.PROVA_OBJETIVA;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

/** {@code /v1/{instituicao}/fases} e {@code /v1/{instituicao}/etapas}: os 10 endpoints (spec 002). */
@QuarkusTest
@TestProfile(SemBanco.class)
class FaseEtapaControllerTest {

    private static final String FASES = "/v1/idecan/fases";
    private static final String ETAPAS = "/v1/idecan/etapas";
    private static final String ABERTURA = "2026-10-01T00:00:00-03:00";
    private static final String FECHAMENTO = "2026-10-31T23:59:00-03:00";

    @BeforeEach
    void dados() {
        ApiEmMemoria.instalar();
    }

    static String fase(String nome, int ordem) {
        return "{\"nome\": \"" + nome + "\", \"ordem\": " + ordem + ", \"dataAbertura\": \"" + ABERTURA
                + "\", \"dataFechamento\": \"" + FECHAMENTO + "\"}";
    }

    static int criarFase(int editalId, String nome, int ordem) {
        return given().contentType(ContentType.JSON).queryParam("editalId", editalId).body(fase(nome, ordem))
                .when().post(FASES).then().statusCode(201).extract().path("id");
    }

    static int criarEtapa(int faseId, String nome, int tipoId) {
        return given().contentType(ContentType.JSON).queryParam("faseId", faseId)
                .body("{\"nome\": \"" + nome + "\", \"tipoId\": " + tipoId + "}")
                .when().post(ETAPAS).then().statusCode(201).extract().path("id");
    }

    // ── fases ─────────────────────────────────────────────────────────────────

    @Test
    void criaListaBuscaAtualizaEExcluiFase() {
        int id = criarFase(10, "Objetiva", 1);
        criarFase(10, "Discursiva", 2);

        given().queryParam("editalId", 10).when().get(FASES)
                .then().statusCode(200).body("$", hasSize(2)).body("[0].ordem", is(1));

        given().when().get(FASES + "/" + id).then().statusCode(200).body("nome", is("Objetiva")).body("editalId", is(10));

        given().contentType(ContentType.JSON).body(fase("Objetiva geral", 1))
                .when().put(FASES + "/" + id).then().statusCode(200).body("nome", is("Objetiva geral"));

        given().when().delete(FASES + "/" + id).then().statusCode(204);
        given().when().get(FASES + "/" + id).then().statusCode(404);
    }

    @Test
    void faseValidaDadosEOrdem() {
        given().contentType(ContentType.JSON).queryParam("editalId", 10).body("{\"nome\": \"Sem datas\", \"ordem\": 1}")
                .when().post(FASES).then().statusCode(400);
        given().contentType(ContentType.JSON).body(fase("Sem edital", 1))
                .when().post(FASES).then().statusCode(400).body("message", containsString("editalId"));
        given().contentType(ContentType.JSON).queryParam("editalId", 999).body(fase("X", 1))
                .when().post(FASES).then().statusCode(404);

        criarFase(10, "Objetiva", 1);
        given().contentType(ContentType.JSON).queryParam("editalId", 10).body(fase("Outra", 1))
                .when().post(FASES).then().statusCode(409);
    }

    @Test
    void listarFasesExigeEditalOuGrupo() {
        given().when().get(FASES).then().statusCode(400);
    }

    @Test
    void faseInexistenteEh404() {
        given().when().get(FASES + "/999").then().statusCode(404);
        given().contentType(ContentType.JSON).body(fase("X", 1)).when().put(FASES + "/999").then().statusCode(404);
        given().when().delete(FASES + "/999").then().statusCode(404);
    }

    // ── etapas ────────────────────────────────────────────────────────────────

    @Test
    void criaListaBuscaAtualizaEExcluiEtapa() {
        int faseId = criarFase(10, "Objetiva", 1);
        int id = criarEtapa(faseId, "Prova", PROVA_OBJETIVA);

        given().queryParam("faseId", faseId).when().get(ETAPAS)
                .then().statusCode(200).body("$", hasSize(1)).body("[0].nome", is("Prova"));

        given().when().get(ETAPAS + "/" + id)
                .then().statusCode(200)
                .body("faseId", is(faseId))
                .body("dataAbertura", nullValue())
                .body("reprovavel", is(false));

        given().contentType(ContentType.JSON).body("{\"nome\": \"Prova objetiva\", \"tipoId\": " + PROVA_OBJETIVA + "}")
                .when().put(ETAPAS + "/" + id).then().statusCode(200).body("nome", is("Prova objetiva"));

        given().when().delete(ETAPAS + "/" + id).then().statusCode(204);
        given().when().get(ETAPAS + "/" + id).then().statusCode(404);
    }

    @Test
    void etapaValidaNomeTipoEFase() {
        int faseId = criarFase(10, "Objetiva", 1);
        given().contentType(ContentType.JSON).queryParam("faseId", faseId).body("{\"nome\": \"Sem tipo\"}")
                .when().post(ETAPAS).then().statusCode(400);
        given().contentType(ContentType.JSON).queryParam("faseId", 999)
                .body("{\"nome\": \"X\", \"tipoId\": " + PROVA_OBJETIVA + "}")
                .when().post(ETAPAS).then().statusCode(404);
        given().when().get(ETAPAS).then().statusCode(400);
    }

    @Test
    void etapaDeAnaliseDeSolicitacoesEhRecusada() { // spec 003, RN05
        int faseId = criarFase(10, "Objetiva", 1);
        given().contentType(ContentType.JSON).queryParam("faseId", faseId)
                .body("{\"nome\": \"PcD\", \"tipoId\": " + ANALISE_PCD + "}")
                .when().post(ETAPAS)
                .then().statusCode(400).body("message", containsString("análise de solicitações"));
    }

    @Test
    void preRequisitoDaEtapaPrecisaExistir() {
        int faseId = criarFase(10, "Objetiva", 1);
        given().contentType(ContentType.JSON).queryParam("faseId", faseId)
                .body("{\"nome\": \"X\", \"tipoId\": " + PROVA_OBJETIVA
                        + ", \"elegibilidadeEtapaId\": 999, \"elegibilidadeStatusId\": 3}")
                .when().post(ETAPAS).then().statusCode(404);
    }

    @Test
    void preRequisitoVemComOStatusExigido() {
        int faseId = criarFase(10, "Objetiva", 1);
        given().contentType(ContentType.JSON).queryParam("faseId", faseId)
                .body("{\"nome\": \"X\", \"tipoId\": " + PROVA_OBJETIVA + ", \"elegibilidadeEtapaId\": 999}")
                .when().post(ETAPAS).then().statusCode(400)
                .body("message", containsString("status exigido"));
    }

    @Test
    void etapaInexistenteEh404() {
        given().when().get(ETAPAS + "/999").then().statusCode(404);
        given().contentType(ContentType.JSON).body("{\"nome\": \"X\", \"tipoId\": 1}")
                .when().put(ETAPAS + "/999").then().statusCode(404);
        given().when().delete(ETAPAS + "/999").then().statusCode(404);
    }
}

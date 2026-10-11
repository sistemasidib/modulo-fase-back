package br.bioregistro.fase.application.controllers;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static br.bioregistro.fase.application.use_cases.TiposEmMemoria.ANALISE_PCD;
import static br.bioregistro.fase.application.use_cases.TiposEmMemoria.PROVA_OBJETIVA;
import static br.bioregistro.fase.application.use_cases.TiposEmMemoria.TAF;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

/** {@code /v1/{instituicao}/grupos-cargo}: os 8 endpoints (spec 001). Edital 10 tem os cargos 101–103. */
@QuarkusTest
@TestProfile(SemBanco.class)
class GrupoCargoControllerTest {

    private static final String BASE = "/v1/idecan/grupos-cargo";

    @BeforeEach
    void dados() {
        ApiEmMemoria.instalar();
    }

    static int criarGrupo(String nome) {
        return given().contentType(ContentType.JSON).queryParam("editalId", 10).body("{\"nome\": \"" + nome + "\"}")
                .when().post(BASE).then().statusCode(201).extract().path("id");
    }

    static String gerar(String faseNome, int... tipos) {
        StringBuilder etapas = new StringBuilder();
        for (int t : tipos) {
            if (!etapas.isEmpty()) etapas.append(",");
            etapas.append("{\"tipoId\": ").append(t).append("}");
        }
        return "{\"fases\": [{\"nome\": \"" + faseNome + "\", \"dataAbertura\": \"2026-10-01T00:00:00-03:00\","
                + " \"dataFechamento\": \"2026-10-31T23:59:00-03:00\", \"etapas\": [" + etapas + "]}]}";
    }

    @Test
    void criaListaERenomeiaGrupo() {
        int id = criarGrupo("Nível médio");
        given().queryParam("editalId", 10).when().get(BASE)
                .then().statusCode(200).body("$", hasSize(1)).body("[0].nome", is("Nível médio"));

        given().contentType(ContentType.JSON).body("{\"nome\": \"Nível superior\"}")
                .when().put(BASE + "/" + id).then().statusCode(200).body("nome", is("Nível superior"));
    }

    @Test
    void nomeRepetidoEh409EEditalObrigatorio() {
        criarGrupo("Nível médio");
        given().contentType(ContentType.JSON).queryParam("editalId", 10).body("{\"nome\": \"Nível médio\"}")
                .when().post(BASE).then().statusCode(409);
        given().when().get(BASE).then().statusCode(400);
        given().contentType(ContentType.JSON).queryParam("editalId", 999).body("{\"nome\": \"X\"}")
                .when().post(BASE).then().statusCode(404);
    }

    @Test
    void adicionaERemoveCargoECargosSemGrupo() {
        int id = criarGrupo("Nível médio");
        given().when().put(BASE + "/" + id + "/cargos/101")
                .then().statusCode(200).body("cargoIds", hasItem(101));

        given().queryParam("editalId", 10).when().get(BASE + "/cargos-sem-grupo")
                .then().statusCode(200).body("id", not(hasItem(101))).body("id", hasItem(102));

        given().when().delete(BASE + "/" + id + "/cargos/101")
                .then().statusCode(200).body("cargoIds", not(hasItem(101)));
    }

    @Test
    void cargoNaoEntraEmDoisGrupos() {
        int a = criarGrupo("A");
        int b = criarGrupo("B");
        given().when().put(BASE + "/" + a + "/cargos/101").then().statusCode(200);
        given().when().put(BASE + "/" + b + "/cargos/101").then().statusCode(409);
    }

    @Test
    void geraFasesEListaPorGrupo() {
        int id = criarGrupo("Nível médio");
        given().when().put(BASE + "/" + id + "/cargos/101").then().statusCode(200);

        given().contentType(ContentType.JSON).body(gerar("Fase 1", PROVA_OBJETIVA, TAF))
                .when().post(BASE + "/" + id + "/gerar-fases")
                .then().statusCode(201)
                .body("$", hasSize(1))
                .body("[0].fase.nome", is("Fase 1"))
                .body("[0].etapas", hasSize(2));

        given().queryParam("grupoCargoId", id).when().get("/v1/idecan/fases")
                .then().statusCode(200).body("$", hasSize(1));
    }

    @Test
    void gerarFasesRecusaGrupoVazioEAnalise() {
        int id = criarGrupo("Nível médio");
        given().contentType(ContentType.JSON).body(gerar("Fase 1", PROVA_OBJETIVA))
                .when().post(BASE + "/" + id + "/gerar-fases")
                .then().statusCode(400).body("message", containsString("cargo"));

        given().when().put(BASE + "/" + id + "/cargos/101").then().statusCode(200);
        given().contentType(ContentType.JSON).body(gerar("Fase 1", PROVA_OBJETIVA, ANALISE_PCD))
                .when().post(BASE + "/" + id + "/gerar-fases")
                .then().statusCode(400).body("message", containsString("análise de solicitações"));
    }

    @Test
    void excluiGrupoSemFasesERecusaComFases() {
        int livre = criarGrupo("Livre");
        given().when().delete(BASE + "/" + livre).then().statusCode(204);

        int comFases = criarGrupo("Com fases");
        given().when().put(BASE + "/" + comFases + "/cargos/101").then().statusCode(200);
        given().contentType(ContentType.JSON).body(gerar("Fase 1", PROVA_OBJETIVA))
                .when().post(BASE + "/" + comFases + "/gerar-fases").then().statusCode(201);
        given().when().delete(BASE + "/" + comFases).then().statusCode(409);
    }

    @Test
    void grupoInexistenteEh404() {
        given().contentType(ContentType.JSON).body("{\"nome\": \"X\"}").when().put(BASE + "/999").then().statusCode(404);
        given().when().delete(BASE + "/999").then().statusCode(404);
        given().when().put(BASE + "/999/cargos/101").then().statusCode(404);
    }
}

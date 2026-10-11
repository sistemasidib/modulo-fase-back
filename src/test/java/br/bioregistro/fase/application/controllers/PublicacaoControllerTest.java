package br.bioregistro.fase.application.controllers;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static br.bioregistro.fase.application.use_cases.TiposEmMemoria.PROVA_OBJETIVA;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

/** {@code /v1/{instituicao}/publicacoes}: os 5 endpoints (spec 001, espaços de publicação). */
@QuarkusTest
@TestProfile(SemBanco.class)
class PublicacaoControllerTest {

    private static final String BASE = "/v1/idecan/publicacoes";

    private int faseId;

    @BeforeEach
    void faseComProvaObjetiva() {
        ApiEmMemoria.instalar();
        faseId = FaseEtapaControllerTest.criarFase(10, "Objetiva", 1);
        FaseEtapaControllerTest.criarEtapa(faseId, "Prova", PROVA_OBJETIVA);
    }

    private int espaco(String tipo) {
        return given().queryParam("faseId", faseId).when().get(BASE)
                .then().statusCode(200).extract().path("find { it.tipo == '" + tipo + "' }.id");
    }

    private static String datas(OffsetDateTime visIni, OffsetDateTime visFim, OffsetDateTime recIni, OffsetDateTime recFim) {
        return "{\"visualizacaoInicio\": " + q(visIni) + ", \"visualizacaoFim\": " + q(visFim)
                + ", \"recursoInicio\": " + q(recIni) + ", \"recursoFim\": " + q(recFim) + "}";
    }

    private static String q(OffsetDateTime d) {
        return d == null ? "null" : "\"" + d + "\"";
    }

    @Test
    void etapaDeProvaObjetivaNasceComOsEspacos() {
        given().queryParam("faseId", faseId).when().get(BASE)
                .then().statusCode(200)
                .body("tipo", hasItem("local_prova"))
                .body("tipo", hasItem("gabarito_preliminar"))
                .body("tipo", hasItem("gabarito_definitivo"));
    }

    @Test
    void preencheDatasAntesDePublicar() {
        given().contentType(ContentType.JSON).body("{\"dataPrevista\": \"2026-10-11T09:00:00-03:00\"}")
                .when().put(BASE + "/" + espaco("local_prova"))
                .then().statusCode(200).body("dataPrevista", notNullValue()).body("publicadaEm", is((Object) null));
    }

    @Test
    void publicarGabaritoAbreORecurso() {
        OffsetDateTime agora = OffsetDateTime.now();
        int id = espaco("gabarito_preliminar");

        // gabarito exige o período de recurso
        given().contentType(ContentType.JSON).body(datas(agora, agora.plusDays(30), null, null))
                .when().post(BASE + "/" + id + "/publicar")
                .then().statusCode(400).body("message", containsString("recurso"));

        given().contentType(ContentType.JSON)
                .body(datas(agora.minusDays(1), agora.plusDays(30), agora.minusHours(1), agora.plusDays(2)))
                .when().post(BASE + "/" + id + "/publicar")
                .then().statusCode(200).body("publicadaEm", notNullValue()).body("recursoAberto", is(true));

        // publicado não publica de novo nem é excluído
        given().contentType(ContentType.JSON)
                .body(datas(agora.minusDays(1), agora.plusDays(30), agora.minusHours(1), agora.plusDays(2)))
                .when().post(BASE + "/" + id + "/publicar").then().statusCode(409);
        given().when().delete(BASE + "/" + id).then().statusCode(409);
    }

    @Test
    void localDeProvaNaoAceitaRecurso() {
        OffsetDateTime agora = OffsetDateTime.now();
        given().contentType(ContentType.JSON).body(datas(agora, agora.plusDays(30), agora, agora.plusDays(2)))
                .when().post(BASE + "/" + espaco("local_prova") + "/publicar")
                .then().statusCode(400);
    }

    @Test
    void criaEExcluiEspacoExtraDaFase() {
        int id = given().contentType(ContentType.JSON).queryParam("faseId", faseId)
                .body("{\"tipoPublicacaoId\": 1, \"etapaId\": null}")
                .when().post(BASE)
                .then().statusCode(201).body("etapaId", is((Object) null)).extract().path("id");
        given().when().delete(BASE + "/" + id).then().statusCode(204);
    }

    @Test
    void validaFaseETipo() {
        given().contentType(ContentType.JSON).queryParam("faseId", 999).body("{\"tipoPublicacaoId\": 1}")
                .when().post(BASE).then().statusCode(404);
        given().contentType(ContentType.JSON).queryParam("faseId", faseId).body("{}")
                .when().post(BASE).then().statusCode(400);
        given().when().get(BASE).then().statusCode(400);
    }

    @Test
    void publicacaoInexistenteEh404() {
        given().contentType(ContentType.JSON).body("{}").when().put(BASE + "/999").then().statusCode(404);
        given().contentType(ContentType.JSON).body("{}").when().post(BASE + "/999/publicar").then().statusCode(404);
        given().when().delete(BASE + "/999").then().statusCode(404);
    }
}

package br.bioregistro.fase.application.controllers;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static br.bioregistro.fase.application.controllers.GrupoCargoControllerTest.criarGrupo;
import static br.bioregistro.fase.application.controllers.GrupoCargoControllerTest.gerar;
import static br.bioregistro.fase.application.use_cases.TiposEmMemoria.PROVA_OBJETIVA;
import static br.bioregistro.fase.application.use_cases.TiposEmMemoria.TAF;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

/** {@code /v1/{instituicao}/editais/{editalId}/fases-do-concurso}: derivadas das fases dos grupos. */
@QuarkusTest
@TestProfile(SemBanco.class)
class FaseDoConcursoControllerTest {

    private static final String URL = "/v1/idecan/editais/10/fases-do-concurso";

    @BeforeEach
    void dados() {
        ApiEmMemoria.instalar();
    }

    @Test
    void editalSemFasesNosGruposDevolveListaVazia() {
        criarGrupo("Nível médio");
        given().when().get(URL).then().statusCode(200).body("$", hasSize(0));
    }

    @Test
    void reuneOsGruposDeCadaTipoNaOrdemDasFases() {
        int medio = grupoComCargo("Nível médio", 101);
        int superior = grupoComCargo("Nível superior", 102);
        gerarFase(medio, "Prova objetiva", PROVA_OBJETIVA);
        gerarFase(superior, "Prova objetiva", PROVA_OBJETIVA);
        gerarFase(superior, "TAF", TAF);

        given().when().get(URL).then().statusCode(200)
                .body("tipoId", contains(PROVA_OBJETIVA, TAF))
                .body("ordem", contains(1, 2))
                .body("[0].valor", is("prova_objetiva"))
                .body("[0].grupoIds", contains(medio, superior))
                .body("[1].grupoIds", contains(superior));
    }

    @Test
    void editalInexistenteEh404() {
        given().when().get("/v1/idecan/editais/999/fases-do-concurso").then().statusCode(404);
    }

    private static int grupoComCargo(String nome, int cargoId) {
        int id = criarGrupo(nome);
        given().when().put("/v1/idecan/grupos-cargo/" + id + "/cargos/" + cargoId).then().statusCode(200);
        return id;
    }

    private static void gerarFase(int grupoId, String nome, int tipoId) {
        given().contentType(ContentType.JSON).body(gerar(nome, tipoId))
                .when().post("/v1/idecan/grupos-cargo/" + grupoId + "/gerar-fases").then().statusCode(201);
    }
}

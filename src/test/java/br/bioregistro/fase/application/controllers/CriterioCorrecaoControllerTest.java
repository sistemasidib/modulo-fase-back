package br.bioregistro.fase.application.controllers;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static br.bioregistro.fase.application.controllers.DisciplinaControllerTest.gerarProva;
import static br.bioregistro.fase.application.controllers.DisciplinaControllerTest.grupoComCargos;
import static br.bioregistro.fase.application.use_cases.TiposEmMemoria.PROVA_DISCURSIVA;
import static br.bioregistro.fase.application.use_cases.TiposEmMemoria.PROVA_OBJETIVA;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

/** Critérios de correção da prova discursiva: CRUD, ordem de 1 a n, cópia e recusa na prova objetiva. */
@QuarkusTest
@TestProfile(SemBanco.class)
class CriterioCorrecaoControllerTest {

    private int prova;

    @BeforeEach
    void dados() {
        ApiEmMemoria.instalar();
        prova = gerarProva(grupoComCargos("Nível médio", 101), "Prova discursiva", PROVA_DISCURSIVA);
    }

    private static String urlDa(int etapaId) {
        return "/v1/idecan/etapas/" + etapaId + "/criterios-correcao";
    }

    private static String corpo(String modulo, String criterio, String desconto, String maximo) {
        return """
                {"modulo": "%s", "criterio": "%s", "descontoPorErro": %s, "pontuacaoMaxima": %s}"""
                .formatted(modulo, criterio, desconto, maximo);
    }

    private ValidatableResponse criar(String corpo) {
        return given().contentType(ContentType.JSON).body(corpo).when().post(urlDa(prova)).then();
    }

    @Test
    void criaListaEAtualiza() {
        criar(corpo("Formal", "Colocação pronominal", "0.25", "2.5")).statusCode(201)
                .body("ordem", is(1))
                .body("etapaId", is(prova))
                .body("descontoPorErro", is(0.25f))
                .body("pontuacaoMaxima", is(2.5f));
        int id = criar(corpo("Técnico", "Conhecimento do tema", "null", "1.5")).statusCode(201)
                .body("ordem", is(2))
                .body("descontoPorErro", nullValue())
                .extract().path("id");

        given().contentType(ContentType.JSON).body(corpo("Técnico", "Progressão temática", "null", "1.5"))
                .when().put("/v1/idecan/criterios-correcao/" + id).then().statusCode(200)
                .body("criterio", is("Progressão temática"))
                .body("ordem", is(2));

        given().when().get(urlDa(prova)).then().statusCode(200)
                .body("modulo", contains("Formal", "Técnico"));
    }

    @Test
    void reordenaEExclui() {
        int a = criar(corpo("Formal", "A", "null", "1")).extract().path("id");
        int b = criar(corpo("Textual", "B", "null", "1")).extract().path("id");

        given().contentType(ContentType.JSON).body("{\"ids\": [" + b + ", " + a + "]}")
                .when().put(urlDa(prova) + "/ordem").then().statusCode(200)
                .body("criterio", contains("B", "A"));

        given().when().delete("/v1/idecan/criterios-correcao/" + b).then().statusCode(204);
        given().when().get(urlDa(prova)).then().statusCode(200)
                .body("criterio", contains("A"))
                .body("ordem", contains(1));
    }

    @Test
    void copiaParaOutraDiscursiva() {
        // outra discursiva no mesmo edital (fase do modelo anterior, criada à mão)
        int outra = FaseEtapaControllerTest.criarEtapa(FaseEtapaControllerTest.criarFase(10, "Outra", 9),
                "Prova discursiva", PROVA_DISCURSIVA);
        criar(corpo("Formal", "A", "0.25", "2.5")).statusCode(201);

        given().contentType(ContentType.JSON).body("{\"etapaIds\": [" + outra + "]}")
                .when().post(urlDa(prova) + "/copiar").then().statusCode(204);

        given().when().get(urlDa(outra)).then().statusCode(200)
                .body("$", hasSize(1))
                .body("criterio", contains("A"));
    }

    @Test
    void provaObjetivaEhRecusada() {
        int objetiva = gerarProva(grupoComCargos("Nível superior", 102), "Prova objetiva", PROVA_OBJETIVA);

        given().when().get(urlDa(objetiva)).then().statusCode(400)
                .body("message", containsString("prova discursiva"));
    }

    @Test
    void pontuacaoInvalidaEhRecusada() {
        criar(corpo("Formal", "A", "3", "2.5")).statusCode(400);
        given().when().get(urlDa(999)).then().statusCode(404);
    }
}

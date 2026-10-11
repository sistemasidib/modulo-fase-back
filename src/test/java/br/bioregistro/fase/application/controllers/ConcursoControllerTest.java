package br.bioregistro.fase.application.controllers;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

/** {@code /v1/{instituicao}/concursos}: os 9 endpoints (spec 003-B, RB01, RB02, RB12). */
@QuarkusTest
@TestProfile(SemBanco.class)
class ConcursoControllerTest {

    private static final String BASE = "/v1/idecan/concursos";

    @BeforeEach
    void dados() {
        ApiEmMemoria.instalar();
    }

    static int criarConcurso(String nome) {
        return given().contentType(ContentType.JSON).body("{\"nome\": \"" + nome + "\"}")
                .when().post(BASE)
                .then().statusCode(201).extract().path("id");
    }

    @Test
    void criaEListaConcursos() {
        int id = criarConcurso("PM-XX 2027");
        given().when().get(BASE)
                .then().statusCode(200)
                .body("$", hasSize(1))
                .body("[0].id", is(id))
                .body("[0].nome", is("PM-XX 2027"))
                .body("[0].editais", hasSize(0))
                .body("[0].cronogramaId", nullValue());
    }

    @Test
    void criarSemNomeEh400ENomeRepetidoEh409() {
        given().contentType(ContentType.JSON).body("{\"nome\": \"  \"}")
                .when().post(BASE).then().statusCode(400).body("message", containsString("nome"));
        given().contentType(ContentType.JSON)
                .when().post(BASE).then().statusCode(400);
        criarConcurso("PM-XX 2027");
        given().contentType(ContentType.JSON).body("{\"nome\": \"PM-XX 2027\"}")
                .when().post(BASE).then().statusCode(409);
    }

    @Test
    void buscaRenomeiaEExclui() {
        int id = criarConcurso("PM-XX 2027");
        given().when().get(BASE + "/" + id).then().statusCode(200).body("nome", is("PM-XX 2027"));

        given().contentType(ContentType.JSON).body("{\"nome\": \"PM-XX 2028\"}")
                .when().put(BASE + "/" + id)
                .then().statusCode(200).body("nome", is("PM-XX 2028"));

        given().when().delete(BASE + "/" + id).then().statusCode(204);
        given().when().get(BASE + "/" + id).then().statusCode(404);
    }

    @Test
    void concursoInexistenteEh404() {
        given().when().get(BASE + "/999").then().statusCode(404).body("message", containsString("999"));
        given().contentType(ContentType.JSON).body("{\"nome\": \"X\"}").when().put(BASE + "/999").then().statusCode(404);
        given().when().delete(BASE + "/999").then().statusCode(404);
    }

    @Test
    void agrupaERetiraEditais() {
        int id = criarConcurso("PM-XX 2027");
        given().when().put(BASE + "/" + id + "/editais/10")
                .then().statusCode(200)
                .body("editais", hasSize(1))
                .body("editais[0].id", is(10))
                .body("editais[0].nome", is("PM-XX 2027"));

        // incluir de novo no mesmo concurso não duplica
        given().when().put(BASE + "/" + id + "/editais/10").then().statusCode(200).body("editais", hasSize(1));

        given().when().delete(BASE + "/" + id + "/editais/10")
                .then().statusCode(200).body("editais", hasSize(0));
    }

    @Test
    void editalEmOutroConcursoEh409ComONome() {
        int a = criarConcurso("PM-XX 2027");
        int b = criarConcurso("Outro");
        given().when().put(BASE + "/" + a + "/editais/10").then().statusCode(200);
        given().when().put(BASE + "/" + b + "/editais/10")
                .then().statusCode(409).body("message", containsString("PM-XX 2027"));
    }

    @Test
    void editalInexistenteOuDeOutraInstituicaoEh404() {
        int id = criarConcurso("PM-XX 2027");
        given().when().put(BASE + "/" + id + "/editais/999").then().statusCode(404);
        // o edital 20 existe só no IDIB
        given().when().put(BASE + "/" + id + "/editais/20").then().statusCode(404);
    }

    @Test
    void listaEditaisSemConcursoComSugestao() {
        int id = criarConcurso("PM-XX 2027");
        given().when().put(BASE + "/" + id + "/editais/10").then().statusCode(200);

        given().when().get(BASE + "/editais-sem-concurso")
                .then().statusCode(200)
                .body("$", hasSize(1))
                .body("[0].id", is(11))
                .body("[0].concursoSugeridoId", is(id))
                .body("[0].concursoSugeridoNome", is("PM-XX 2027"));
    }

    @Test
    void cadaInstituicaoTemSeusConcursos() {
        criarConcurso("PM-XX 2027");
        given().when().get("/v1/idib/concursos").then().statusCode(200).body("$", hasSize(0));
    }

    @Test
    void abrirCronogramaCriaUmaVezEDepoisDevolveOMesmo() {
        int id = criarConcurso("PM-XX 2027");
        int cronogramaId = given().header("X-Usuario", "Jo%C3%A3o")
                .when().post(BASE + "/" + id + "/cronograma")
                .then().statusCode(201).body("criado", is(true)).extract().path("id");

        given().when().post(BASE + "/" + id + "/cronograma")
                .then().statusCode(200).body("criado", is(false)).body("id", is(cronogramaId));

        given().when().get(BASE + "/" + id).then().body("cronogramaId", is(cronogramaId));
        // autor vem do header, decodificado
        given().when().get("/v1/idecan/cronogramas/" + cronogramaId + "/historico")
                .then().body("[0].autor", is("João")).body("[0].origem", is("geração automática"));
    }

    @Test
    void concursoComCronogramaNaoPodeSerExcluido() {
        int id = criarConcurso("PM-XX 2027");
        given().when().post(BASE + "/" + id + "/cronograma").then().statusCode(201);
        given().when().delete(BASE + "/" + id)
                .then().statusCode(409).body("message", notNullValue());
    }

    @Test
    void abrirCronogramaDeConcursoInexistenteEh404() {
        given().when().post(BASE + "/999/cronograma").then().statusCode(404);
    }
}

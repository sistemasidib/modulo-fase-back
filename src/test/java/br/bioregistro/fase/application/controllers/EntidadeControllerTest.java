package br.bioregistro.fase.application.controllers;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

/**
 * {@code /v1/{instituicao}/entidades} e o cronograma aberto a partir do edital
 * ({@code /v1/{instituicao}/editais/{editalId}/cronograma}).
 */
@QuarkusTest
@TestProfile(SemBanco.class)
class EntidadeControllerTest {

    private static final String BASE = "/v1/idecan/entidades";

    @BeforeEach
    void dados() {
        ApiEmMemoria.instalar();
    }

    static int criarEntidade(String nome) {
        return given().contentType(ContentType.JSON).body("{\"nome\": \"" + nome + "\"}")
                .when().post(BASE)
                .then().statusCode(201).extract().path("id");
    }

    @Test
    void criaEListaEntidades() {
        int id = criarEntidade("Prefeitura de X");
        given().when().get(BASE)
                .then().statusCode(200)
                .body("$", hasSize(1))
                .body("[0].id", is(id))
                .body("[0].nome", is("Prefeitura de X"))
                .body("[0].cnpj", nullValue())
                .body("[0].editais", hasSize(0))
                .body("[0].cronogramasDosEditais", hasSize(0));
    }

    @Test
    void cnpjOpcionalValidadoEUnico() {
        given().contentType(ContentType.JSON).body("{\"nome\": \"Prefeitura de X\", \"cnpj\": \"11.222.333/0001-81\"}")
                .when().post(BASE)
                .then().statusCode(201).body("cnpj", is("11222333000181"));
        given().contentType(ContentType.JSON).body("{\"nome\": \"Prefeitura de Y\", \"cnpj\": \"11222333000181\"}")
                .when().post(BASE).then().statusCode(409).body("message", containsString("CNPJ"));
        given().contentType(ContentType.JSON).body("{\"nome\": \"Prefeitura de Y\", \"cnpj\": \"11222333000182\"}")
                .when().post(BASE).then().statusCode(400).body("message", containsString("CNPJ inválido"));
    }

    @Test
    void criarSemNomeEh400ENomeRepetidoEh409() {
        given().contentType(ContentType.JSON).body("{\"nome\": \"  \"}")
                .when().post(BASE).then().statusCode(400).body("message", containsString("nome"));
        given().contentType(ContentType.JSON)
                .when().post(BASE).then().statusCode(400);
        criarEntidade("Prefeitura de X");
        given().contentType(ContentType.JSON).body("{\"nome\": \"Prefeitura de X\"}")
                .when().post(BASE).then().statusCode(409);
    }

    @Test
    void buscaEditaEExclui() {
        int id = criarEntidade("Prefeitura de X");
        given().when().get(BASE + "/" + id).then().statusCode(200).body("nome", is("Prefeitura de X"));

        given().contentType(ContentType.JSON).body("{\"nome\": \"Prefeitura Municipal de X\", \"cnpj\": \"12.ABC.345/01DE-35\"}")
                .when().put(BASE + "/" + id)
                .then().statusCode(200).body("nome", is("Prefeitura Municipal de X")).body("cnpj", is("12ABC34501DE35"));

        given().when().delete(BASE + "/" + id).then().statusCode(204);
        given().when().get(BASE + "/" + id).then().statusCode(404);
    }

    @Test
    void entidadeInexistenteEh404() {
        given().when().get(BASE + "/999").then().statusCode(404).body("message", containsString("999"));
        given().contentType(ContentType.JSON).body("{\"nome\": \"X\"}").when().put(BASE + "/999").then().statusCode(404);
        given().when().delete(BASE + "/999").then().statusCode(404);
    }

    @Test
    void agrupaERetiraEditais() {
        int id = criarEntidade("Prefeitura de X");
        given().when().put(BASE + "/" + id + "/editais/10")
                .then().statusCode(200)
                .body("editais", hasSize(1))
                .body("editais[0].id", is(10))
                .body("editais[0].nome", is("PM-XX 2027"));

        // incluir de novo na mesma entidade não duplica
        given().when().put(BASE + "/" + id + "/editais/10").then().statusCode(200).body("editais", hasSize(1));

        given().when().delete(BASE + "/" + id + "/editais/10")
                .then().statusCode(200).body("editais", hasSize(0));
        given().when().delete(BASE + "/" + id + "/editais/10").then().statusCode(404);
    }

    @Test
    void editalEmOutraEntidadeEh409ComONome() {
        int a = criarEntidade("Prefeitura de X");
        int b = criarEntidade("Prefeitura de Y");
        given().when().put(BASE + "/" + a + "/editais/10").then().statusCode(200);
        given().when().put(BASE + "/" + b + "/editais/10")
                .then().statusCode(409).body("message", containsString("Prefeitura de X"));
    }

    @Test
    void editalInexistenteOuDeOutraInstituicaoEh404() {
        int id = criarEntidade("Prefeitura de X");
        given().when().put(BASE + "/" + id + "/editais/999").then().statusCode(404);
        // o edital 20 existe só no IDIB
        given().when().put(BASE + "/" + id + "/editais/20").then().statusCode(404);
    }

    @Test
    void listaEditaisSemEntidadeComSugestao() {
        int id = criarEntidade("Prefeitura de X");
        given().when().put(BASE + "/" + id + "/editais/10").then().statusCode(200);

        given().when().get(BASE + "/editais-sem-entidade")
                .then().statusCode(200)
                .body("$", hasSize(1))
                .body("[0].id", is(11))
                .body("[0].entidadeSugeridaId", is(id))
                .body("[0].entidadeSugeridaNome", is("Prefeitura de X"));
    }

    @Test
    void cadaInstituicaoTemSuasEntidades() {
        criarEntidade("Prefeitura de X");
        given().when().get("/v1/idib/entidades").then().statusCode(200).body("$", hasSize(0));
    }

    @Test
    void abrirCronogramaDoEditalCriaUmaVezEDepoisDevolveOMesmo() {
        int cronogramaId = given().header("X-Usuario", "Jo%C3%A3o")
                .when().post("/v1/idecan/editais/10/cronograma")
                .then().statusCode(201).body("criado", is(true)).extract().path("id");

        given().when().post("/v1/idecan/editais/10/cronograma")
                .then().statusCode(200).body("criado", is(false)).body("id", is(cronogramaId));
        given().when().post("/v1/idecan/editais/999/cronograma").then().statusCode(404);

        // autor vem do header, decodificado
        given().when().get("/v1/idecan/cronogramas/" + cronogramaId + "/historico")
                .then().body("find { it.origem == 'geração automática' }.autor", is("João"));
    }

    /**
     * Uma entidade pode ter vários concursos ao mesmo tempo: cada edital abre o seu cronograma, ou
     * segue o de outro edital; excluir a entidade não mexe nos cronogramas.
     */
    @Test
    void editaisDaEntidadeTemCadaUmOSeuCronograma() {
        int id = criarEntidade("Prefeitura de X");
        given().when().put(BASE + "/" + id + "/editais/10").then().statusCode(200);
        given().when().put(BASE + "/" + id + "/editais/11").then().statusCode(200);
        int deDez = given().when().post("/v1/idecan/editais/10/cronograma").then().statusCode(201)
                .extract().path("id");
        int deOnze = given().when().post("/v1/idecan/editais/11/cronograma").then().statusCode(201)
                .body("id", not(deDez)).extract().path("id");

        given().when().get(BASE + "/" + id).then().statusCode(200)
                .body("cronogramasDosEditais", hasSize(2))
                .body("cronogramasDosEditais.find { it.editalId == 10 }.cronogramaId", is(deDez))
                .body("cronogramasDosEditais.find { it.editalId == 11 }.cronogramaId", is(deOnze));

        given().contentType(ContentType.JSON).body("{\"cronogramaId\": " + deDez + "}")
                .when().put("/v1/idecan/editais/11/cronograma").then().statusCode(200).body("id", is(deDez));
        given().when().get("/v1/idecan/cronogramas/" + deDez).then().statusCode(200)
                .body("editais.editalId", contains(10, 11));

        given().when().delete(BASE + "/" + id).then().statusCode(204);
        given().when().get("/v1/idecan/cronogramas/" + deDez + "?editalId=10").then().statusCode(200);
    }
}

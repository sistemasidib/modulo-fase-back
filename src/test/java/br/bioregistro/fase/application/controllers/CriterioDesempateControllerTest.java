package br.bioregistro.fase.application.controllers;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static br.bioregistro.fase.application.controllers.DisciplinaControllerTest.gerarFase;
import static br.bioregistro.fase.application.controllers.DisciplinaControllerTest.grupoComCargos;
import static br.bioregistro.fase.application.use_cases.TiposEmMemoria.PROVA_OBJETIVA;
import static br.bioregistro.fase.application.use_cases.TiposEmMemoria.TAF;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

/** Critérios de desempate de cada fase: CRUD, ordem sempre de 1 a n e cópia para outras fases. */
@QuarkusTest
@TestProfile(SemBanco.class)
class CriterioDesempateControllerTest {

    private int fase;
    private String url;

    @BeforeEach
    void dados() {
        ApiEmMemoria.instalar();
        fase = gerarFase(grupoComCargos("Nível médio", 101), "Prova objetiva", PROVA_OBJETIVA);
        url = urlDa(fase);
    }

    @Test
    void criaNoFimDaListaENormalizaOsTextos() {
        criar(idoso()).statusCode(201)
                .body("ordem", is(1))
                .body("faseId", is(fase))
                .body("tipo", is("IDADE"))
                .body("faseAplicacao", is("AMBAS"))
                .body("referencia", nullValue());
        criar(disciplina("  Direito Constitucional  ")).statusCode(201)
                .body("ordem", is(2))
                .body("referencia", is("Direito Constitucional"));

        given().when().get(url).then().statusCode(200)
                .body("ordem", contains(1, 2))
                .body("tipo", contains("IDADE", "DISCIPLINA"));
    }

    @Test
    void cadaFaseTemOsSeusCriterios() {
        int outra = gerarFase(grupoComCargos("Nível superior", 102), "TAF", TAF);
        criar(idoso()).statusCode(201);

        given().when().get(urlDa(outra)).then().statusCode(200).body("$", hasSize(0));
        given().contentType(ContentType.JSON).body(sorteio()).when().post(urlDa(outra)).then().statusCode(201)
                .body("ordem", is(1));
        given().when().get(url).then().statusCode(200).body("tipo", contains("IDADE"));
    }

    @Test
    void copiaSubstituindoOsCriteriosDoDestino() {
        int outra = gerarFase(grupoComCargos("Nível superior", 102), "Prova objetiva", PROVA_OBJETIVA);
        given().contentType(ContentType.JSON).body(sorteio()).when().post(urlDa(outra)).then().statusCode(201);
        criar(idoso()).statusCode(201);
        criar(disciplina("Direito Civil")).statusCode(201);

        copiar("[" + outra + "]").statusCode(204);
        given().when().get(urlDa(outra)).then().statusCode(200)
                .body("tipo", contains("IDADE", "DISCIPLINA"))
                .body("ordem", contains(1, 2))
                .body("faseId", contains(outra, outra));
        copiar("[]").statusCode(400);
        copiar("[999]").statusCode(404);
    }

    @Test
    void atualizaSemMudarAOrdem() {
        criar(idoso()).statusCode(201);
        int id = criar(disciplina("Direito Civil")).statusCode(201).extract().path("id");

        given().contentType(ContentType.JSON).body(disciplina("Direito Ambiental"))
                .when().put("/v1/idecan/criterios-desempate/" + id).then().statusCode(200)
                .body("ordem", is(2))
                .body("referencia", is("Direito Ambiental"));
    }

    @Test
    void excluirRenumeraOsSeguintes() {
        int primeiro = criar(idoso()).statusCode(201).extract().path("id");
        criar(disciplina("Direito Civil")).statusCode(201);
        criar(sorteio()).statusCode(201);

        given().when().delete("/v1/idecan/criterios-desempate/" + primeiro).then().statusCode(204);
        given().when().get(url).then().statusCode(200)
                .body("ordem", contains(1, 2))
                .body("tipo", contains("DISCIPLINA", "SORTEIO"));
    }

    @Test
    void reordenaComTodosOsCriterios() {
        int a = criar(idoso()).statusCode(201).extract().path("id");
        int b = criar(disciplina("Direito Civil")).statusCode(201).extract().path("id");
        int c = criar(sorteio()).statusCode(201).extract().path("id");

        reordenar("[" + c + ", " + a + ", " + b + "]").statusCode(200)
                .body("id", contains(c, a, b))
                .body("ordem", contains(1, 2, 3));
        reordenar("[" + a + ", " + b + "]").statusCode(400);
        reordenar("[" + a + ", " + a + ", " + b + "]").statusCode(400);
    }

    @Test
    void validaOsCamposObrigatorios() {
        criar("{\"tipo\": \"NOTA\", \"regra\": \"MAIOR\", \"faseAplicacao\": \"AMBAS\"}").statusCode(400);
        criar("{\"criterio\": \"Maior nota\", \"tipo\": \"DISCIPLINA\", \"regra\": \"MAIOR\", \"faseAplicacao\": \"AMBAS\"}")
                .statusCode(400);
        criar("{\"criterio\": \"x\", \"tipo\": \"QUALQUER\", \"regra\": \"MAIOR\", \"faseAplicacao\": \"AMBAS\"}")
                .statusCode(400);
        given().when().get(url).then().statusCode(200).body("$", hasSize(0));
    }

    @Test
    void faseOuCriterioInexistenteEh404() {
        given().when().get(urlDa(999)).then().statusCode(404);
        given().contentType(ContentType.JSON).body(idoso())
                .when().put("/v1/idecan/criterios-desempate/999").then().statusCode(404);
        given().when().delete("/v1/idecan/criterios-desempate/999").then().statusCode(404);
    }

    @Test
    void excluirAFaseApagaOsCriterios() {
        int id = criar(idoso()).statusCode(201).extract().path("id");
        given().when().delete("/v1/idecan/fases/" + fase).then().statusCode(204);
        given().contentType(ContentType.JSON).body(idoso())
                .when().put("/v1/idecan/criterios-desempate/" + id).then().statusCode(404);
    }

    private static String urlDa(int faseId) {
        return "/v1/idecan/fases/" + faseId + "/criterios-desempate";
    }

    private ValidatableResponse criar(String corpo) {
        return given().contentType(ContentType.JSON).body(corpo).when().post(url).then();
    }

    private ValidatableResponse reordenar(String ids) {
        return given().contentType(ContentType.JSON).body("{\"ids\": " + ids + "}")
                .when().put(url + "/ordem").then();
    }

    private ValidatableResponse copiar(String faseIds) {
        return given().contentType(ContentType.JSON).body("{\"faseIds\": " + faseIds + "}")
                .when().post(url + "/copiar").then();
    }

    private static String idoso() {
        return """
                {"criterio": "Idade igual ou superior a 60 anos", "tipo": "IDADE", "regra": "MAIOR",
                 "faseAplicacao": "AMBAS", "fundamentacao": "Estatuto da Pessoa Idosa, art. 27",
                 "observacao": "Idade no último dia de inscrição", "referencia": "  "}""";
    }

    private static String disciplina(String nome) {
        return """
                {"criterio": "Maior nota na disciplina", "tipo": "DISCIPLINA", "regra": "MAIOR",
                 "faseAplicacao": "PROVA_OBJETIVA", "referencia": "%s", "fundamentacao": "14.3"}""".formatted(nome);
    }

    private static String sorteio() {
        return """
                {"criterio": "Sorteio público", "tipo": "SORTEIO", "regra": "SIM_NAO", "faseAplicacao": "RESULTADO_FINAL"}""";
    }
}

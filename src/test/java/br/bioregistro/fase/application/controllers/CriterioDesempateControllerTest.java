package br.bioregistro.fase.application.controllers;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static br.bioregistro.fase.application.use_cases.TiposEmMemoria.PROVA_OBJETIVA;
import static br.bioregistro.fase.application.use_cases.TiposEmMemoria.TAF;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

/** Critérios de desempate de cada etapa: CRUD, ordem sempre de 1 a n e cópia para outras etapas. */
@QuarkusTest
@TestProfile(SemBanco.class)
class CriterioDesempateControllerTest {

    private int etapa;
    private int fase;
    private String url;

    @BeforeEach
    void dados() {
        ApiEmMemoria.instalar();
        fase = FaseEtapaControllerTest.criarFase(10, "Prova objetiva", 1);
        etapa = FaseEtapaControllerTest.criarEtapa(fase, "Prova objetiva", PROVA_OBJETIVA);
        url = urlDa(etapa);
    }

    @Test
    void criaNoFimDaListaENormalizaOsTextos() {
        criar(idoso()).statusCode(201)
                .body("ordem", is(1))
                .body("etapaId", is(etapa))
                .body("tipo", is("idade"))
                .body("direcao", is("desc"))
                .body("baseLegal", is("idoso"))
                .body("fonte", nullValue());
        criar(notaDisciplina("  conhecimentos_especificos  ")).statusCode(201)
                .body("ordem", is(2))
                .body("fonte", is("conhecimentos_especificos"));

        given().when().get(url).then().statusCode(200)
                .body("ordem", contains(1, 2))
                .body("tipo", contains("idade", "nota"))
                .body("label", contains("Idade igual ou superior a 60 anos", "Maior nota em Conhecimentos Específicos"));
    }

    @Test
    void cadaEtapaTemOsSeusCriterios() {
        int outraFase = FaseEtapaControllerTest.criarFase(10, "TAF", 2);
        int outra = FaseEtapaControllerTest.criarEtapa(outraFase, "TAF", TAF);
        criar(idoso()).statusCode(201);

        given().when().get(urlDa(outra)).then().statusCode(200).body("$", hasSize(0));
        given().contentType(ContentType.JSON).body(jurado()).when().post(urlDa(outra)).then().statusCode(201)
                .body("ordem", is(1));
        given().when().get(url).then().statusCode(200).body("tipo", contains("idade"));
    }

    @Test
    void copiaSubstituindoOsCriteriosDoDestino() {
        int outraFase = FaseEtapaControllerTest.criarFase(10, "Outra", 9);
        int outra = FaseEtapaControllerTest.criarEtapa(outraFase, "Prova objetiva", PROVA_OBJETIVA);
        given().contentType(ContentType.JSON).body(jurado()).when().post(urlDa(outra)).then().statusCode(201);
        criar(idoso()).statusCode(201);
        criar(notaDisciplina("lingua_portuguesa")).statusCode(201);

        copiar("[" + outra + "]").statusCode(204);
        given().when().get(urlDa(outra)).then().statusCode(200)
                .body("tipo", contains("idade", "nota"))
                .body("ordem", contains(1, 2))
                .body("etapaId", contains(outra, outra));
        copiar("[]").statusCode(400);
        copiar("[999]").statusCode(404);
    }

    @Test
    void atualizaSemMudarAOrdem() {
        criar(idoso()).statusCode(201);
        int id = criar(notaDisciplina("direito_civil")).statusCode(201).extract().path("id");

        given().contentType(ContentType.JSON).body(notaDisciplina("direito_ambiental"))
                .when().put("/v1/idecan/criterios-desempate/" + id).then().statusCode(200)
                .body("ordem", is(2))
                .body("fonte", is("direito_ambiental"));
    }

    @Test
    void excluirRenumeraOsSeguintes() {
        int primeiro = criar(idoso()).statusCode(201).extract().path("id");
        criar(notaDisciplina("direito_civil")).statusCode(201);
        criar(jurado()).statusCode(201);

        given().when().delete("/v1/idecan/criterios-desempate/" + primeiro).then().statusCode(204);
        given().when().get(url).then().statusCode(200)
                .body("ordem", contains(1, 2))
                .body("tipo", contains("nota", "booleano"));
    }

    @Test
    void reordenaComTodosOsCriterios() {
        int a = criar(idoso()).statusCode(201).extract().path("id");
        int b = criar(notaDisciplina("direito_civil")).statusCode(201).extract().path("id");
        int c = criar(jurado()).statusCode(201).extract().path("id");

        reordenar("[" + c + ", " + a + ", " + b + "]").statusCode(200)
                .body("id", contains(c, a, b))
                .body("ordem", contains(1, 2, 3));
        reordenar("[" + a + ", " + b + "]").statusCode(400);
        reordenar("[" + a + ", " + a + ", " + b + "]").statusCode(400);
    }

    @Test
    void validaOsCamposObrigatorios() {
        criar("{\"tipo\": \"nota\", \"direcao\": \"desc\", \"fonte\": \"prova_objetiva\"}").statusCode(400);
        criar("""
                {"label": "Maior nota", "tipo": "nota", "direcao": "desc"}
                """).statusCode(400);
        criar("""
                {"label": "x", "tipo": "qualquer", "direcao": "desc"}
                """).statusCode(400);
        given().when().get(url).then().statusCode(200).body("$", hasSize(0));
    }

    @Test
    void etapaOuCriterioInexistenteEh404() {
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

    private static String urlDa(int etapaId) {
        return "/v1/idecan/etapas/" + etapaId + "/criterios-desempate";
    }

    private ValidatableResponse criar(String corpo) {
        return given().contentType(ContentType.JSON).body(corpo).when().post(url).then();
    }

    private ValidatableResponse reordenar(String ids) {
        return given().contentType(ContentType.JSON).body("{\"ids\": " + ids + "}")
                .when().put(url + "/ordem").then();
    }

    private ValidatableResponse copiar(String etapaIds) {
        return given().contentType(ContentType.JSON).body("{\"etapaIds\": " + etapaIds + "}")
                .when().post(url + "/copiar").then();
    }

    private static String idoso() {
        return """
                {"label": "Idade igual ou superior a 60 anos", "tipo": "idade", "direcao": "desc",
                 "condicao": {"operador": ">=", "valor": 60, "unidade": "anos"},
                 "dataReferencia": "ultimo_dia_inscricao", "fonte": null, "baseLegal": "idoso"}""";
    }

    private static String notaDisciplina(String fonte) {
        return """
                {"label": "Maior nota em Conhecimentos Específicos", "tipo": "nota", "direcao": "desc",
                 "condicao": null, "dataReferencia": null, "fonte": "%s", "baseLegal": null}""".formatted(fonte);
    }

    private static String jurado() {
        return """
                {"label": "Ter exercido a função de jurado", "tipo": "booleano", "direcao": "desc",
                 "condicao": {"operador": "==", "valor": true},
                 "dataReferencia": "ultimo_dia_inscricao", "fonte": null, "baseLegal": "jurado"}""";
    }
}

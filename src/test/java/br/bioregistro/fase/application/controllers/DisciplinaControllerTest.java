package br.bioregistro.fase.application.controllers;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static br.bioregistro.fase.application.controllers.GrupoCargoControllerTest.criarGrupo;
import static br.bioregistro.fase.application.controllers.GrupoCargoControllerTest.gerar;
import static br.bioregistro.fase.application.use_cases.TiposEmMemoria.PROVA_OBJETIVA;
import static br.bioregistro.fase.application.use_cases.TiposEmMemoria.TAF;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

/** Disciplinas de cada prova (etapa) e o valor de cada questão. Edital 10 tem os cargos 101–103. */
@QuarkusTest
@TestProfile(SemBanco.class)
class DisciplinaControllerTest {

    private ApiEmMemoria api;

    @BeforeEach
    void dados() {
        api = ApiEmMemoria.instalar();
        api.disciplinas
                .com(10, 1, "Direito Constitucional", 1, 10, null)
                .com(10, 2, "Direito Ambiental", 11, 15, 101)
                .com(10, 3, "Direito Civil", 11, 15, 102)
                .com(11, 4, "De outro edital", 1, 5, null);
    }

    @Test
    void provaObjetivaMostraAsFaixasAntigasDosCargosDoGrupo() {
        int grupo = grupoComCargos("Procurador", 101);
        int etapa = gerarProva(grupo, "Prova objetiva", PROVA_OBJETIVA);

        listar(etapa).statusCode(200)
                .body("disciplinas.nome", contains("Direito Constitucional", "Direito Ambiental"))
                .body("disciplinas.questoes", contains(10, 5))
                .body("disciplinas.peso", contains(nullValue(), nullValue()))
                .body("grupoId", is(grupo))
                .body("cargoIds", contains(101));
    }

    @Test
    void cadaProvaTemAsSuasDisciplinas() {
        int grupo = grupoComCargos("Procurador", 101, 102);
        int objetiva = gerarProva(grupo, "Prova objetiva", PROVA_OBJETIVA);
        int outra = gerarProva(grupo, "TAF", TAF);

        listar(outra).statusCode(200).body("disciplinas", hasSize(0));
        criar(outra, "{\"nome\": \"  Redação \", \"questaoInicio\": 1, \"questaoFim\": 2}").statusCode(201)
                .body("disciplinas.nome", contains("Redação", "Redação"))
                .body("disciplinas.cargoId", contains(101, 102))
                .body("disciplinas.etapaId", contains(outra, outra));

        listar(objetiva).statusCode(200)
                .body("disciplinas.nome", contains("Direito Constitucional", "Direito Ambiental", "Direito Civil"));
        criar(objetiva, "{\"nome\": \"Português\", \"questaoInicio\": 16, \"questaoFim\": 30, \"cargoIds\": [102]}")
                .statusCode(201)
                .body("disciplinas.nome", contains("Direito Constitucional", "Direito Ambiental", "Direito Civil",
                        "Português"))
                .body("disciplinas[3].etapaId", is(objetiva));
    }

    @Test
    void recusaFaixaInvalidaOuQueJaTemDisciplina() {
        int grupo = grupoComCargos("Procurador", 101);
        int etapa = gerarProva(grupo, "Prova objetiva", PROVA_OBJETIVA);

        criar(etapa, "{\"nome\": \"Português\", \"questaoInicio\": 5, \"questaoFim\": 12}").statusCode(400)
                .body("message", containsString("Direito Constitucional"));
        criar(etapa, "{\"nome\": \" \", \"questaoInicio\": 16, \"questaoFim\": 20}").statusCode(400);
        criar(etapa, "{\"nome\": \"Português\", \"questaoInicio\": 20, \"questaoFim\": 16}").statusCode(400);
        criar(etapa, "{\"nome\": \"Português\", \"questaoInicio\": 16, \"questaoFim\": 20, \"cargoIds\": [103]}")
                .statusCode(400);
        criar(999, "{\"nome\": \"Português\", \"questaoInicio\": 16, \"questaoFim\": 20}").statusCode(404);
    }

    @Test
    void removerFaixaEExcluirEtapaTiramAsDisciplinas() {
        int grupo = grupoComCargos("Procurador", 101);
        int etapa = gerarProva(grupo, "TAF", TAF);
        int faixa = criar(etapa, "{\"nome\": \"Corrida\", \"questaoInicio\": 1, \"questaoFim\": 1}").statusCode(201)
                .extract().path("disciplinas[0].faixaId");

        given().when().delete("/v1/idecan/disciplinas/" + faixa).then().statusCode(204);
        given().when().delete("/v1/idecan/disciplinas/" + faixa).then().statusCode(404);

        int outra = criar(etapa, "{\"nome\": \"Natação\", \"questaoInicio\": 2, \"questaoFim\": 2}").statusCode(201)
                .extract().path("disciplinas[0].faixaId");
        given().when().delete("/v1/idecan/etapas/" + etapa).then().statusCode(204);
        definirPeso(outra, "1").statusCode(404);
    }

    @Test
    void valorDaQuestaoDefineOsPontos() {
        int grupo = grupoComCargos("Procurador", 101);
        int etapa = gerarProva(grupo, "Prova objetiva", PROVA_OBJETIVA);
        definirPeso(1, "1.0").statusCode(200).body("pontos", is(10.0f));
        definirPeso(2, "2.5").statusCode(200).body("peso", is(2.5f)).body("pontos", is(12.5f));

        listar(etapa).statusCode(200).body("disciplinas.pontos", contains(10.0f, 12.5f));
    }

    @Test
    void valorInvalidoEh400() {
        definirPeso(1, "0").statusCode(400);
        definirPeso(1, "1.234").statusCode(400);
        definirPeso(1, "101").statusCode(400);
    }

    @Test
    void faixaOuEtapaInexistenteEh404() {
        definirPeso(999, "1").statusCode(404);
        listar(999).statusCode(404);
    }

    private static ValidatableResponse listar(int etapaId) {
        return given().when().get("/v1/idecan/etapas/" + etapaId + "/disciplinas").then();
    }

    private static ValidatableResponse criar(int etapaId, String corpo) {
        return given().contentType(ContentType.JSON).body(corpo)
                .when().post("/v1/idecan/etapas/" + etapaId + "/disciplinas").then();
    }

    private static ValidatableResponse definirPeso(int faixaId, String peso) {
        return given().contentType(ContentType.JSON).body("{\"peso\": " + peso + "}")
                .when().put("/v1/idecan/disciplinas/" + faixaId + "/peso").then();
    }

    static int grupoComCargos(String nome, int... cargoIds) {
        int id = criarGrupo(nome);
        for (int cargoId : cargoIds) {
            given().when().put("/v1/idecan/grupos-cargo/" + id + "/cargos/" + cargoId).then().statusCode(200);
        }
        return id;
    }

    /** Gera uma fase com uma etapa do tipo e devolve o id da etapa. */
    static int gerarProva(int grupoId, String nome, int tipoId) {
        return given().contentType(ContentType.JSON).body(gerar(nome, tipoId))
                .when().post("/v1/idecan/grupos-cargo/" + grupoId + "/gerar-fases").then().statusCode(201)
                .extract().path("[0].etapas[0].id");
    }

    /** Gera uma fase com uma etapa do tipo e devolve o id da fase. */
    static int gerarFase(int grupoId, String nome, int tipoId) {
        return given().contentType(ContentType.JSON).body(gerar(nome, tipoId))
                .when().post("/v1/idecan/grupos-cargo/" + grupoId + "/gerar-fases").then().statusCode(201)
                .extract().path("[0].fase.id");
    }
}

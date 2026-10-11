package br.bioregistro.fase.application.controllers;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

/** {@code /v1/{instituicao}/tipos}. */
@QuarkusTest
@TestProfile(SemBanco.class)
class TipoControllerTest {

    @BeforeEach
    void dados() {
        ApiEmMemoria.instalar();
    }

    @Test
    void listaTodosOsTiposDeEtapaComCategoria() {
        given().when().get("/v1/idecan/tipos/etapa")
                .then().statusCode(200)
                .body("$", hasSize(3))
                .body("find { it.valor == 'analise_pcd' }.categoria", is("ANALISE"))
                .body("find { it.valor == 'prova_objetiva' }.categoria", is("EXECUCAO"));
    }

    @Test
    void filtraSoTiposDeExecucao() {
        given().queryParam("categoria", "execucao")
                .when().get("/v1/idecan/tipos/etapa")
                .then().statusCode(200)
                .body("categoria", everyItem(is("EXECUCAO")))
                .body("valor", not(hasItem("analise_pcd")));
    }

    @Test
    void categoriaInvalidaEh400() {
        given().queryParam("categoria", "xyz")
                .when().get("/v1/idecan/tipos/etapa")
                .then().statusCode(400)
                .body("message", containsString("Categoria inválida"));
    }

    @Test
    void listaTiposDePublicacao() {
        given().when().get("/v1/idib/tipos/publicacao")
                .then().statusCode(200)
                .body("valor", hasItem("gabarito_preliminar"));
    }

    @Test
    void instituicaoInvalidaEhRecusada() {
        given().when().get("/v1/xpto/tipos/etapa")
                .then().statusCode(org.hamcrest.Matchers.anyOf(is(400), is(404)));
    }
}

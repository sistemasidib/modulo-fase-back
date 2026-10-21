package br.bioregistro.fase.application.controllers;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;

@QuarkusTest
@TestProfile(SemBanco.class)
class CronogramaPublicoControllerTest {

    private static final String API = "/v1/idecan/editais/10/cronograma-publico";

    @BeforeEach
    void limpar() {
        ApiEmMemoria.instalar();
    }

    @Test
    void listaVaziaNoInicio() {
        given().when().get(API)
                .then().statusCode(200)
                .body("$", hasSize(0));
    }

    @Test
    void salvaEListaItensAnexoIi() {
        given().contentType(ContentType.JSON)
                .body("""
                        {"itens":[
                          {"descricao":"Publicação do edital","inicio":"2026-05-19","termino":"2026-05-19","destaque":false},
                          {"descricao":"Período de inscrições","inicio":"2026-05-19","termino":"2026-06-22","destaque":true}
                        ]}
                        """)
                .when().put(API)
                .then().statusCode(200)
                .body("$", hasSize(2))
                .body("[0].ordem", is(1))
                .body("[0].descricao", is("Publicação do edital"))
                .body("[1].destaque", is(true))
                .body("[1].termino", is("2026-06-22"));

        given().when().get(API)
                .then().statusCode(200)
                .body("$", hasSize(2))
                .body("[1].descricao", is("Período de inscrições"));
    }

    @Test
    void rejeitaTerminoAntesDoInicio() {
        given().contentType(ContentType.JSON)
                .body("""
                        {"itens":[
                          {"descricao":"X","inicio":"2026-06-01","termino":"2026-05-01","destaque":false}
                        ]}
                        """)
                .when().put(API)
                .then().statusCode(400);
    }

    @Test
    void baixaPdfAnexoIi() {
        given().contentType(ContentType.JSON)
                .body("""
                        {"itens":[
                          {"descricao":"Publicacao","inicio":"2026-05-19","termino":"2026-05-19","destaque":false}
                        ]}
                        """)
                .when().put(API)
                .then().statusCode(200);

        given().when().get(API + "/pdf")
                .then().statusCode(200)
                .header("Content-Type", startsWith("application/pdf"))
                .header("Content-Disposition", org.hamcrest.Matchers.containsString("cronograma-publico-10.pdf"));
    }
}

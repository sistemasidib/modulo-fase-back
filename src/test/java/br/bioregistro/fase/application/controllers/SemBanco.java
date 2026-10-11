package br.bioregistro.fase.application.controllers;

import io.quarkus.test.junit.QuarkusTestProfile;

import java.util.Map;

/**
 * Sobe o serviço sem SQL Server: os datasources recebem uma URL qualquer (nunca usada, porque
 * {@link ApiEmMemoria} troca todos os repositórios) e a checagem de versão do banco é desligada.
 */
public class SemBanco implements QuarkusTestProfile {

    private static final String URL = "jdbc:sqlserver://localhost:1;databaseName=teste;encrypt=false";

    @Override
    public Map<String, String> getConfigOverrides() {
        return Map.ofEntries(
                Map.entry("quarkus.datasource.idecan.jdbc.url", URL),
                Map.entry("quarkus.datasource.idecan.username", "teste"),
                Map.entry("quarkus.datasource.idecan.password", "teste"),
                Map.entry("quarkus.datasource.idib.jdbc.url", URL),
                Map.entry("quarkus.datasource.idib.username", "teste"),
                Map.entry("quarkus.datasource.idib.password", "teste"),
                Map.entry("quarkus.datasource.modulo.jdbc.url", URL),
                Map.entry("quarkus.datasource.modulo.username", "teste"),
                Map.entry("quarkus.datasource.modulo.password", "teste"),
                Map.entry("quarkus.hibernate-orm.idecan.database.version-check.enabled", "false"),
                Map.entry("quarkus.hibernate-orm.idib.database.version-check.enabled", "false"),
                Map.entry("quarkus.hibernate-orm.modulo.database.version-check.enabled", "false"),
                Map.entry("quarkus.datasource.idecan.jdbc.min-size", "0"),
                Map.entry("quarkus.datasource.idib.jdbc.min-size", "0"),
                Map.entry("quarkus.datasource.modulo.jdbc.min-size", "0"));
    }
}

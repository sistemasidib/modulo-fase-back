package br.bioregistro.fase.infrastructure;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

import java.time.Clock;
import java.time.ZoneId;

/** "Hoje" do cronograma (RN11, RN31) no fuso de Brasília; os testes passam um relógio fixo. */
public class Relogio {

    @Produces
    @ApplicationScoped
    Clock clock() {
        return Clock.system(ZoneId.of("America/Sao_Paulo"));
    }
}

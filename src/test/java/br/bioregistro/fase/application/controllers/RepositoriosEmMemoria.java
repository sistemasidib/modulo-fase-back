package br.bioregistro.fase.application.controllers;

import br.bioregistro.fase.domain.port.CriterioCorrecaoRepository;
import br.bioregistro.fase.domain.port.CriterioDesempateRepository;
import br.bioregistro.fase.domain.port.CronogramaRepository;
import br.bioregistro.fase.domain.port.DisciplinaRepository;
import br.bioregistro.fase.domain.port.EntidadeRepository;
import br.bioregistro.fase.domain.port.EtapaRepository;
import br.bioregistro.fase.domain.port.FaseRepository;
import br.bioregistro.fase.domain.port.GrupoCargoRepository;
import br.bioregistro.fase.domain.port.PublicacaoRepository;
import br.bioregistro.fase.domain.port.TipoRepository;
import br.bioregistro.fase.domain.port.VinculoCronogramaRepository;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import jakarta.enterprise.inject.Produces;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.time.Clock;
import java.util.function.Function;

/**
 * Nos testes, as portas de persistência e o relógio vêm daqui (alternativa CDI com prioridade),
 * no lugar dos adapters de SQL Server. Cada chamada vai para o {@link ApiEmMemoria} instalado
 * pelo teste em andamento.
 */
@Alternative
@Priority(1)
@ApplicationScoped
public class RepositoriosEmMemoria {

    static volatile ApiEmMemoria atual;

    @Produces
    FaseRepository fases() {
        return delegar(FaseRepository.class, a -> a.fases);
    }

    @Produces
    GrupoCargoRepository grupos() {
        return delegar(GrupoCargoRepository.class, a -> a.grupos);
    }

    @Produces
    EtapaRepository etapas() {
        return delegar(EtapaRepository.class, a -> a.etapas);
    }

    @Produces
    PublicacaoRepository publicacoes() {
        return delegar(PublicacaoRepository.class, a -> a.publicacoes);
    }

    @Produces
    TipoRepository tipos() {
        return delegar(TipoRepository.class, a -> a.tipos);
    }

    @Produces
    EntidadeRepository entidades() {
        return delegar(EntidadeRepository.class, a -> a.entidades);
    }

    @Produces
    CronogramaRepository cronogramas() {
        return delegar(CronogramaRepository.class, a -> a.cronogramas);
    }

    @Produces
    VinculoCronogramaRepository vinculos() {
        return delegar(VinculoCronogramaRepository.class, a -> a.vinculos);
    }

    @Produces
    DisciplinaRepository disciplinas() {
        return delegar(DisciplinaRepository.class, a -> a.disciplinas);
    }

    @Produces
    CriterioDesempateRepository criteriosDesempate() {
        return delegar(CriterioDesempateRepository.class, a -> a.criteriosDesempate);
    }

    @Produces
    CriterioCorrecaoRepository criteriosCorrecao() {
        return delegar(CriterioCorrecaoRepository.class, a -> a.criteriosCorrecao);
    }

    @Produces
    Clock relogio() {
        return Clock.fixed(ApiEmMemoria.HOJE.atStartOfDay(ApiEmMemoria.BR).toInstant(), ApiEmMemoria.BR);
    }

    private static <T> T delegar(Class<T> porta, Function<ApiEmMemoria, Object> alvo) {
        Object proxy = Proxy.newProxyInstance(porta.getClassLoader(), new Class<?>[]{porta}, (p, metodo, args) -> {
            try {
                return metodo.invoke(alvo.apply(atual), args);
            } catch (InvocationTargetException e) {
                throw e.getCause();
            }
        });
        return porta.cast(proxy);
    }
}

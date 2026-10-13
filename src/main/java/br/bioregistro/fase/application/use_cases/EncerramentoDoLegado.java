package br.bioregistro.fase.application.use_cases;

import br.bioregistro.fase.domain.exception.ConflitoException;
import br.bioregistro.fase.domain.model.Instituicao;
import br.bioregistro.fase.domain.port.CronogramaRepository;
import br.bioregistro.fase.domain.port.FaseRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Optional;

/**
 * Fim do modelo anterior (spec 005, plano §4): edital sem cronograma ainda aceita datas de
 * publicação digitadas, até a data de encerramento ({@code fase.legado.encerramento}, AAAA-MM-DD).
 * Depois dela, é preciso abrir o cronograma do edital. Sem a propriedade, nada é bloqueado.
 */
@ApplicationScoped
public class EncerramentoDoLegado {

    private final Optional<LocalDate> encerramento;
    private final Clock relogio;
    private final FaseRepository fases;
    private final CronogramaRepository cronogramas;

    @Inject
    public EncerramentoDoLegado(@ConfigProperty(name = "fase.legado.encerramento") Optional<String> encerramento,
                                Clock relogio, FaseRepository fases, CronogramaRepository cronogramas) {
        this(encerramento.filter(s -> !s.isBlank()).map(LocalDate::parse).orElse(null), relogio, fases, cronogramas);
    }

    /** @param encerramento {@code null} = sem data de encerramento */
    EncerramentoDoLegado(LocalDate encerramento, Clock relogio, FaseRepository fases, CronogramaRepository cronogramas) {
        this.encerramento = Optional.ofNullable(encerramento);
        this.relogio = relogio;
        this.fases = fases;
        this.cronogramas = cronogramas;
    }

    /** Datas digitadas na publicação da fase: só enquanto o legado não terminou ou se não há cronograma. */
    public void exigirAberto(Instituicao instituicao, int faseId) {
        if (encerramento.isEmpty() || !LocalDate.now(relogio).isAfter(encerramento.get())) {
            return;
        }
        fases.buscar(instituicao, faseId)
                .filter(f -> cronogramas.doEdital(instituicao, f.editalId()).isEmpty())
                .ifPresent(f -> {
                    throw new ConflitoException("O modelo sem cronograma terminou em " + encerramento.get()
                            + ". Abra o cronograma do edital " + f.editalId() + ": as datas passam a vir dele.");
                });
    }
}

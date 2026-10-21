package br.bioregistro.fase.application.use_cases;

import br.bioregistro.fase.domain.model.Instituicao;
import br.bioregistro.fase.domain.model.cronograma.Cronograma;
import br.bioregistro.fase.domain.model.cronograma.CronogramaCalculado;
import br.bioregistro.fase.domain.model.cronograma.CronogramaPublico;
import br.bioregistro.fase.domain.model.cronograma.EditalDoCronograma;
import br.bioregistro.fase.domain.model.cronograma.PdfCronogramaPublico;
import br.bioregistro.fase.domain.port.CronogramaRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Monta o retrato do cronograma público (spec 006, RN07–RN09) e o PDF (RN07).
 */
@ApplicationScoped
public class ExportarCronogramaPublico {

    private final CronogramaRepository cronogramas;
    private final ConsultarCronograma consultar;
    private final Clock relogio;

    public ExportarCronogramaPublico(CronogramaRepository cronogramas, ConsultarCronograma consultar, Clock relogio) {
        this.cronogramas = cronogramas;
        this.consultar = consultar;
        this.relogio = relogio;
    }

    public CronogramaPublico executar(Instituicao instituicao, int cronogramaId, Integer editalId) {
        ConsultarCronograma.Alvo alvo = consultar.alvo(instituicao, cronogramaId, editalId);
        Cronograma visto = alvo.visto();
        CronogramaCalculado calculado = consultar.calcular(visto, visto.edicao());
        List<EditalDoCronograma> editais = cronogramas.editais(instituicao, cronogramaId);
        if (editalId != null) {
            editais = editais.stream().filter(e -> e.editalId() == editalId).toList();
        }
        List<String> nomes = editais.stream().map(e -> "Edital " + e.editalId()).toList();
        String titulo = editalId != null ? "Edital " + editalId : "Cronograma " + cronogramaId;
        return CronogramaPublico.de(titulo, nomes, OffsetDateTime.now(relogio), calculado);
    }

    public byte[] pdf(Instituicao instituicao, int cronogramaId, Integer editalId) {
        return PdfCronogramaPublico.gerar(executar(instituicao, cronogramaId, editalId));
    }
}

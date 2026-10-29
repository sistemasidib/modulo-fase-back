package br.bioregistro.fase.application.use_cases;

import br.bioregistro.fase.domain.exception.RegraDeNegocioException;
import br.bioregistro.fase.domain.model.Etapa;
import br.bioregistro.fase.domain.model.Instituicao;
import br.bioregistro.fase.domain.model.Modalidade;
import br.bioregistro.fase.domain.model.TipoEtapa;
import br.bioregistro.fase.domain.port.TipoRepository;

import java.math.BigDecimal;
import java.util.Set;

/**
 * Defaults e restrições da discursiva e do S05 (spec 006, RN04–RN06 e RN16–RN17).
 */
final class CamposDaEtapa {

    static final String PROVA_DISCURSIVA = "prova_discursiva";
    static final Set<String> S05 = Set.of("heteroidentificacao", "biopsicossocial");

    private CamposDaEtapa() {
    }

    static Etapa aplicar(TipoRepository tipos, Instituicao instituicao, Etapa etapa, boolean nova) {
        TipoEtapa tipo = tipos.tipoEtapa(instituicao, etapa.tipoId())
                .orElseThrow(() -> new RegraDeNegocioException("Tipo de etapa inexistente."));
        Boolean zero = etapa.notaZeroElimina();
        BigDecimal minima = etapa.notaMinimaPercentual();
        Modalidade modalidade = etapa.modalidade();

        if (PROVA_DISCURSIVA.equals(tipo.valor())) {
            if (nova && zero == null) {
                zero = true;
            }
            if (modalidade != null) {
                throw new RegraDeNegocioException("Modalidade só vale para heteroidentificação e biopsicossocial.");
            }
        } else {
            if (zero != null || minima != null) {
                throw new RegraDeNegocioException(
                        "Nota zero elimina e nota mínima só valem na prova discursiva.");
            }
            zero = null;
            minima = null;
        }

        if (S05.contains(tipo.valor())) {
            if (modalidade == null) {
                modalidade = Modalidade.PRESENCIAL;
            } else {
                modalidade = modalidade.canonica();
            }
        } else if (modalidade != null) {
            throw new RegraDeNegocioException("Modalidade só vale para heteroidentificação e biopsicossocial.");
        }

        return new Etapa(etapa.id(), etapa.faseId(), etapa.nome(), etapa.tipoId(), etapa.descricao(),
                etapa.dataAbertura(), etapa.dataFechamento(), etapa.statusId(), etapa.reprovavel(),
                etapa.obrigatoriaParaFase(), etapa.todosCandidatos(), etapa.aceitaDocumentos(),
                etapa.aceitaRecurso(), etapa.preliminar(), etapa.elegibilidadeEtapaId(),
                etapa.elegibilidadeStatusId(), etapa.responsavelId(), etapa.publicadaEm(),
                zero, minima, modalidade, etapa.diasRecurso());
    }
}

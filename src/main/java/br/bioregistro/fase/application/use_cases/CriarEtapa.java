package br.bioregistro.fase.application.use_cases;

import br.bioregistro.fase.application.dtos.EtapaRequest;
import br.bioregistro.fase.application.dtos.EtapaResponse;
import br.bioregistro.fase.domain.exception.NaoEncontradoException;
import br.bioregistro.fase.domain.exception.RegraDeNegocioException;
import br.bioregistro.fase.domain.model.CriterioCorrecao;
import br.bioregistro.fase.domain.model.Etapa;
import br.bioregistro.fase.domain.model.Instituicao;
import br.bioregistro.fase.domain.model.TipoEtapa;
import br.bioregistro.fase.domain.port.CriterioCorrecaoRepository;
import br.bioregistro.fase.domain.port.EtapaRepository;
import br.bioregistro.fase.domain.port.FaseRepository;
import br.bioregistro.fase.domain.port.TipoRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.math.BigDecimal;

/** Cria a etapa já com os espaços de publicação padrão do tipo dela. */
@ApplicationScoped
public class CriarEtapa {

    private final FaseRepository fases;
    private final EtapaRepository etapas;
    private final TipoRepository tipos;
    private final EspacosDePublicacao espacos;
    private final CriterioCorrecaoRepository criterios;
    private final ResultadoFinalDaFase resultadoFinal;
    private final AplicarConfigPublicacaoNaEtapa aplicarConfig;

    public CriarEtapa(FaseRepository fases, EtapaRepository etapas, TipoRepository tipos,
                      EspacosDePublicacao espacos, CriterioCorrecaoRepository criterios,
                      ResultadoFinalDaFase resultadoFinal, AplicarConfigPublicacaoNaEtapa aplicarConfig) {
        this.fases = fases;
        this.etapas = etapas;
        this.tipos = tipos;
        this.espacos = espacos;
        this.criterios = criterios;
        this.resultadoFinal = resultadoFinal;
        this.aplicarConfig = aplicarConfig;
    }

    @Transactional
    public EtapaResponse executar(Instituicao instituicao, int faseId, EtapaRequest request) {
        if (fases.buscar(instituicao, faseId).isEmpty()) {
            throw new NaoEncontradoException("Fase", faseId);
        }
        Etapa pedida = request.paraDominio(null, faseId);
        TipoEtapa tipo = TipoEtapaExistente.exigirNovo(tipos, instituicao, pedida.tipoId());
        if (tipo.ehResultadoFinal()) {
            throw new RegraDeNegocioException(
                    "\"Resultado Final\" é obrigatória em toda fase e não se cria manualmente.");
        }
        Etapa etapa = CamposDaEtapa.aplicar(tipos, instituicao, pedida, true);
        EtapaPreRequisito.exigirExistente(etapas, instituicao, etapa);
        Etapa criada = etapas.salvar(instituicao, etapa);
        espacos.criarParaEtapa(instituicao, faseId, criada.id(), criada.tipoId());
        espacos.ajustarEspacoDaModalidade(instituicao, faseId, criada.id(), null, criada.modalidade());
        aplicarConfig.executar(instituicao, criada);
        resultadoFinal.garantir(instituicao, faseId);
        espacos.sincronizarResultadoDaFase(instituicao, faseId);
        return EtapaResponse.de(criada, pontuacaoMaxima(instituicao, criada.id()));
    }

    private BigDecimal pontuacaoMaxima(Instituicao instituicao, int etapaId) {
        BigDecimal maxima = criterios.daEtapa(instituicao, etapaId).stream()
                .map(CriterioCorrecao::pontuacaoMaxima)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return maxima.compareTo(BigDecimal.ZERO) == 0 ? null : maxima;
    }
}

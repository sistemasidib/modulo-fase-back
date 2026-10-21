package br.bioregistro.fase.application.use_cases;

import br.bioregistro.fase.application.dtos.EtapaResponse;
import br.bioregistro.fase.domain.exception.NaoEncontradoException;
import br.bioregistro.fase.domain.model.CriterioCorrecao;
import br.bioregistro.fase.domain.model.Etapa;
import br.bioregistro.fase.domain.model.Instituicao;
import br.bioregistro.fase.domain.port.CriterioCorrecaoRepository;
import br.bioregistro.fase.domain.port.EtapaRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.math.BigDecimal;

@ApplicationScoped
public class BuscarEtapa {

    private final EtapaRepository etapas;
    private final CriterioCorrecaoRepository criterios;

    public BuscarEtapa(EtapaRepository etapas, CriterioCorrecaoRepository criterios) {
        this.etapas = etapas;
        this.criterios = criterios;
    }

    public EtapaResponse executar(Instituicao instituicao, int id) {
        Etapa etapa = etapas.buscar(instituicao, id)
                .orElseThrow(() -> new NaoEncontradoException("Etapa", id));
        BigDecimal maxima = criterios.daEtapa(instituicao, id).stream()
                .map(CriterioCorrecao::pontuacaoMaxima)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return EtapaResponse.de(etapa, maxima.compareTo(BigDecimal.ZERO) == 0 ? null : maxima);
    }
}

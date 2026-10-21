package br.bioregistro.fase.application.use_cases;

import br.bioregistro.fase.application.dtos.EtapaRequest;
import br.bioregistro.fase.application.dtos.EtapaResponse;
import br.bioregistro.fase.domain.exception.ConflitoException;
import br.bioregistro.fase.domain.exception.NaoEncontradoException;
import br.bioregistro.fase.domain.model.CriterioCorrecao;
import br.bioregistro.fase.domain.model.Etapa;
import br.bioregistro.fase.domain.model.Instituicao;
import br.bioregistro.fase.domain.model.Modalidade;
import br.bioregistro.fase.domain.port.CriterioCorrecaoRepository;
import br.bioregistro.fase.domain.port.EtapaRepository;
import br.bioregistro.fase.domain.port.TipoRepository;
import br.bioregistro.fase.domain.port.VinculoCronogramaRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.math.BigDecimal;

/**
 * Edita os dados da etapa. A etapa continua na mesma fase. Trocar o tipo não recria os
 * espaços de publicação já existentes. A etapa que o cronograma criou tem o tipo do módulo dela
 * e não troca (spec 005, RN03b). Modalidade S05 troca o espaço não publicado (spec 006, RN18).
 */
@ApplicationScoped
public class AtualizarEtapa {

    private final EtapaRepository etapas;
    private final TipoRepository tipos;
    private final VinculoCronogramaRepository vinculos;
    private final EspacosDePublicacao espacos;
    private final CriterioCorrecaoRepository criterios;

    public AtualizarEtapa(EtapaRepository etapas, TipoRepository tipos, VinculoCronogramaRepository vinculos,
                          EspacosDePublicacao espacos, CriterioCorrecaoRepository criterios) {
        this.etapas = etapas;
        this.tipos = tipos;
        this.vinculos = vinculos;
        this.espacos = espacos;
        this.criterios = criterios;
    }

    @Transactional
    public EtapaResponse executar(Instituicao instituicao, int id, EtapaRequest request) {
        Etapa atual = etapas.buscar(instituicao, id)
                .orElseThrow(() -> new NaoEncontradoException("Etapa", id));
        Etapa etapa = CamposDaEtapa.aplicar(tipos, instituicao, request.paraDominio(id, atual.faseId()), false);
        TipoEtapaExistente.exigir(tipos, instituicao, etapa.tipoId());
        if (!etapa.tipoId().equals(atual.tipoId())) {
            vinculos.moduloDaFase(instituicao, atual.faseId())
                    .filter(m -> tipos.tipoEtapa(instituicao, atual.tipoId())
                            .map(t -> m.tipoEtapa().equals(t.valor())).orElse(false))
                    .ifPresent(m -> {
                        throw new ConflitoException("A etapa " + id + " é a " + m.nome()
                                + " do cronograma: o tipo dela não muda. Mude os módulos no cronograma.");
                    });
        }
        EtapaPreRequisito.exigirExistente(etapas, instituicao, etapa);
        Modalidade modalidadeAnterior = atual.modalidade();
        Etapa salva = etapas.salvar(instituicao, etapa);
        String aviso = espacos.ajustarEspacoDaModalidade(instituicao, salva.faseId(), salva.id(),
                modalidadeAnterior, salva.modalidade());
        return EtapaResponse.de(salva, pontuacaoMaxima(instituicao, salva.id()), aviso);
    }

    private BigDecimal pontuacaoMaxima(Instituicao instituicao, int etapaId) {
        BigDecimal maxima = criterios.daEtapa(instituicao, etapaId).stream()
                .map(CriterioCorrecao::pontuacaoMaxima)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return maxima.compareTo(BigDecimal.ZERO) == 0 ? null : maxima;
    }
}

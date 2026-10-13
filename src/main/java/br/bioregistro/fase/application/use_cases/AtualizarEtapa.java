package br.bioregistro.fase.application.use_cases;

import br.bioregistro.fase.application.dtos.EtapaRequest;
import br.bioregistro.fase.application.dtos.EtapaResponse;
import br.bioregistro.fase.domain.exception.ConflitoException;
import br.bioregistro.fase.domain.exception.NaoEncontradoException;
import br.bioregistro.fase.domain.model.Etapa;
import br.bioregistro.fase.domain.model.Instituicao;
import br.bioregistro.fase.domain.port.EtapaRepository;
import br.bioregistro.fase.domain.port.TipoRepository;
import br.bioregistro.fase.domain.port.VinculoCronogramaRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

/**
 * Edita os dados da etapa. A etapa continua na mesma fase. Trocar o tipo não recria os
 * espaços de publicação já existentes. A etapa que o cronograma criou tem o tipo do módulo dela
 * e não troca (spec 005, RN03b).
 */
@ApplicationScoped
public class AtualizarEtapa {

    private final EtapaRepository etapas;
    private final TipoRepository tipos;
    private final VinculoCronogramaRepository vinculos;

    public AtualizarEtapa(EtapaRepository etapas, TipoRepository tipos, VinculoCronogramaRepository vinculos) {
        this.etapas = etapas;
        this.tipos = tipos;
        this.vinculos = vinculos;
    }

    @Transactional
    public EtapaResponse executar(Instituicao instituicao, int id, EtapaRequest request) {
        Etapa atual = etapas.buscar(instituicao, id)
                .orElseThrow(() -> new NaoEncontradoException("Etapa", id));
        Etapa etapa = request.paraDominio(id, atual.faseId());
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
        return EtapaResponse.de(etapas.salvar(instituicao, etapa));
    }
}

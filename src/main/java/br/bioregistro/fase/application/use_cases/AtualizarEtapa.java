package br.bioregistro.fase.application.use_cases;

import br.bioregistro.fase.application.dtos.EtapaRequest;
import br.bioregistro.fase.application.dtos.EtapaResponse;
import br.bioregistro.fase.domain.exception.NaoEncontradoException;
import br.bioregistro.fase.domain.model.Etapa;
import br.bioregistro.fase.domain.model.Instituicao;
import br.bioregistro.fase.domain.port.EtapaRepository;
import br.bioregistro.fase.domain.port.TipoRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

/**
 * Edita os dados da etapa. A etapa continua na mesma fase. Trocar o tipo não recria os
 * espaços de publicação já existentes.
 */
@ApplicationScoped
public class AtualizarEtapa {

    private final EtapaRepository etapas;
    private final TipoRepository tipos;

    public AtualizarEtapa(EtapaRepository etapas, TipoRepository tipos) {
        this.etapas = etapas;
        this.tipos = tipos;
    }

    @Transactional
    public EtapaResponse executar(Instituicao instituicao, int id, EtapaRequest request) {
        Etapa atual = etapas.buscar(instituicao, id)
                .orElseThrow(() -> new NaoEncontradoException("Etapa", id));
        Etapa etapa = request.paraDominio(id, atual.faseId());
        TipoEtapaExistente.exigir(tipos, instituicao, etapa.tipoId());
        EtapaPreRequisito.exigirExistente(etapas, instituicao, etapa);
        return EtapaResponse.de(etapas.salvar(instituicao, etapa));
    }
}

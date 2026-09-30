package br.bioregistro.fase.application.use_cases;

import br.bioregistro.fase.application.dtos.EtapaRequest;
import br.bioregistro.fase.application.dtos.EtapaResponse;
import br.bioregistro.fase.domain.exception.NaoEncontradoException;
import br.bioregistro.fase.domain.model.Etapa;
import br.bioregistro.fase.domain.model.Instituicao;
import br.bioregistro.fase.domain.port.EtapaRepository;
import br.bioregistro.fase.domain.port.FaseRepository;
import br.bioregistro.fase.domain.port.TipoRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

/** Cria a etapa já com os espaços de publicação padrão do tipo dela. */
@ApplicationScoped
public class CriarEtapa {

    private final FaseRepository fases;
    private final EtapaRepository etapas;
    private final TipoRepository tipos;
    private final EspacosDePublicacao espacos;

    public CriarEtapa(FaseRepository fases, EtapaRepository etapas, TipoRepository tipos,
                      EspacosDePublicacao espacos) {
        this.fases = fases;
        this.etapas = etapas;
        this.tipos = tipos;
        this.espacos = espacos;
    }

    @Transactional
    public EtapaResponse executar(Instituicao instituicao, int faseId, EtapaRequest request) {
        if (fases.buscar(instituicao, faseId).isEmpty()) {
            throw new NaoEncontradoException("Fase", faseId);
        }
        Etapa etapa = request.paraDominio(null, faseId);
        TipoEtapaExistente.exigir(tipos, instituicao, etapa.tipoId());
        EtapaPreRequisito.exigirExistente(etapas, instituicao, etapa);
        Etapa criada = etapas.salvar(instituicao, etapa);
        espacos.criarParaEtapa(instituicao, faseId, criada.id(), criada.tipoId());
        return EtapaResponse.de(criada);
    }
}

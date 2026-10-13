package br.bioregistro.fase.application.use_cases;

import br.bioregistro.fase.application.dtos.PublicacaoResponse;
import br.bioregistro.fase.domain.exception.NaoEncontradoException;
import br.bioregistro.fase.domain.model.Instituicao;
import br.bioregistro.fase.domain.port.FaseRepository;
import br.bioregistro.fase.domain.port.PublicacaoRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.OffsetDateTime;
import java.util.List;

/** Publicações da fase e das etapas dela; as vinculadas ao cronograma vêm com as datas dele. */
@ApplicationScoped
public class ListarPublicacoes {

    private final FaseRepository fases;
    private final PublicacaoRepository publicacoes;
    private final PublicacoesDoCronograma doCronograma;

    public ListarPublicacoes(FaseRepository fases, PublicacaoRepository publicacoes,
                             PublicacoesDoCronograma doCronograma) {
        this.fases = fases;
        this.publicacoes = publicacoes;
        this.doCronograma = doCronograma;
    }

    public List<PublicacaoResponse> executar(Instituicao instituicao, int faseId) {
        if (fases.buscar(instituicao, faseId).isEmpty()) {
            throw new NaoEncontradoException("Fase", faseId);
        }
        OffsetDateTime agora = OffsetDateTime.now();
        return doCronograma.comDatas(instituicao, faseId, publicacoes.listarPorFase(instituicao, faseId)).stream()
                .map(p -> PublicacaoResponse.de(p, agora))
                .toList();
    }
}

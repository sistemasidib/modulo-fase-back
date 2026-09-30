package br.bioregistro.fase.application.use_cases;

import br.bioregistro.fase.application.dtos.FaseRequest;
import br.bioregistro.fase.application.dtos.FaseResponse;
import br.bioregistro.fase.domain.exception.ConflitoException;
import br.bioregistro.fase.domain.exception.NaoEncontradoException;
import br.bioregistro.fase.domain.model.Fase;
import br.bioregistro.fase.domain.model.Instituicao;
import br.bioregistro.fase.domain.port.FaseRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

/** Edita os dados da fase. A fase continua no mesmo edital. */
@ApplicationScoped
public class AtualizarFase {

    private final FaseRepository fases;

    public AtualizarFase(FaseRepository fases) {
        this.fases = fases;
    }

    @Transactional
    public FaseResponse executar(Instituicao instituicao, int id, FaseRequest request) {
        Fase atual = fases.buscar(instituicao, id)
                .orElseThrow(() -> new NaoEncontradoException("Fase", id));
        Fase fase = request.paraDominio(id, atual.editalId());
        if (fases.ordemEmUso(instituicao, fase.editalId(), fase.ordem(), id)) {
            throw new ConflitoException("Já existe uma fase com a ordem " + fase.ordem() + " neste edital.");
        }
        return FaseResponse.de(fases.salvar(instituicao, fase));
    }
}

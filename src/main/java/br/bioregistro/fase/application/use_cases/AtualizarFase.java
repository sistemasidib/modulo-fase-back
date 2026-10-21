package br.bioregistro.fase.application.use_cases;

import br.bioregistro.fase.application.dtos.FaseRequest;
import br.bioregistro.fase.application.dtos.FaseResponse;
import br.bioregistro.fase.domain.exception.ConflitoException;
import br.bioregistro.fase.domain.exception.NaoEncontradoException;
import br.bioregistro.fase.domain.model.Fase;
import br.bioregistro.fase.domain.model.Instituicao;
import br.bioregistro.fase.domain.port.FaseRepository;
import br.bioregistro.fase.domain.port.GrupoCargoRepository;
import br.bioregistro.fase.domain.port.VinculoCronogramaRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.List;

/**
 * Edita os dados da fase. A fase continua no mesmo edital. Na fase que segue o cronograma, as
 * datas são as dele (spec 005): as do pedido são ignoradas. Editar afeta todos os grupos da fase
 * (spec 006, RN03).
 */
@ApplicationScoped
public class AtualizarFase {

    private final FaseRepository fases;
    private final VinculoCronogramaRepository vinculos;
    private final GrupoCargoRepository grupos;

    public AtualizarFase(FaseRepository fases, VinculoCronogramaRepository vinculos, GrupoCargoRepository grupos) {
        this.fases = fases;
        this.vinculos = vinculos;
        this.grupos = grupos;
    }

    @Transactional
    public FaseResponse executar(Instituicao instituicao, int id, FaseRequest request) {
        Fase atual = fases.buscar(instituicao, id)
                .orElseThrow(() -> new NaoEncontradoException("Fase", id));
        FaseRequest pedido = vinculos.moduloDaFase(instituicao, id).isEmpty() ? request
                : new FaseRequest(request.nome(), request.descricao(), request.ordem(), atual.dataAbertura(),
                atual.dataFechamento());
        Fase fase = pedido.paraDominio(id, atual.editalId());
        if (fases.ordemEmUso(instituicao, fase.editalId(), fase.ordem(), id)) {
            throw new ConflitoException("Já existe uma fase com a ordem " + fase.ordem() + " neste edital.");
        }
        Fase salva = fases.salvar(instituicao, fase);
        List<Integer> grupoIds = grupos.gruposDaFase(instituicao, salva.id());
        return FaseResponse.de(salva, grupoIds);
    }
}

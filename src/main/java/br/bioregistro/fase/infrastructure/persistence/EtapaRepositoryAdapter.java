package br.bioregistro.fase.infrastructure.persistence;

import br.bioregistro.fase.domain.model.Etapa;
import br.bioregistro.fase.domain.model.Instituicao;
import br.bioregistro.fase.domain.port.EtapaRepository;
import br.bioregistro.fase.infrastructure.persistence.idecan.IdecanEtapas;
import br.bioregistro.fase.infrastructure.persistence.idib.IdibEtapas;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;

/** Escolhe o banco (datasource) da instituição. */
@ApplicationScoped
public class EtapaRepositoryAdapter implements EtapaRepository {

    private final IdecanEtapas idecan;
    private final IdibEtapas idib;

    public EtapaRepositoryAdapter(IdecanEtapas idecan, IdibEtapas idib) {
        this.idecan = idecan;
        this.idib = idib;
    }

    @Override
    public Optional<Etapa> buscar(Instituicao instituicao, int id) {
        return switch (instituicao) {
            case IDECAN -> idecan.buscar(id);
            case IDIB -> idib.buscar(id);
        };
    }

    @Override
    public List<Etapa> listarPorFase(Instituicao instituicao, int faseId) {
        return switch (instituicao) {
            case IDECAN -> idecan.listarPorFase(faseId);
            case IDIB -> idib.listarPorFase(faseId);
        };
    }

    @Override
    public Etapa salvar(Instituicao instituicao, Etapa etapa) {
        return switch (instituicao) {
            case IDECAN -> idecan.salvar(etapa);
            case IDIB -> idib.salvar(etapa);
        };
    }

    @Override
    public void excluir(Instituicao instituicao, int id) {
        switch (instituicao) {
            case IDECAN -> idecan.excluir(id);
            case IDIB -> idib.excluir(id);
        }
    }
}

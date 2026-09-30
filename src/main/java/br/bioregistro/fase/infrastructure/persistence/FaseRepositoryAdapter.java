package br.bioregistro.fase.infrastructure.persistence;

import br.bioregistro.fase.domain.model.Fase;
import br.bioregistro.fase.domain.model.Instituicao;
import br.bioregistro.fase.domain.port.FaseRepository;
import br.bioregistro.fase.infrastructure.persistence.idecan.IdecanFases;
import br.bioregistro.fase.infrastructure.persistence.idib.IdibFases;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;

/** Escolhe o banco (datasource) da instituição. */
@ApplicationScoped
public class FaseRepositoryAdapter implements FaseRepository {

    private final IdecanFases idecan;
    private final IdibFases idib;

    public FaseRepositoryAdapter(IdecanFases idecan, IdibFases idib) {
        this.idecan = idecan;
        this.idib = idib;
    }

    @Override
    public Optional<Fase> buscar(Instituicao instituicao, int id) {
        return switch (instituicao) {
            case IDECAN -> idecan.buscar(id);
            case IDIB -> idib.buscar(id);
        };
    }

    @Override
    public List<Fase> listarPorEdital(Instituicao instituicao, int editalId) {
        return switch (instituicao) {
            case IDECAN -> idecan.listarPorEdital(editalId);
            case IDIB -> idib.listarPorEdital(editalId);
        };
    }

    @Override
    public List<Fase> listarPorGrupo(Instituicao instituicao, int grupoId) {
        return switch (instituicao) {
            case IDECAN -> idecan.listarPorGrupo(grupoId);
            case IDIB -> idib.listarPorGrupo(grupoId);
        };
    }

    @Override
    public short maiorOrdem(Instituicao instituicao, int editalId) {
        return switch (instituicao) {
            case IDECAN -> idecan.maiorOrdem(editalId);
            case IDIB -> idib.maiorOrdem(editalId);
        };
    }

    @Override
    public boolean editalExiste(Instituicao instituicao, int editalId) {
        return switch (instituicao) {
            case IDECAN -> idecan.editalExiste(editalId);
            case IDIB -> idib.editalExiste(editalId);
        };
    }

    @Override
    public boolean ordemEmUso(Instituicao instituicao, int editalId, short ordem, Integer ignorarFaseId) {
        return switch (instituicao) {
            case IDECAN -> idecan.ordemEmUso(editalId, ordem, ignorarFaseId);
            case IDIB -> idib.ordemEmUso(editalId, ordem, ignorarFaseId);
        };
    }

    @Override
    public Fase salvar(Instituicao instituicao, Fase fase) {
        return switch (instituicao) {
            case IDECAN -> idecan.salvar(fase);
            case IDIB -> idib.salvar(fase);
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

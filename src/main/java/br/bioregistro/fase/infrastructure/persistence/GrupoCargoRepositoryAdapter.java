package br.bioregistro.fase.infrastructure.persistence;

import br.bioregistro.fase.domain.model.Cargo;
import br.bioregistro.fase.domain.model.GrupoCargo;
import br.bioregistro.fase.domain.model.Instituicao;
import br.bioregistro.fase.domain.port.GrupoCargoRepository;
import br.bioregistro.fase.infrastructure.persistence.idecan.IdecanGrupos;
import br.bioregistro.fase.infrastructure.persistence.idib.IdibGrupos;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;

/** Escolhe o banco (datasource) da instituição. */
@ApplicationScoped
public class GrupoCargoRepositoryAdapter implements GrupoCargoRepository {

    private final IdecanGrupos idecan;
    private final IdibGrupos idib;

    public GrupoCargoRepositoryAdapter(IdecanGrupos idecan, IdibGrupos idib) {
        this.idecan = idecan;
        this.idib = idib;
    }

    @Override
    public Optional<GrupoCargo> buscar(Instituicao instituicao, int id) {
        return switch (instituicao) {
            case IDECAN -> idecan.buscar(id);
            case IDIB -> idib.buscar(id);
        };
    }

    @Override
    public List<GrupoCargo> listarPorEdital(Instituicao instituicao, int editalId) {
        return switch (instituicao) {
            case IDECAN -> idecan.listarPorEdital(editalId);
            case IDIB -> idib.listarPorEdital(editalId);
        };
    }

    @Override
    public boolean nomeEmUso(Instituicao instituicao, int editalId, String nome, Integer ignorarGrupoId) {
        return switch (instituicao) {
            case IDECAN -> idecan.nomeEmUso(editalId, nome, ignorarGrupoId);
            case IDIB -> idib.nomeEmUso(editalId, nome, ignorarGrupoId);
        };
    }

    @Override
    public GrupoCargo salvar(Instituicao instituicao, GrupoCargo grupo) {
        return switch (instituicao) {
            case IDECAN -> idecan.salvar(grupo);
            case IDIB -> idib.salvar(grupo);
        };
    }

    @Override
    public void excluir(Instituicao instituicao, int id) {
        switch (instituicao) {
            case IDECAN -> idecan.excluir(id);
            case IDIB -> idib.excluir(id);
        }
    }

    @Override
    public List<Cargo> cargosDoEdital(Instituicao instituicao, int editalId) {
        return switch (instituicao) {
            case IDECAN -> idecan.cargosDoEdital(editalId);
            case IDIB -> idib.cargosDoEdital(editalId);
        };
    }

    @Override
    public List<Integer> cargosDoGrupo(Instituicao instituicao, int grupoId) {
        return switch (instituicao) {
            case IDECAN -> idecan.cargosDoGrupo(grupoId);
            case IDIB -> idib.cargosDoGrupo(grupoId);
        };
    }

    @Override
    public Optional<Integer> grupoDoCargo(Instituicao instituicao, int cargoId) {
        return switch (instituicao) {
            case IDECAN -> idecan.grupoDoCargo(cargoId);
            case IDIB -> idib.grupoDoCargo(cargoId);
        };
    }

    @Override
    public void adicionarCargo(Instituicao instituicao, int grupoId, int cargoId) {
        switch (instituicao) {
            case IDECAN -> idecan.adicionarCargo(grupoId, cargoId);
            case IDIB -> idib.adicionarCargo(grupoId, cargoId);
        }
    }

    @Override
    public void removerCargo(Instituicao instituicao, int grupoId, int cargoId) {
        switch (instituicao) {
            case IDECAN -> idecan.removerCargo(grupoId, cargoId);
            case IDIB -> idib.removerCargo(grupoId, cargoId);
        }
    }

    @Override
    public void vincularFase(Instituicao instituicao, int faseId, int grupoId) {
        switch (instituicao) {
            case IDECAN -> idecan.vincularFase(faseId, grupoId);
            case IDIB -> idib.vincularFase(faseId, grupoId);
        }
    }

    @Override
    public void desvincularFase(Instituicao instituicao, int faseId) {
        switch (instituicao) {
            case IDECAN -> idecan.desvincularFase(faseId);
            case IDIB -> idib.desvincularFase(faseId);
        }
    }

    @Override
    public boolean possuiFases(Instituicao instituicao, int grupoId) {
        return switch (instituicao) {
            case IDECAN -> idecan.possuiFases(grupoId);
            case IDIB -> idib.possuiFases(grupoId);
        };
    }
}

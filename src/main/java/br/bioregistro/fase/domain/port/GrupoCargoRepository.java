package br.bioregistro.fase.domain.port;

import br.bioregistro.fase.domain.model.Cargo;
import br.bioregistro.fase.domain.model.GrupoCargo;
import br.bioregistro.fase.domain.model.Instituicao;

import java.util.List;
import java.util.Optional;

public interface GrupoCargoRepository {

    Optional<GrupoCargo> buscar(Instituicao instituicao, int id);

    List<GrupoCargo> listarPorEdital(Instituicao instituicao, int editalId);

    boolean nomeEmUso(Instituicao instituicao, int editalId, String nome, Integer ignorarGrupoId);

    /** Insere quando {@code grupo.id()} é nulo; senão atualiza. */
    GrupoCargo salvar(Instituicao instituicao, GrupoCargo grupo);

    void excluir(Instituicao instituicao, int id);

    List<Cargo> cargosDoEdital(Instituicao instituicao, int editalId);

    List<Integer> cargosDoGrupo(Instituicao instituicao, int grupoId);

    /** Grupo em que o cargo está, se estiver em algum. */
    Optional<Integer> grupoDoCargo(Instituicao instituicao, int cargoId);

    void adicionarCargo(Instituicao instituicao, int grupoId, int cargoId);

    void removerCargo(Instituicao instituicao, int grupoId, int cargoId);

    void vincularFase(Instituicao instituicao, int faseId, int grupoId);

    void desvincularFase(Instituicao instituicao, int faseId);

    boolean possuiFases(Instituicao instituicao, int grupoId);
}

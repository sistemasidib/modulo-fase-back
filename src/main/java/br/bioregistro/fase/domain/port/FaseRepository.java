package br.bioregistro.fase.domain.port;

import br.bioregistro.fase.domain.model.Fase;
import br.bioregistro.fase.domain.model.Instituicao;

import java.util.List;
import java.util.Optional;

public interface FaseRepository {

    Optional<Fase> buscar(Instituicao instituicao, int id);

    List<Fase> listarPorEdital(Instituicao instituicao, int editalId);

    List<Fase> listarPorGrupo(Instituicao instituicao, int grupoId);

    /** Maior ordem já usada no edital (0 se não houver fase). */
    short maiorOrdem(Instituicao instituicao, int editalId);

    boolean editalExiste(Instituicao instituicao, int editalId);

    /** Há outra fase no edital com esta ordem? {@code ignorarFaseId} exclui a própria fase na edição. */
    boolean ordemEmUso(Instituicao instituicao, int editalId, short ordem, Integer ignorarFaseId);

    /** Insere quando {@code fase.id()} é nulo; senão atualiza. Devolve a fase com id. */
    Fase salvar(Instituicao instituicao, Fase fase);

    void excluir(Instituicao instituicao, int id);
}

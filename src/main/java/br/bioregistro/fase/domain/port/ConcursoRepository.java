package br.bioregistro.fase.domain.port;

import br.bioregistro.fase.domain.model.Concurso;
import br.bioregistro.fase.domain.model.EditalResumo;
import br.bioregistro.fase.domain.model.Instituicao;

import java.util.List;
import java.util.Optional;

public interface ConcursoRepository {

    List<Concurso> listar(Instituicao instituicao);

    Optional<Concurso> buscar(Instituicao instituicao, int id);

    boolean nomeEmUso(Instituicao instituicao, String nome, Integer ignorarConcursoId);

    /** Insere quando {@code concurso.id()} é nulo; senão atualiza. */
    Concurso salvar(Instituicao instituicao, Concurso concurso);

    /** Exclui o concurso e os vínculos com editais. */
    void excluir(Instituicao instituicao, int id);

    List<EditalResumo> editais(Instituicao instituicao, int concursoId);

    Optional<Concurso> concursoDoEdital(Instituicao instituicao, int editalId);

    void incluirEdital(Instituicao instituicao, int concursoId, int editalId);

    void retirarEdital(Instituicao instituicao, int concursoId, int editalId);

    /** Editais não excluídos que não estão em nenhum concurso. */
    List<EditalResumo> editaisSemConcurso(Instituicao instituicao);
}

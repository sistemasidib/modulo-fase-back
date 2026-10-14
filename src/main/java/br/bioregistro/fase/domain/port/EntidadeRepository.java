package br.bioregistro.fase.domain.port;

import br.bioregistro.fase.domain.model.EditalResumo;
import br.bioregistro.fase.domain.model.Entidade;
import br.bioregistro.fase.domain.model.Instituicao;

import java.util.List;
import java.util.Optional;

public interface EntidadeRepository {

    List<Entidade> listar(Instituicao instituicao);

    Optional<Entidade> buscar(Instituicao instituicao, int id);

    boolean nomeEmUso(Instituicao instituicao, String nome, Integer ignorarEntidadeId);

    boolean cnpjEmUso(Instituicao instituicao, String cnpj, Integer ignorarEntidadeId);

    /** Insere quando {@code entidade.id()} é nulo; senão atualiza. */
    Entidade salvar(Instituicao instituicao, Entidade entidade);

    /** Exclui a entidade e os vínculos com editais. */
    void excluir(Instituicao instituicao, int id);

    List<EditalResumo> editais(Instituicao instituicao, int entidadeId);

    Optional<Entidade> entidadeDoEdital(Instituicao instituicao, int editalId);

    void incluirEdital(Instituicao instituicao, int entidadeId, int editalId);

    void retirarEdital(Instituicao instituicao, int entidadeId, int editalId);

    /** Editais não excluídos que não estão em nenhuma entidade. */
    List<EditalResumo> editaisSemEntidade(Instituicao instituicao);
}

package br.bioregistro.fase.domain.port;

import br.bioregistro.fase.domain.model.Etapa;
import br.bioregistro.fase.domain.model.Instituicao;

import java.util.List;
import java.util.Optional;

public interface EtapaRepository {

    Optional<Etapa> buscar(Instituicao instituicao, int id);

    List<Etapa> listarPorFase(Instituicao instituicao, int faseId);

    /** Insere quando {@code etapa.id()} é nulo; senão atualiza. Devolve a etapa com id. */
    Etapa salvar(Instituicao instituicao, Etapa etapa);

    void excluir(Instituicao instituicao, int id);
}

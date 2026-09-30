package br.bioregistro.fase.domain.port;

import br.bioregistro.fase.domain.model.Instituicao;
import br.bioregistro.fase.domain.model.Publicacao;

import java.util.List;
import java.util.Optional;

public interface PublicacaoRepository {

    Optional<Publicacao> buscar(Instituicao instituicao, int id);

    /** Publicações da fase e das etapas dela. */
    List<Publicacao> listarPorFase(Instituicao instituicao, int faseId);

    List<Publicacao> listarPorEtapa(Instituicao instituicao, int etapaId);

    /** Insere quando {@code publicacao.id()} é nulo; senão atualiza. */
    Publicacao salvar(Instituicao instituicao, Publicacao publicacao);

    void excluir(Instituicao instituicao, int id);
}

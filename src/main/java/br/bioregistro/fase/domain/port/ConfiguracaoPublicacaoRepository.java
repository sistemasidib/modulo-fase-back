package br.bioregistro.fase.domain.port;

import br.bioregistro.fase.domain.model.ConfiguracaoDePublicacao;
import br.bioregistro.fase.domain.model.Instituicao;

import java.util.Optional;

public interface ConfiguracaoPublicacaoRepository {

    Optional<ConfiguracaoDePublicacao> padrao(Instituicao instituicao, int tipoPublicacaoId);

    Optional<ConfiguracaoDePublicacao> doEdital(Instituicao instituicao, int editalId, int tipoPublicacaoId);

    /** Insere ou atualiza o override do edital. */
    ConfiguracaoDePublicacao salvarDoEdital(Instituicao instituicao, int editalId, ConfiguracaoDePublicacao config);

    void excluirDoEdital(Instituicao instituicao, int editalId, int tipoPublicacaoId);
}

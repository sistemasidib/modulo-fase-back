package br.bioregistro.fase.domain.port;

import br.bioregistro.fase.domain.model.Instituicao;
import br.bioregistro.fase.domain.model.TipoEtapa;
import br.bioregistro.fase.domain.model.TipoPublicacao;

import java.util.List;
import java.util.Optional;

/** Tabelas de domínio: tipos de etapa, tipos de publicação e publicações padrão por tipo de etapa. */
public interface TipoRepository {

    List<TipoEtapa> tiposEtapa(Instituicao instituicao);

    Optional<TipoEtapa> tipoEtapa(Instituicao instituicao, int id);

    List<TipoPublicacao> tiposPublicacao(Instituicao instituicao);

    Optional<TipoPublicacao> tipoPublicacao(Instituicao instituicao, int id);

    Optional<TipoPublicacao> tipoPublicacao(Instituicao instituicao, String valor);

    /** Espaços de publicação criados junto com uma etapa deste tipo. */
    List<TipoPublicacao> publicacoesPadrao(Instituicao instituicao, int tipoEtapaId);
}

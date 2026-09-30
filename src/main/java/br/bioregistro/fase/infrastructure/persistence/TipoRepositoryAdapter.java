package br.bioregistro.fase.infrastructure.persistence;

import br.bioregistro.fase.domain.model.TipoEtapa;
import br.bioregistro.fase.domain.model.TipoPublicacao;
import br.bioregistro.fase.domain.model.Instituicao;
import br.bioregistro.fase.domain.port.TipoRepository;
import br.bioregistro.fase.infrastructure.persistence.idecan.IdecanTipos;
import br.bioregistro.fase.infrastructure.persistence.idib.IdibTipos;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;

/** Escolhe o banco (datasource) da instituição. */
@ApplicationScoped
public class TipoRepositoryAdapter implements TipoRepository {

    private final IdecanTipos idecan;
    private final IdibTipos idib;

    public TipoRepositoryAdapter(IdecanTipos idecan, IdibTipos idib) {
        this.idecan = idecan;
        this.idib = idib;
    }

    @Override
    public List<TipoEtapa> tiposEtapa(Instituicao instituicao) {
        return switch (instituicao) {
            case IDECAN -> idecan.tiposEtapa();
            case IDIB -> idib.tiposEtapa();
        };
    }

    @Override
    public Optional<TipoEtapa> tipoEtapa(Instituicao instituicao, int id) {
        return switch (instituicao) {
            case IDECAN -> idecan.tipoEtapa(id);
            case IDIB -> idib.tipoEtapa(id);
        };
    }

    @Override
    public List<TipoPublicacao> tiposPublicacao(Instituicao instituicao) {
        return switch (instituicao) {
            case IDECAN -> idecan.tiposPublicacao();
            case IDIB -> idib.tiposPublicacao();
        };
    }

    @Override
    public Optional<TipoPublicacao> tipoPublicacao(Instituicao instituicao, int id) {
        return switch (instituicao) {
            case IDECAN -> idecan.tipoPublicacao(id);
            case IDIB -> idib.tipoPublicacao(id);
        };
    }

    @Override
    public Optional<TipoPublicacao> tipoPublicacao(Instituicao instituicao, String valor) {
        return switch (instituicao) {
            case IDECAN -> idecan.tipoPublicacao(valor);
            case IDIB -> idib.tipoPublicacao(valor);
        };
    }

    @Override
    public List<TipoPublicacao> publicacoesPadrao(Instituicao instituicao, int tipoEtapaId) {
        return switch (instituicao) {
            case IDECAN -> idecan.publicacoesPadrao(tipoEtapaId);
            case IDIB -> idib.publicacoesPadrao(tipoEtapaId);
        };
    }
}

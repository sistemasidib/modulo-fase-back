package br.bioregistro.fase.infrastructure.persistence;

import br.bioregistro.fase.domain.model.Publicacao;
import br.bioregistro.fase.domain.model.Instituicao;
import br.bioregistro.fase.domain.port.PublicacaoRepository;
import br.bioregistro.fase.infrastructure.persistence.idecan.IdecanPublicacoes;
import br.bioregistro.fase.infrastructure.persistence.idib.IdibPublicacoes;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;

/** Escolhe o banco (datasource) da instituição. */
@ApplicationScoped
public class PublicacaoRepositoryAdapter implements PublicacaoRepository {

    private final IdecanPublicacoes idecan;
    private final IdibPublicacoes idib;

    public PublicacaoRepositoryAdapter(IdecanPublicacoes idecan, IdibPublicacoes idib) {
        this.idecan = idecan;
        this.idib = idib;
    }

    @Override
    public Optional<Publicacao> buscar(Instituicao instituicao, int id) {
        return switch (instituicao) {
            case IDECAN -> idecan.buscar(id);
            case IDIB -> idib.buscar(id);
        };
    }

    @Override
    public List<Publicacao> listarPorFase(Instituicao instituicao, int faseId) {
        return switch (instituicao) {
            case IDECAN -> idecan.listarPorFase(faseId);
            case IDIB -> idib.listarPorFase(faseId);
        };
    }

    @Override
    public List<Publicacao> listarPorEtapa(Instituicao instituicao, int etapaId) {
        return switch (instituicao) {
            case IDECAN -> idecan.listarPorEtapa(etapaId);
            case IDIB -> idib.listarPorEtapa(etapaId);
        };
    }

    @Override
    public Publicacao salvar(Instituicao instituicao, Publicacao publicacao) {
        return switch (instituicao) {
            case IDECAN -> idecan.salvar(publicacao);
            case IDIB -> idib.salvar(publicacao);
        };
    }

    @Override
    public void excluir(Instituicao instituicao, int id) {
        switch (instituicao) {
            case IDECAN -> idecan.excluir(id);
            case IDIB -> idib.excluir(id);
        }
    }
}

package br.bioregistro.fase.application.use_cases;

import br.bioregistro.fase.domain.exception.ConflitoException;
import br.bioregistro.fase.domain.exception.NaoEncontradoException;
import br.bioregistro.fase.domain.model.Instituicao;
import br.bioregistro.fase.domain.model.Publicacao;
import br.bioregistro.fase.domain.port.PublicacaoRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

/** Remove um espaço de publicação que ainda não foi publicado. */
@ApplicationScoped
public class ExcluirPublicacao {

    private final PublicacaoRepository publicacoes;

    public ExcluirPublicacao(PublicacaoRepository publicacoes) {
        this.publicacoes = publicacoes;
    }

    @Transactional
    public void executar(Instituicao instituicao, int id) {
        Publicacao publicacao = publicacoes.buscar(instituicao, id)
                .orElseThrow(() -> new NaoEncontradoException("Publicação", id));
        if (publicacao.publicada()) {
            throw new ConflitoException("A publicação " + id + " já foi publicada e não pode ser excluída.");
        }
        publicacoes.excluir(instituicao, id);
    }
}

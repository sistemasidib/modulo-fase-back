package br.bioregistro.fase.application.use_cases;

import br.bioregistro.fase.domain.exception.ConflitoException;
import br.bioregistro.fase.domain.model.Instituicao;
import br.bioregistro.fase.domain.model.Publicacao;
import br.bioregistro.fase.domain.model.TipoPublicacao;
import br.bioregistro.fase.domain.port.PublicacaoRepository;
import br.bioregistro.fase.domain.port.TipoRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

/**
 * Espaços de publicação que nascem com a fase (resultado preliminar e oficial) e com a
 * etapa (conforme {@code dom_tipo_etapa_publicacao}), e sua remoção junto com elas.
 */
@ApplicationScoped
public class EspacosDePublicacao {

    private static final List<String> DA_FASE =
            List.of(TipoPublicacao.RESULTADO_PRELIMINAR, TipoPublicacao.RESULTADO_OFICIAL);

    private final TipoRepository tipos;
    private final PublicacaoRepository publicacoes;

    public EspacosDePublicacao(TipoRepository tipos, PublicacaoRepository publicacoes) {
        this.tipos = tipos;
        this.publicacoes = publicacoes;
    }

    public void criarParaFase(Instituicao instituicao, int faseId) {
        for (String valor : DA_FASE) {
            TipoPublicacao tipo = tipos.tipoPublicacao(instituicao, valor)
                    .orElseThrow(() -> new IllegalStateException(
                            "dom_tipo_publicacao sem o valor '" + valor + "' em " + instituicao + "."));
            publicacoes.salvar(instituicao, Publicacao.espaco(faseId, null, tipo));
        }
    }

    public void criarParaEtapa(Instituicao instituicao, int faseId, int etapaId, int tipoEtapaId) {
        for (TipoPublicacao tipo : tipos.publicacoesPadrao(instituicao, tipoEtapaId)) {
            publicacoes.salvar(instituicao, Publicacao.espaco(faseId, etapaId, tipo));
        }
    }

    public void removerDaFase(Instituicao instituicao, int faseId) {
        remover(instituicao, publicacoes.listarPorFase(instituicao, faseId).stream()
                .filter(p -> p.etapaId() == null)
                .toList(), "da fase " + faseId);
    }

    public void removerDaEtapa(Instituicao instituicao, int etapaId) {
        remover(instituicao, publicacoes.listarPorEtapa(instituicao, etapaId), "da etapa " + etapaId);
    }

    /**
     * Provisório até a pendência 11 da spec 001: só remove espaços ainda vazios; se algum
     * já tiver data ou publicação, recusa a exclusão.
     */
    private void remover(Instituicao instituicao, List<Publicacao> espacos, String dono) {
        if (espacos.stream().anyMatch(Publicacao::preenchida)) {
            throw new ConflitoException("Há publicações " + dono + " com datas preenchidas ou já publicadas.");
        }
        espacos.forEach(p -> publicacoes.excluir(instituicao, p.id()));
    }
}

package br.bioregistro.fase.application.use_cases;

import br.bioregistro.fase.domain.exception.ConflitoException;
import br.bioregistro.fase.domain.exception.NaoEncontradoException;
import br.bioregistro.fase.domain.model.Instituicao;
import br.bioregistro.fase.domain.model.Modalidade;
import br.bioregistro.fase.domain.model.Publicacao;
import br.bioregistro.fase.domain.model.TipoPublicacao;
import br.bioregistro.fase.domain.port.PublicacaoRepository;
import br.bioregistro.fase.domain.port.TipoRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Espaços de publicação da fase (resultado preliminar e oficial) e da etapa (conforme
 * {@code dom_tipo_etapa_publicacao}), e sua remoção junto com elas.
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

    /**
     * A fase só tem resultado próprio quando ele não repete o de uma etapa: se uma única etapa
     * publica resultado, o resultado dela é o da fase. Chamar sempre que as etapas mudarem.
     * Espaços da fase já preenchidos nunca são removidos.
     */
    public void sincronizarResultadoDaFase(Instituicao instituicao, int faseId) {
        List<Publicacao> todas = publicacoes.listarPorFase(instituicao, faseId);
        long etapasComResultado = todas.stream()
                .filter(p -> p.etapaId() != null && ehResultado(p))
                .map(Publicacao::etapaId)
                .distinct()
                .count();
        List<Publicacao> daFase = todas.stream().filter(p -> p.etapaId() == null && ehResultado(p)).toList();

        if (etapasComResultado == 1) {
            daFase.stream().filter(p -> !p.preenchida()).forEach(p -> publicacoes.excluir(instituicao, p.id()));
            return;
        }
        for (String valor : DA_FASE) {
            if (daFase.stream().noneMatch(p -> p.tipo().valor().equals(valor))) {
                publicacoes.salvar(instituicao, Publicacao.espaco(faseId, null, tipoPorValor(instituicao, valor)));
            }
        }
    }

    private static boolean ehResultado(Publicacao p) {
        return DA_FASE.contains(p.tipo().valor());
    }

    /**
     * Troca o espaço de locais ↔ link conforme a modalidade S05 (RN18). Espaço já publicado
     * não muda; devolve aviso para a tela.
     *
     * @return aviso quando o espaço publicado impede a troca; {@code null} se trocou ou não há o que fazer
     */
    public String ajustarEspacoDaModalidade(Instituicao instituicao, int faseId, int etapaId,
                                            Modalidade anterior, Modalidade nova) {
        if (nova == null || Objects.equals(anterior, nova)) {
            return null;
        }
        boolean remota = nova != null && nova.remota();
        String de = remota ? TipoPublicacao.LOCAL_PROVA : TipoPublicacao.LINK_ACESSO;
        String para = remota ? TipoPublicacao.LINK_ACESSO : TipoPublicacao.LOCAL_PROVA;
        Optional<Publicacao> atual = publicacoes.listarPorEtapa(instituicao, etapaId).stream()
                .filter(p -> de.equals(p.tipo().valor()) || para.equals(p.tipo().valor()))
                .findFirst();
        if (atual.isEmpty()) {
            TipoPublicacao tipo = tipoPorValor(instituicao, para);
            publicacoes.salvar(instituicao, Publicacao.espaco(faseId, etapaId, tipo));
            return null;
        }
        Publicacao espaco = atual.get();
        if (para.equals(espaco.tipo().valor())) {
            return null;
        }
        if (espaco.publicada()) {
            return "A publicação de " + espaco.tipo().descricao()
                    + " já está publicada: o tipo não muda. Ajuste no cronograma ou publique o link à parte.";
        }
        publicacoes.excluir(instituicao, espaco.id());
        publicacoes.salvar(instituicao, Publicacao.espaco(faseId, etapaId, tipoPorValor(instituicao, para)));
        return null;
    }

    public void criarParaEtapa(Instituicao instituicao, int faseId, int etapaId, int tipoEtapaId) {
        criarParaEtapa(instituicao, faseId, etapaId, tipoEtapaId, null);
    }

    /**
     * @param escolhidos ids dos tipos de publicação; nulo = os padrão do tipo de etapa. O resultado
     *                   (preliminar e oficial) entra sempre.
     */
    public void criarParaEtapa(Instituicao instituicao, int faseId, int etapaId, int tipoEtapaId,
                               List<Integer> escolhidos) {
        List<TipoPublicacao> lista = escolhidos == null
                ? tipos.publicacoesPadrao(instituicao, tipoEtapaId)
                : escolhidosComResultado(instituicao, escolhidos);
        for (TipoPublicacao tipo : lista) {
            publicacoes.salvar(instituicao, Publicacao.espaco(faseId, etapaId, tipo));
        }
    }

    /**
     * Deixa a etapa com um espaço de cada tipo escolhido: cria os que faltam e remove os de tipos
     * desmarcados. Espaço preenchido de tipo desmarcado impede a mudança ({@link ConflitoException}).
     */
    public void definirDaEtapa(Instituicao instituicao, int faseId, int etapaId, List<TipoPublicacao> escolhidos) {
        List<Publicacao> atuais = publicacoes.listarPorEtapa(instituicao, etapaId);
        List<Publicacao> sobrando = atuais.stream()
                .filter(p -> escolhidos.stream().noneMatch(t -> t.id().equals(p.tipo().id())))
                .toList();
        remover(instituicao, sobrando, "da etapa " + etapaId + " nos tipos desmarcados");
        for (TipoPublicacao tipo : escolhidos) {
            if (atuais.stream().noneMatch(p -> p.tipo().id().equals(tipo.id()))) {
                publicacoes.salvar(instituicao, Publicacao.espaco(faseId, etapaId, tipo));
            }
        }
    }

    /**
     * Valida e resolve os ids (sem repetição, na ordem recebida) e acrescenta o resultado preliminar
     * e oficial: toda etapa escolhida pelo usuário gera resultado.
     */
    public List<TipoPublicacao> escolhidosComResultado(Instituicao instituicao, List<Integer> ids) {
        List<TipoPublicacao> escolhidos = new ArrayList<>(ids.stream()
                .filter(Objects::nonNull)
                .distinct()
                .map(id -> tipos.tipoPublicacao(instituicao, id)
                        .orElseThrow(() -> new NaoEncontradoException("Tipo de publicação", id)))
                .toList());
        for (String valor : DA_FASE) {
            if (escolhidos.stream().noneMatch(t -> t.valor().equals(valor))) {
                escolhidos.add(tipoPorValor(instituicao, valor));
            }
        }
        return escolhidos;
    }

    private TipoPublicacao tipoPorValor(Instituicao instituicao, String valor) {
        return tipos.tipoPublicacao(instituicao, valor)
                .orElseThrow(() -> new IllegalStateException(
                        "dom_tipo_publicacao sem o valor '" + valor + "' em " + instituicao + "."));
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

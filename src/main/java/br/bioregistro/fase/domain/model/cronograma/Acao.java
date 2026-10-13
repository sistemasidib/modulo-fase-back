package br.bioregistro.fase.domain.model.cronograma;

import br.bioregistro.fase.domain.exception.RegraDeNegocioException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;

/**
 * Linha do histórico (RN27): só de inclusão. Uma cascata é uma ação com N itens.
 *
 * @param quando   {@code null} = agora (preenchido ao gravar)
 * @param editalId edital cuja sobreposição mudou; {@code null} = o cronograma compartilhado
 */
public record Acao(Long id, String autor, OffsetDateTime quando, String acao, String detalhe, String origem,
                   String justificativa, List<Previa.Item> itens, Integer editalId) {

    public static final String GERACAO = "geração automática";
    public static final String EDICAO = "edição manual";
    public static final String RECALCULO = "edição manual + recálculo";
    public static final String DESFAZER = "desfazer";
    public static final String SALVAMENTO = "salvamento";

    public static final Set<String> ORIGENS = Set.of(GERACAO, EDICAO, RECALCULO, DESFAZER, SALVAMENTO);

    private static final int TEXTO_MAX = 500;

    public Acao {
        if (acao == null || acao.isBlank()) {
            throw new RegraDeNegocioException("A ação do histórico precisa de descrição.");
        }
        if (!ORIGENS.contains(origem)) {
            throw new RegraDeNegocioException("Origem do histórico inválida: " + origem + ".");
        }
        acao = corta(acao.strip());
        detalhe = detalhe == null ? null : corta(detalhe);
        itens = List.copyOf(itens == null ? List.of() : itens);
    }

    public static Acao de(String autor, String acao, String detalhe, String origem, List<Previa.Item> itens) {
        return new Acao(null, autor, null, acao, detalhe, origem, null, itens, null);
    }

    public Acao comAutor(String novoAutor) {
        return new Acao(id, novoAutor, quando, acao, detalhe, origem, justificativa, itens, editalId);
    }

    public Acao doEdital(Integer edital) {
        return new Acao(id, autor, quando, acao, detalhe, origem, justificativa, itens, edital);
    }

    private static String corta(String texto) {
        return texto.length() > TEXTO_MAX ? texto.substring(0, TEXTO_MAX) : texto;
    }
}

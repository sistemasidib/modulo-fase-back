package br.bioregistro.fase.domain.model;

import br.bioregistro.fase.domain.exception.ConflitoException;
import br.bioregistro.fase.domain.exception.RegraDeNegocioException;

import java.time.OffsetDateTime;

/**
 * Espaço de publicação de uma fase ({@code etapaId} nulo) ou de uma etapa.
 * Todas as datas são informadas pelo usuário. Publicar exige o período de visualização
 * e, quando o tipo abre recurso, o período de recurso.
 */
public record Publicacao(
        Integer id,
        Integer faseId,
        Integer etapaId,
        TipoPublicacao tipo,
        OffsetDateTime dataPrevista,
        OffsetDateTime visualizacaoInicio,
        OffsetDateTime visualizacaoFim,
        OffsetDateTime recursoInicio,
        OffsetDateTime recursoFim,
        OffsetDateTime publicadaEm
) {

    public Publicacao {
        if (faseId == null || tipo == null) {
            throw new RegraDeNegocioException("A publicação precisa de fase e tipo.");
        }
        periodo(visualizacaoInicio, visualizacaoFim, "visualização");
        periodo(recursoInicio, recursoFim, "recurso");
        if (!tipo.abreRecurso() && (recursoInicio != null || recursoFim != null)) {
            throw new RegraDeNegocioException("Publicação do tipo " + tipo.descricao() + " não abre recurso.");
        }
    }

    /** Espaço vazio, criado junto com a fase ou a etapa. */
    public static Publicacao espaco(Integer faseId, Integer etapaId, TipoPublicacao tipo) {
        return new Publicacao(null, faseId, etapaId, tipo, null, null, null, null, null, null);
    }

    public Publicacao comDatas(OffsetDateTime dataPrevista,
                               OffsetDateTime visualizacaoInicio, OffsetDateTime visualizacaoFim,
                               OffsetDateTime recursoInicio, OffsetDateTime recursoFim) {
        return new Publicacao(id, faseId, etapaId, tipo, dataPrevista, visualizacaoInicio, visualizacaoFim,
                recursoInicio, recursoFim, publicadaEm);
    }

    /** Publicar abre o recurso: a partir daqui ele fica aberto dentro do período informado. */
    public Publicacao publicar(OffsetDateTime agora) {
        if (publicada()) {
            throw new ConflitoException("A publicação " + id + " já foi publicada.");
        }
        if (visualizacaoInicio == null || visualizacaoFim == null) {
            throw new RegraDeNegocioException("Informe o início e o fim da visualização do documento.");
        }
        if (tipo.abreRecurso() && (recursoInicio == null || recursoFim == null)) {
            throw new RegraDeNegocioException("Informe o início e o fim do período de recurso.");
        }
        return new Publicacao(id, faseId, etapaId, tipo, dataPrevista, visualizacaoInicio, visualizacaoFim,
                recursoInicio, recursoFim, agora);
    }

    public boolean publicada() {
        return publicadaEm != null;
    }

    public boolean recursoAberto(OffsetDateTime agora) {
        return publicada() && tipo.abreRecurso()
                && !agora.isBefore(recursoInicio) && !agora.isAfter(recursoFim);
    }

    /** Já recebeu alguma informação do usuário (data ou publicação). */
    public boolean preenchida() {
        return publicada() || dataPrevista != null || visualizacaoInicio != null || visualizacaoFim != null
                || recursoInicio != null || recursoFim != null;
    }

    public Publicacao comId(Integer novoId) {
        return new Publicacao(novoId, faseId, etapaId, tipo, dataPrevista, visualizacaoInicio, visualizacaoFim,
                recursoInicio, recursoFim, publicadaEm);
    }

    private static void periodo(OffsetDateTime inicio, OffsetDateTime fim, String nome) {
        if ((inicio == null) != (fim == null)) {
            throw new RegraDeNegocioException("Informe o início e o fim do período de " + nome + ".");
        }
        if (inicio != null && fim.isBefore(inicio)) {
            throw new RegraDeNegocioException("O fim do período de " + nome + " não pode ser anterior ao início.");
        }
    }
}

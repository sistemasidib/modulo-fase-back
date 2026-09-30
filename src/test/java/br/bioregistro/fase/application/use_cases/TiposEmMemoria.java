package br.bioregistro.fase.application.use_cases;

import br.bioregistro.fase.domain.model.Instituicao;
import br.bioregistro.fase.domain.model.TipoEtapa;
import br.bioregistro.fase.domain.model.TipoPublicacao;
import br.bioregistro.fase.domain.port.TipoRepository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Mesmos valores do script {@code db/001-fases-e-etapas.sql}, resumidos. */
class TiposEmMemoria implements TipoRepository {

    static final int PROVA_OBJETIVA = 1;
    static final int TAF = 2;

    static final TipoPublicacao LOCAL_PROVA = new TipoPublicacao(1, "local_prova", "Local de prova", false);
    static final TipoPublicacao GABARITO_PRELIMINAR =
            new TipoPublicacao(2, "gabarito_preliminar", "Gabarito preliminar", true);
    static final TipoPublicacao GABARITO_DEFINITIVO =
            new TipoPublicacao(3, "gabarito_definitivo", "Gabarito definitivo", false);
    static final TipoPublicacao RESULTADO_PRELIMINAR =
            new TipoPublicacao(4, "resultado_preliminar", "Resultado preliminar", true);
    static final TipoPublicacao RESULTADO_OFICIAL =
            new TipoPublicacao(5, "resultado_oficial", "Resultado oficial", false);

    private static final List<TipoEtapa> TIPOS_ETAPA = List.of(
            new TipoEtapa(PROVA_OBJETIVA, "prova_objetiva", "Prova objetiva"),
            new TipoEtapa(TAF, "taf", "Teste de aptidão física (TAF)"));

    private static final List<TipoPublicacao> TIPOS_PUBLICACAO =
            List.of(LOCAL_PROVA, GABARITO_PRELIMINAR, GABARITO_DEFINITIVO, RESULTADO_PRELIMINAR, RESULTADO_OFICIAL);

    private static final Map<Integer, List<TipoPublicacao>> PADRAO = Map.of(
            PROVA_OBJETIVA, TIPOS_PUBLICACAO,
            TAF, List.of(RESULTADO_PRELIMINAR, RESULTADO_OFICIAL));

    @Override
    public List<TipoEtapa> tiposEtapa(Instituicao instituicao) {
        return TIPOS_ETAPA;
    }

    @Override
    public Optional<TipoEtapa> tipoEtapa(Instituicao instituicao, int id) {
        return TIPOS_ETAPA.stream().filter(t -> t.id() == id).findFirst();
    }

    @Override
    public List<TipoPublicacao> tiposPublicacao(Instituicao instituicao) {
        return TIPOS_PUBLICACAO;
    }

    @Override
    public Optional<TipoPublicacao> tipoPublicacao(Instituicao instituicao, int id) {
        return TIPOS_PUBLICACAO.stream().filter(t -> t.id() == id).findFirst();
    }

    @Override
    public Optional<TipoPublicacao> tipoPublicacao(Instituicao instituicao, String valor) {
        return TIPOS_PUBLICACAO.stream().filter(t -> t.valor().equals(valor)).findFirst();
    }

    @Override
    public List<TipoPublicacao> publicacoesPadrao(Instituicao instituicao, int tipoEtapaId) {
        return PADRAO.getOrDefault(tipoEtapaId, List.of());
    }
}

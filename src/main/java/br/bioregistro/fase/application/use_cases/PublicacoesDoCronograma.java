package br.bioregistro.fase.application.use_cases;

import br.bioregistro.fase.domain.exception.ConflitoException;
import br.bioregistro.fase.domain.model.ConfiguracaoDePublicacao;
import br.bioregistro.fase.domain.model.Fase;
import br.bioregistro.fase.domain.model.Instituicao;
import br.bioregistro.fase.domain.model.Publicacao;
import br.bioregistro.fase.domain.model.cronograma.LinhaDoCronograma;
import br.bioregistro.fase.domain.model.cronograma.StatusEtapa;
import br.bioregistro.fase.domain.port.ConfiguracaoPublicacaoRepository;
import br.bioregistro.fase.domain.port.FaseRepository;
import br.bioregistro.fase.domain.port.VinculoCronogramaRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Publicações vinculadas ao cronograma (spec 005): as datas são lidas da linha do cronograma pela
 * FK, na hora, e nunca digitadas. O período de visualização usa os dias no ar da configuração
 * (spec 006, RN22): saída + dias_no_ar − 1. Sem configuração, o fim é o fim do dia da linha.
 */
@ApplicationScoped
public class PublicacoesDoCronograma {

    static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");

    private final VinculoCronogramaRepository vinculos;
    private final EncerramentoDoLegado legado;
    private final FaseRepository fases;
    private final ConfiguracaoPublicacaoRepository configs;

    public PublicacoesDoCronograma(VinculoCronogramaRepository vinculos, EncerramentoDoLegado legado,
                                   FaseRepository fases, ConfiguracaoPublicacaoRepository configs) {
        this.vinculos = vinculos;
        this.legado = legado;
        this.fases = fases;
        this.configs = configs;
    }

    /** As publicações da fase, com as datas do cronograma nas que são vinculadas. */
    public List<Publicacao> comDatas(Instituicao instituicao, int faseId, List<Publicacao> publicacoes) {
        Map<Integer, LinhaDoCronograma.DaPublicacao> vinculadas = vinculos.publicacoesDaFase(instituicao, faseId);
        Optional<Integer> editalId = fases.buscar(instituicao, faseId).map(Fase::editalId);
        return publicacoes.stream()
                .map(p -> vinculadas.containsKey(p.id())
                        ? comDatas(instituicao, editalId.orElse(null), p, vinculadas.get(p.id()))
                        : p)
                .toList();
    }

    public void exigirDatasManuais(Instituicao instituicao, Publicacao publicacao) {
        exigirManual(instituicao, publicacao.id());
        legado.exigirAberto(instituicao, publicacao.faseId());
    }

    public void exigirManual(Instituicao instituicao, int publicacaoId) {
        vinculos.daPublicacao(instituicao, publicacaoId).ifPresent(v -> {
            throw new ConflitoException("A publicação " + publicacaoId + " segue a etapa " + v.data().codigo()
                    + " do cronograma: datas e publicação mudam no cronograma do edital.");
        });
    }

    private Publicacao comDatas(Instituicao instituicao, Integer editalId, Publicacao p,
                                LinhaDoCronograma.DaPublicacao linhas) {
        LinhaDoCronograma data = linhas.data();
        LinhaDoCronograma recurso = linhas.recurso();
        OffsetDateTime inicio = data.comData() ? inicioDoDia(data.inicio()) : null;
        OffsetDateTime fim = data.comData() ? fimVisualizacao(instituicao, editalId, p, data) : null;
        boolean comRecurso = recurso != null && recurso.comData();
        OffsetDateTime publicadaEm = inicio != null && data.status() == StatusEtapa.PUBLICADO ? inicio : null;
        return p.comDatasDoCronograma(inicio, inicio, fim,
                comRecurso ? inicioDoDia(recurso.inicio()) : null, comRecurso ? fimDoDia(recurso.fim()) : null,
                publicadaEm);
    }

    private OffsetDateTime fimVisualizacao(Instituicao instituicao, Integer editalId, Publicacao p,
                                           LinhaDoCronograma data) {
        if (editalId != null && p.tipo() != null && p.tipo().id() != null) {
            Optional<ConfiguracaoDePublicacao> config = configs.doEdital(instituicao, editalId, p.tipo().id())
                    .or(() -> configs.padrao(instituicao, p.tipo().id()));
            if (config.isPresent()) {
                LocalDate fim = config.get().fimDaVisualizacao(data.inicio());
                return fim == null ? null : fimDoDia(fim);
            }
        }
        return fimDoDia(data.fim());
    }

    static OffsetDateTime inicioDoDia(LocalDate dia) {
        return dia.atStartOfDay(FUSO).toOffsetDateTime();
    }

    static OffsetDateTime fimDoDia(LocalDate dia) {
        return dia.atTime(LocalTime.of(23, 59, 59)).atZone(FUSO).toOffsetDateTime();
    }
}

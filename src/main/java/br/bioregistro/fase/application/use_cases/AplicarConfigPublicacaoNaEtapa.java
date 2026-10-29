package br.bioregistro.fase.application.use_cases;

import br.bioregistro.fase.domain.model.ConfiguracaoDePublicacao;
import br.bioregistro.fase.domain.model.Etapa;
import br.bioregistro.fase.domain.model.Fase;
import br.bioregistro.fase.domain.model.Instituicao;
import br.bioregistro.fase.domain.model.Publicacao;
import br.bioregistro.fase.domain.model.TipoEtapa;
import br.bioregistro.fase.domain.port.ConfiguracaoPublicacaoRepository;
import br.bioregistro.fase.domain.port.EtapaRepository;
import br.bioregistro.fase.domain.port.FaseRepository;
import br.bioregistro.fase.domain.port.PublicacaoRepository;
import br.bioregistro.fase.domain.port.TipoRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Preenche períodos de visualização (e recurso) dos espaços da etapa a partir da configuração
 * efetiva do edital e das datas da etapa âncora (spec 006 / RN07 / RN10).
 * Espaços já publicados não são alterados; demais são recalculados quando a âncora muda.
 */
@ApplicationScoped
public class AplicarConfigPublicacaoNaEtapa {

    /** Fallback se tipo e etapa não definirem duração (comportamento histórico). */
    private static final int DIAS_RECURSO_PADRAO = 2;

    private final PublicacaoRepository publicacoes;
    private final ConfiguracaoPublicacaoRepository configs;
    private final FaseRepository fases;
    private final EtapaRepository etapas;
    private final TipoRepository tipos;

    public AplicarConfigPublicacaoNaEtapa(PublicacaoRepository publicacoes,
                                          ConfiguracaoPublicacaoRepository configs,
                                          FaseRepository fases, EtapaRepository etapas,
                                          TipoRepository tipos) {
        this.publicacoes = publicacoes;
        this.configs = configs;
        this.fases = fases;
        this.etapas = etapas;
        this.tipos = tipos;
    }

    public void executar(Instituicao instituicao, Etapa etapa) {
        if (etapa == null || etapa.id() == null) {
            return;
        }
        Optional<Fase> fase = fases.buscar(instituicao, etapa.faseId());
        if (fase.isEmpty()) {
            return;
        }
        int editalId = fase.get().editalId();
        List<Etapa> daFase = etapas.listarPorFase(instituicao, etapa.faseId());
        for (Publicacao espaco : publicacoes.listarPorEtapa(instituicao, etapa.id())) {
            aplicar(instituicao, editalId, etapa, daFase, espaco);
        }
    }

    private void aplicar(Instituicao instituicao, int editalId, Etapa etapaDona,
                         List<Etapa> daFase, Publicacao espaco) {
        if (espaco.publicada()) {
            return;
        }
        if (espaco.tipo() == null || espaco.tipo().id() == null) {
            return;
        }
        Optional<ConfiguracaoDePublicacao> config = configs.doEdital(instituicao, editalId, espaco.tipo().id())
                .or(() -> configs.padrao(instituicao, espaco.tipo().id()));
        if (config.isEmpty()) {
            return;
        }
        LocalDate ancora = ancora(instituicao, config.get(), etapaDona, daFase);
        if (ancora == null) {
            return;
        }
        int diasApos = config.get().diasAposReferencia() == null ? 0 : config.get().diasAposReferencia();
        LocalDate saida = ancora.plusDays(diasApos);
        LocalDate fimViz = config.get().fimDaVisualizacao(saida);
        OffsetDateTime inicio = PublicacoesDoCronograma.inicioDoDia(saida);
        OffsetDateTime fim = PublicacoesDoCronograma.fimDoDia(fimViz);
        OffsetDateTime recursoInicio = null;
        OffsetDateTime recursoFim = null;
        if (espaco.tipo().abreRecurso()) {
            int diasRecurso = diasRecurso(instituicao, etapaDona);
            recursoInicio = inicio;
            recursoFim = PublicacoesDoCronograma.fimDoDia(saida.plusDays(diasRecurso - 1L));
        }
        publicacoes.salvar(instituicao, espaco.comDatas(inicio, inicio, fim, recursoInicio, recursoFim));
    }

    private int diasRecurso(Instituicao instituicao, Etapa etapa) {
        if (etapa.diasRecurso() != null) {
            return etapa.diasRecurso();
        }
        return tipos.tipoEtapa(instituicao, etapa.tipoId())
                .map(TipoEtapa::diasRecursoPadrao)
                .filter(d -> d != null && d >= 1)
                .orElse(DIAS_RECURSO_PADRAO);
    }

    private LocalDate ancora(Instituicao instituicao, ConfiguracaoDePublicacao config, Etapa etapaDona,
                             List<Etapa> daFase) {
        String ref = config.referenciaCodigo();
        if (ref == null || ref.isBlank()) {
            return dataAncora(etapaDona);
        }
        Optional<Etapa> referida = daFase.stream()
                .filter(e -> tipos.tipoEtapa(instituicao, e.tipoId())
                        .map(TipoEtapa::valor)
                        .map(ref::equals)
                        .orElse(false))
                .findFirst();
        return referida.map(this::dataAncora).orElse(null);
    }

    private LocalDate dataAncora(Etapa etapa) {
        if (etapa.dataFechamento() != null) {
            return etapa.dataFechamento().toLocalDate();
        }
        if (etapa.dataAbertura() != null) {
            return etapa.dataAbertura().toLocalDate();
        }
        return null;
    }
}

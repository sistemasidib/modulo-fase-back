package br.bioregistro.fase.application.use_cases;

import br.bioregistro.fase.domain.model.Etapa;
import br.bioregistro.fase.domain.model.Fase;
import br.bioregistro.fase.domain.model.Instituicao;
import br.bioregistro.fase.domain.model.Publicacao;
import br.bioregistro.fase.domain.model.TipoEtapa;
import br.bioregistro.fase.domain.model.TipoPublicacao;
import br.bioregistro.fase.domain.model.cronograma.CronogramaCalculado;
import br.bioregistro.fase.domain.model.cronograma.EditalDoCronograma;
import br.bioregistro.fase.domain.model.cronograma.EtapaCalculada;
import br.bioregistro.fase.domain.model.cronograma.ModuloDeExecucao;
import br.bioregistro.fase.domain.model.cronograma.ModuloDeExecucao.PublicacaoDoModulo;
import br.bioregistro.fase.domain.port.CronogramaRepository;
import br.bioregistro.fase.domain.port.EtapaRepository;
import br.bioregistro.fase.domain.port.FaseRepository;
import br.bioregistro.fase.domain.port.PublicacaoRepository;
import br.bioregistro.fase.domain.port.TipoRepository;
import br.bioregistro.fase.domain.port.VinculoCronogramaRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Lado da estrutura da ponte (spec 005, RN02/RN03): para cada módulo de execução ligado no
 * cronograma do edital, garante uma fase do edital com a etapa do tipo do módulo e as publicações
 * do módulo apontando para as linhas do cronograma. A fase nasce sem grupos: quem participa é
 * definido na fase.
 *
 * <p>Não duplica (uma fase por módulo no edital; fase já existente com a etapa do tipo é adotada),
 * não cria nada no rascunho (sem data do edital, a fase não tem data) e nunca exclui: desligar um
 * módulo deixa a fase como está (P-02).
 */
@ApplicationScoped
public class EstruturaDoCronograma {

    private final CronogramaRepository cronogramas;
    private final VinculoCronogramaRepository vinculos;
    private final FaseRepository fases;
    private final EtapaRepository etapas;
    private final TipoRepository tipos;
    private final PublicacaoRepository publicacoes;

    public EstruturaDoCronograma(CronogramaRepository cronogramas, VinculoCronogramaRepository vinculos,
                                 FaseRepository fases, EtapaRepository etapas, TipoRepository tipos,
                                 PublicacaoRepository publicacoes) {
        this.cronogramas = cronogramas;
        this.vinculos = vinculos;
        this.fases = fases;
        this.etapas = etapas;
        this.tipos = tipos;
        this.publicacoes = publicacoes;
    }

    public void sincronizar(Instituicao instituicao, EditalDoCronograma edital, CronogramaCalculado calculado) {
        if (!calculado.materializado()) {
            return;
        }
        Map<String, Integer> linhas = cronogramas.linhasDoEdital(instituicao, edital.id());
        for (ModuloDeExecucao modulo : ModuloDeExecucao.values()) {
            Optional<TipoEtapa> tipo = tipoEtapa(instituicao, modulo);
            if (tipo.isEmpty()) {
                continue;
            }
            Optional<Integer> vinculada = vinculos.faseDoModulo(instituicao, edital.id(), modulo);
            if (vinculada.isEmpty() && !modulo.ligado(calculado)) {
                continue;
            }
            Fase fase = vinculada.flatMap(id -> fases.buscar(instituicao, id))
                    .map(f -> comDatas(instituicao, f, modulo, calculado))
                    .orElseGet(() -> faseNova(instituicao, edital, modulo, tipo.get(), calculado));
            Etapa etapa = etapaDoTipo(instituicao, fase, tipo.get());
            for (PublicacaoDoModulo pedida : modulo.publicacoes()) {
                vincularPublicacao(instituicao, fase, etapa, pedida, linhas);
            }
        }
    }

    private Optional<TipoEtapa> tipoEtapa(Instituicao instituicao, ModuloDeExecucao modulo) {
        return tipos.tiposEtapa(instituicao).stream().filter(t -> modulo.tipoEtapa().equals(t.valor())).findFirst();
    }

    /** Adota a fase do edital que já tem uma etapa do tipo (sem vínculo); senão cria. */
    private Fase faseNova(Instituicao instituicao, EditalDoCronograma edital, ModuloDeExecucao modulo, TipoEtapa tipo,
                          CronogramaCalculado calculado) {
        List<Fase> candidatas = fases.listarPorEdital(instituicao, edital.editalId()).stream()
                .filter(f -> vinculos.moduloDaFase(instituicao, f.id()).isEmpty())
                .filter(f -> etapas.listarPorFase(instituicao, f.id()).stream()
                        .anyMatch(e -> Objects.equals(e.tipoId(), tipo.id())))
                .toList();
        Fase fase;
        if (candidatas.size() == 1) {
            fase = comDatas(instituicao, candidatas.get(0), modulo, calculado);
        } else {
            short ordem = (short) (fases.maiorOrdem(instituicao, edital.editalId()) + 1);
            fase = fases.salvar(instituicao, new Fase(null, edital.editalId(), modulo.nome(), null, ordem,
                    abertura(modulo, calculado), fechamento(modulo, calculado)));
        }
        vinculos.vincularFase(instituicao, fase.id(), edital.id(), modulo);
        return fase;
    }

    /** As datas da fase vinculada são as do cronograma (colunas NOT NULL no banco legado). */
    private Fase comDatas(Instituicao instituicao, Fase fase, ModuloDeExecucao modulo, CronogramaCalculado calculado) {
        OffsetDateTime abertura = abertura(modulo, calculado);
        OffsetDateTime fechamento = fechamento(modulo, calculado);
        if (abertura == null || fechamento == null
                || (abertura.equals(fase.dataAbertura()) && fechamento.equals(fase.dataFechamento()))) {
            return fase;
        }
        return fases.salvar(instituicao, new Fase(fase.id(), fase.editalId(), fase.nome(), fase.descricao(),
                fase.ordem(), abertura, fechamento));
    }

    private static OffsetDateTime abertura(ModuloDeExecucao modulo, CronogramaCalculado calculado) {
        return calculado.etapa(modulo.abertura()).map(EtapaCalculada::inicio)
                .map(PublicacoesDoCronograma::inicioDoDia).orElse(null);
    }

    private static OffsetDateTime fechamento(ModuloDeExecucao modulo, CronogramaCalculado calculado) {
        return calculado.etapa(modulo.fechamento()).map(EtapaCalculada::fim)
                .map(PublicacoesDoCronograma::fimDoDia).orElse(null);
    }

    private Etapa etapaDoTipo(Instituicao instituicao, Fase fase, TipoEtapa tipo) {
        return etapas.listarPorFase(instituicao, fase.id()).stream()
                .filter(e -> Objects.equals(e.tipoId(), tipo.id()))
                .findFirst()
                .orElseGet(() -> etapas.salvar(instituicao, new Etapa(null, fase.id(), tipo.descricao(), tipo.id(),
                        null, null, null, null, false, false, false, false, null, null, null, null, null, null)));
    }

    /** A publicação do tipo na etapa (criada se faltar) passa a apontar para as linhas do cronograma. */
    private void vincularPublicacao(Instituicao instituicao, Fase fase, Etapa etapa, PublicacaoDoModulo pedida,
                                    Map<String, Integer> linhas) {
        Integer linha = linhas.get(pedida.codigo());
        Optional<TipoPublicacao> tipo = tipos.tipoPublicacao(instituicao, pedida.tipo());
        if (linha == null || tipo.isEmpty()) {
            return;
        }
        Publicacao publicacao = publicacoes.listarPorEtapa(instituicao, etapa.id()).stream()
                .filter(p -> p.tipo().id().equals(tipo.get().id()))
                .findFirst()
                .orElseGet(() -> publicacoes.salvar(instituicao, Publicacao.espaco(fase.id(), etapa.id(), tipo.get())));
        Integer recurso = pedida.recurso() == null ? null : linhas.get(pedida.recurso());
        vinculos.vincularPublicacao(instituicao, publicacao.id(), linha, recurso);
    }
}

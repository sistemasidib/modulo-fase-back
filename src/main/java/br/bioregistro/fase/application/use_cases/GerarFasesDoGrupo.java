package br.bioregistro.fase.application.use_cases;

import br.bioregistro.fase.application.dtos.EtapaResponse;
import br.bioregistro.fase.application.dtos.FaseGeradaResponse;
import br.bioregistro.fase.application.dtos.FaseResponse;
import br.bioregistro.fase.application.dtos.GerarFasesRequest;
import br.bioregistro.fase.application.dtos.GerarFasesRequest.EtapaGerada;
import br.bioregistro.fase.application.dtos.GerarFasesRequest.FaseGerada;
import br.bioregistro.fase.domain.exception.NaoEncontradoException;
import br.bioregistro.fase.domain.exception.RegraDeNegocioException;
import br.bioregistro.fase.domain.model.Etapa;
import br.bioregistro.fase.domain.model.Fase;
import br.bioregistro.fase.domain.model.GrupoCargo;
import br.bioregistro.fase.domain.model.Instituicao;
import br.bioregistro.fase.domain.model.TipoEtapa;
import br.bioregistro.fase.domain.port.EtapaRepository;
import br.bioregistro.fase.domain.port.FaseRepository;
import br.bioregistro.fase.domain.port.GrupoCargoRepository;
import br.bioregistro.fase.domain.port.TipoRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Gera, para o grupo, as fases e as etapas escolhidas pelo usuário (tipos de etapa marcados).
 * A fase é do edital (spec 005): se o edital já tem uma fase com exatamente os tipos de etapa
 * pedidos (a criada pelo cronograma, ou a de outro grupo), o grupo passa a participar dela, sem
 * duplicar.
 * Senão, a fase é criada: cada etapa já nasce com os espaços de publicação escolhidos (ou os
 * padrão do tipo), e a fase com o resultado próprio quando ele não repete o de uma etapa. As fases
 * novas entram depois da última ordem do edital; a ordem é única no edital inteiro.
 */
@ApplicationScoped
public class GerarFasesDoGrupo {

    private final GrupoCargoRepository grupos;
    private final FaseRepository fases;
    private final EtapaRepository etapas;
    private final TipoRepository tipos;
    private final EspacosDePublicacao espacos;
    private final ResultadoFinalDaFase resultadoFinal;
    private final AplicarConfigPublicacaoNaEtapa aplicarConfig;

    public GerarFasesDoGrupo(GrupoCargoRepository grupos, FaseRepository fases, EtapaRepository etapas,
                             TipoRepository tipos, EspacosDePublicacao espacos,
                             ResultadoFinalDaFase resultadoFinal,
                             AplicarConfigPublicacaoNaEtapa aplicarConfig) {
        this.grupos = grupos;
        this.fases = fases;
        this.etapas = etapas;
        this.tipos = tipos;
        this.espacos = espacos;
        this.resultadoFinal = resultadoFinal;
        this.aplicarConfig = aplicarConfig;
    }

    @Transactional
    public List<FaseGeradaResponse> executar(Instituicao instituicao, int grupoId, GerarFasesRequest request) {
        GrupoCargo grupo = grupos.buscar(instituicao, grupoId)
                .orElseThrow(() -> new NaoEncontradoException("Grupo de cargos", grupoId));
        if (grupos.cargosDoGrupo(instituicao, grupoId).isEmpty()) {
            throw new RegraDeNegocioException("Inclua ao menos um cargo no grupo antes de gerar as fases.");
        }
        if (request.fases() == null || request.fases().isEmpty()) {
            throw new RegraDeNegocioException("Escolha ao menos uma fase para gerar.");
        }
        short ordem = fases.maiorOrdem(instituicao, grupo.editalId());
        List<FaseGeradaResponse> geradas = new ArrayList<>();
        for (FaseGerada pedido : request.fases()) {
            Optional<Fase> existente = faseComTipo(instituicao, grupo.editalId(), pedido);
            if (existente.isPresent()) {
                geradas.add(participar(instituicao, grupo, existente.get()));
            } else {
                ordem++;
                geradas.add(gerarFase(instituicao, grupo, ordem, pedido));
            }
        }
        return geradas;
    }

    private Optional<Fase> faseComTipo(Instituicao instituicao, int editalId, FaseGerada pedido) {
        Set<Integer> pedidos = tiposExecucao(instituicao, (pedido.etapas() == null ? List.<EtapaGerada>of() : pedido.etapas())
                .stream().map(EtapaGerada::tipoId).filter(Objects::nonNull).collect(Collectors.toSet()));
        return fases.listarPorEdital(instituicao, editalId).stream()
                .filter(f -> tiposExecucao(instituicao, etapas.listarPorFase(instituicao, f.id()).stream()
                        .map(Etapa::tipoId).collect(Collectors.toSet())).equals(pedidos))
                .findFirst();
    }

    /** Ignora Resultado Final (obrigatória) na comparação de fases equivalentes. */
    private Set<Integer> tiposExecucao(Instituicao instituicao, Set<Integer> tipoIds) {
        return tipoIds.stream()
                .filter(id -> tipos.tipoEtapa(instituicao, id).map(t -> !t.ehResultadoFinal()).orElse(true))
                .collect(Collectors.toSet());
    }

    private FaseGeradaResponse participar(Instituicao instituicao, GrupoCargo grupo, Fase fase) {
        if (!grupos.gruposDaFase(instituicao, fase.id()).contains(grupo.id())) {
            grupos.vincularFase(instituicao, fase.id(), grupo.id());
        }
        resultadoFinal.garantir(instituicao, fase.id());
        return resposta(instituicao, fase);
    }

    private FaseGeradaResponse gerarFase(Instituicao instituicao, GrupoCargo grupo, short ordem, FaseGerada pedido) {
        Fase fase = fases.salvar(instituicao, new Fase(null, grupo.editalId(), pedido.nome(), pedido.descricao(),
                ordem, pedido.dataAbertura(), pedido.dataFechamento()));
        grupos.vincularFase(instituicao, fase.id(), grupo.id());

        List<EtapaGerada> pedidas = pedido.etapas() == null ? List.of() : pedido.etapas();
        for (EtapaGerada pedida : pedidas) {
            if (pedida.tipoId() == null) {
                throw new RegraDeNegocioException("O tipo da etapa é obrigatório.");
            }
            TipoEtapa tipo = TipoEtapaExistente.exigirNovo(tipos, instituicao, pedida.tipoId());
            String nome = pedida.nome() == null || pedida.nome().isBlank() ? tipo.descricao() : pedida.nome();
            Etapa etapa = etapas.salvar(instituicao, CamposDaEtapa.aplicar(tipos, instituicao,
                    new Etapa(null, fase.id(), nome, tipo.id(), null,
                            pedido.dataAbertura(), pedido.dataFechamento(),
                            null, false, false, false, false, null, null, null, null, null, null, null, null, null),
                    true));
            espacos.criarParaEtapa(instituicao, fase.id(), etapa.id(), tipo.id(), pedida.publicacoes());
            espacos.ajustarEspacoDaModalidade(instituicao, fase.id(), etapa.id(), null, etapa.modalidade());
            aplicarConfig.executar(instituicao, etapa);
        }
        resultadoFinal.garantir(instituicao, fase.id());
        espacos.sincronizarResultadoDaFase(instituicao, fase.id());
        return resposta(instituicao, fase);
    }

    private FaseGeradaResponse resposta(Instituicao instituicao, Fase fase) {
        return new FaseGeradaResponse(FaseResponse.de(fase),
                resultadoFinal.comFinalPorUltimo(instituicao, etapas.listarPorFase(instituicao, fase.id())).stream()
                        .map(EtapaResponse::de)
                        .toList());
    }
}

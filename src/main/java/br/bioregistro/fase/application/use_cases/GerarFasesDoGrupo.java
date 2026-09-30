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

/**
 * Gera, para o grupo, as fases e as etapas escolhidas pelo usuário (tipos de etapa marcados).
 * Cada fase e cada etapa já nascem com seus espaços de publicação. As fases entram depois
 * da última ordem do edital; a ordem é única no edital inteiro, não por grupo.
 */
@ApplicationScoped
public class GerarFasesDoGrupo {

    private final GrupoCargoRepository grupos;
    private final FaseRepository fases;
    private final EtapaRepository etapas;
    private final TipoRepository tipos;
    private final EspacosDePublicacao espacos;

    public GerarFasesDoGrupo(GrupoCargoRepository grupos, FaseRepository fases, EtapaRepository etapas,
                             TipoRepository tipos, EspacosDePublicacao espacos) {
        this.grupos = grupos;
        this.fases = fases;
        this.etapas = etapas;
        this.tipos = tipos;
        this.espacos = espacos;
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
            ordem++;
            geradas.add(gerarFase(instituicao, grupo, ordem, pedido));
        }
        return geradas;
    }

    private FaseGeradaResponse gerarFase(Instituicao instituicao, GrupoCargo grupo, short ordem, FaseGerada pedido) {
        Fase fase = fases.salvar(instituicao, new Fase(null, grupo.editalId(), pedido.nome(), pedido.descricao(),
                ordem, pedido.dataAbertura(), pedido.dataFechamento()));
        grupos.vincularFase(instituicao, fase.id(), grupo.id());
        espacos.criarParaFase(instituicao, fase.id());

        List<EtapaGerada> pedidas = pedido.etapas() == null ? List.of() : pedido.etapas();
        List<EtapaResponse> criadas = new ArrayList<>();
        for (EtapaGerada pedida : pedidas) {
            if (pedida.tipoId() == null) {
                throw new RegraDeNegocioException("O tipo da etapa é obrigatório.");
            }
            TipoEtapa tipo = TipoEtapaExistente.exigir(tipos, instituicao, pedida.tipoId());
            String nome = pedida.nome() == null || pedida.nome().isBlank() ? tipo.descricao() : pedida.nome();
            Etapa etapa = etapas.salvar(instituicao, new Etapa(null, fase.id(), nome, tipo.id(), null, null, null,
                    null, false, false, false, false, null, null, null, null, null, null));
            espacos.criarParaEtapa(instituicao, fase.id(), etapa.id(), tipo.id());
            criadas.add(EtapaResponse.de(etapa));
        }
        return new FaseGeradaResponse(FaseResponse.de(fase), criadas);
    }
}

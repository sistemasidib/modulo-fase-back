package br.bioregistro.fase.application.use_cases;

import br.bioregistro.fase.application.dtos.CriterioDesempateDtos.CriterioDesempateResponse;
import br.bioregistro.fase.application.dtos.EstruturaPorGrupoDtos.FaseNaEstrutura;
import br.bioregistro.fase.application.dtos.EstruturaPorGrupoDtos.GrupoEstruturaResponse;
import br.bioregistro.fase.application.dtos.EtapaResponse;
import br.bioregistro.fase.application.dtos.FaseResponse;
import br.bioregistro.fase.domain.exception.NaoEncontradoException;
import br.bioregistro.fase.domain.model.Fase;
import br.bioregistro.fase.domain.model.GrupoCargo;
import br.bioregistro.fase.domain.model.Instituicao;
import br.bioregistro.fase.domain.port.CriterioDesempateRepository;
import br.bioregistro.fase.domain.port.EtapaRepository;
import br.bioregistro.fase.domain.port.FaseRepository;
import br.bioregistro.fase.domain.port.GrupoCargoRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Monta a estrutura do edital por grupo (spec 006, RN01–RN03): fases na ordem do edital,
 * desempate no fim do grupo. Cada etapa tem a própria lista; aqui só se agrega a leitura —
 * critérios das etapas das fases do grupo, sem repetir o mesmo id.
 */
@ApplicationScoped
public class EstruturaPorGrupo {

    private final FaseRepository fases;
    private final EtapaRepository etapas;
    private final GrupoCargoRepository grupos;
    private final CriterioDesempateRepository desempates;

    public EstruturaPorGrupo(FaseRepository fases, EtapaRepository etapas, GrupoCargoRepository grupos,
                             CriterioDesempateRepository desempates) {
        this.fases = fases;
        this.etapas = etapas;
        this.grupos = grupos;
        this.desempates = desempates;
    }

    public List<GrupoEstruturaResponse> executar(Instituicao instituicao, int editalId) {
        if (!fases.editalExiste(instituicao, editalId)) {
            throw new NaoEncontradoException("Edital", editalId);
        }
        List<GrupoEstruturaResponse> resultado = new ArrayList<>();
        for (GrupoCargo grupo : grupos.listarPorEdital(instituicao, editalId)) {
            List<Fase> fasesDoGrupo = fases.listarPorGrupo(instituicao, grupo.id());
            List<FaseNaEstrutura> fasesResp = new ArrayList<>();
            Map<Integer, CriterioDesempateResponse> desempateNoFim = new LinkedHashMap<>();
            for (Fase fase : fasesDoGrupo) {
                List<Integer> grupoIds = grupos.gruposDaFase(instituicao, fase.id());
                List<EtapaResponse> etapasResp = etapas.listarPorFase(instituicao, fase.id()).stream()
                        .map(EtapaResponse::de)
                        .toList();
                fasesResp.add(new FaseNaEstrutura(FaseResponse.de(fase, grupoIds), grupoIds, grupoIds.size(),
                        etapasResp));
                for (CriterioDesempateResponse c : desempates.daFase(instituicao, fase.id()).stream()
                        .map(CriterioDesempateResponse::de)
                        .toList()) {
                    desempateNoFim.putIfAbsent(c.id(), c);
                }
            }
            resultado.add(new GrupoEstruturaResponse(grupo.id(), grupo.nome(),
                    grupos.cargosDoGrupo(instituicao, grupo.id()), fasesResp,
                    List.copyOf(desempateNoFim.values())));
        }
        return resultado;
    }
}

package br.bioregistro.fase.application.use_cases;

import br.bioregistro.fase.application.dtos.CriterioDesempateDtos.CriterioDesempateRequest;
import br.bioregistro.fase.application.dtos.CriterioDesempateDtos.CriterioDesempateResponse;
import br.bioregistro.fase.domain.exception.NaoEncontradoException;
import br.bioregistro.fase.domain.exception.RegraDeNegocioException;
import br.bioregistro.fase.domain.model.CriterioDesempate;
import br.bioregistro.fase.domain.model.Instituicao;
import br.bioregistro.fase.domain.port.CriterioDesempateRepository;
import br.bioregistro.fase.domain.port.FaseRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/** Critérios de desempate de uma fase (de um grupo), sempre numerados de 1 a n na ordem de aplicação. */
@ApplicationScoped
public class CriteriosDeDesempate {

    private final CriterioDesempateRepository criterios;
    private final FaseRepository fases;

    public CriteriosDeDesempate(CriterioDesempateRepository criterios, FaseRepository fases) {
        this.criterios = criterios;
        this.fases = fases;
    }

    public List<CriterioDesempateResponse> listar(Instituicao instituicao, int faseId) {
        exigirFase(instituicao, faseId);
        return resposta(criterios.daFase(instituicao, faseId));
    }

    @Transactional
    public CriterioDesempateResponse criar(Instituicao instituicao, int faseId, CriterioDesempateRequest r) {
        exigirFase(instituicao, faseId);
        int ordem = criterios.daFase(instituicao, faseId).size() + 1;
        CriterioDesempate novo = new CriterioDesempate(null, faseId, ordem, r.criterio(), r.tipo(), r.regra(),
                r.referencia(), r.faseAplicacao(), r.fundamentacao(), r.observacao());
        return CriterioDesempateResponse.de(criterios.salvar(instituicao, novo));
    }

    @Transactional
    public CriterioDesempateResponse atualizar(Instituicao instituicao, int id, CriterioDesempateRequest r) {
        CriterioDesempate atual = buscar(instituicao, id);
        CriterioDesempate alterado = new CriterioDesempate(id, atual.faseId(), atual.ordem(), r.criterio(), r.tipo(),
                r.regra(), r.referencia(), r.faseAplicacao(), r.fundamentacao(), r.observacao());
        return CriterioDesempateResponse.de(criterios.salvar(instituicao, alterado));
    }

    /** Os seguintes sobem uma posição. */
    @Transactional
    public void excluir(Instituicao instituicao, int id) {
        CriterioDesempate atual = buscar(instituicao, id);
        criterios.excluir(instituicao, id);
        numerar(instituicao, criterios.daFase(instituicao, atual.faseId()));
    }

    /** {@code ids} = todos os critérios da fase, do 1º ao último. */
    @Transactional
    public List<CriterioDesempateResponse> reordenar(Instituicao instituicao, int faseId, List<Integer> ids) {
        exigirFase(instituicao, faseId);
        List<CriterioDesempate> atuais = criterios.daFase(instituicao, faseId);
        Map<Integer, CriterioDesempate> porId = atuais.stream()
                .collect(Collectors.toMap(CriterioDesempate::id, Function.identity()));
        List<Integer> ordem = ids == null ? List.of() : ids;
        Set<Integer> vistos = new HashSet<>(ordem);
        if (ordem.size() != atuais.size() || vistos.size() != ordem.size() || !porId.keySet().equals(vistos)) {
            throw new RegraDeNegocioException("Envie todos os critérios da fase, cada um uma vez, na nova ordem.");
        }
        return resposta(numerar(instituicao, ordem.stream().map(porId::get).toList()));
    }

    /** Troca os critérios de cada fase de destino por cópias dos critérios da fase de origem, na mesma ordem. */
    @Transactional
    public void copiar(Instituicao instituicao, int origemId, List<Integer> destinoIds) {
        exigirFase(instituicao, origemId);
        List<Integer> destinos = destinoIds == null ? List.of() : destinoIds.stream().distinct()
                .filter(id -> id != origemId).toList();
        if (destinos.isEmpty()) {
            throw new RegraDeNegocioException("Escolha ao menos uma fase de destino.");
        }
        destinos.forEach(id -> exigirFase(instituicao, id));
        List<CriterioDesempate> modelo = criterios.daFase(instituicao, origemId);
        for (int destino : destinos) {
            criterios.excluirDaFase(instituicao, destino);
            for (CriterioDesempate c : modelo) {
                criterios.salvar(instituicao, new CriterioDesempate(null, destino, c.ordem(), c.criterio(), c.tipo(),
                        c.regra(), c.referencia(), c.faseAplicacao(), c.fundamentacao(), c.observacao()));
            }
        }
    }

    private List<CriterioDesempate> numerar(Instituicao instituicao, List<CriterioDesempate> naOrdem) {
        return IntStream.range(0, naOrdem.size())
                .mapToObj(i -> {
                    CriterioDesempate c = naOrdem.get(i);
                    return c.ordem() == i + 1 ? c : criterios.salvar(instituicao, c.comOrdem(i + 1));
                })
                .toList();
    }

    private CriterioDesempate buscar(Instituicao instituicao, int id) {
        return criterios.buscar(instituicao, id).orElseThrow(() -> new NaoEncontradoException("Critério de desempate", id));
    }

    private void exigirFase(Instituicao instituicao, int faseId) {
        if (fases.buscar(instituicao, faseId).isEmpty()) {
            throw new NaoEncontradoException("Fase", faseId);
        }
    }

    private static List<CriterioDesempateResponse> resposta(List<CriterioDesempate> lista) {
        return lista.stream().map(CriterioDesempateResponse::de).toList();
    }
}

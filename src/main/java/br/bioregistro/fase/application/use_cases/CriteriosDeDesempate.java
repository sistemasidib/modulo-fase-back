package br.bioregistro.fase.application.use_cases;

import br.bioregistro.fase.application.dtos.CriterioDesempateDtos.CriterioDesempateRequest;
import br.bioregistro.fase.application.dtos.CriterioDesempateDtos.CriterioDesempateResponse;
import br.bioregistro.fase.domain.exception.NaoEncontradoException;
import br.bioregistro.fase.domain.exception.RegraDeNegocioException;
import br.bioregistro.fase.domain.model.CriterioDesempate;
import br.bioregistro.fase.domain.model.Instituicao;
import br.bioregistro.fase.domain.port.CriterioDesempateRepository;
import br.bioregistro.fase.domain.port.EtapaRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/** Critérios de desempate de uma etapa, sempre numerados de 1 a n na ordem de aplicação. */
@ApplicationScoped
public class CriteriosDeDesempate {

    private final CriterioDesempateRepository criterios;
    private final EtapaRepository etapas;

    public CriteriosDeDesempate(CriterioDesempateRepository criterios, EtapaRepository etapas) {
        this.criterios = criterios;
        this.etapas = etapas;
    }

    public List<CriterioDesempateResponse> listar(Instituicao instituicao, int etapaId) {
        exigirEtapa(instituicao, etapaId);
        return resposta(criterios.daEtapa(instituicao, etapaId));
    }

    @Transactional
    public CriterioDesempateResponse criar(Instituicao instituicao, int etapaId, CriterioDesempateRequest r) {
        exigirEtapa(instituicao, etapaId);
        int ordem = criterios.daEtapa(instituicao, etapaId).size() + 1;
        CriterioDesempate novo = de(null, etapaId, ordem, r);
        return CriterioDesempateResponse.de(criterios.salvar(instituicao, novo));
    }

    @Transactional
    public CriterioDesempateResponse atualizar(Instituicao instituicao, int id, CriterioDesempateRequest r) {
        CriterioDesempate atual = buscar(instituicao, id);
        CriterioDesempate alterado = de(id, atual.etapaId(), atual.ordem(), r);
        return CriterioDesempateResponse.de(criterios.salvar(instituicao, alterado));
    }

    /** Os seguintes sobem uma posição. */
    @Transactional
    public void excluir(Instituicao instituicao, int id) {
        CriterioDesempate atual = buscar(instituicao, id);
        criterios.excluir(instituicao, id);
        numerar(instituicao, criterios.daEtapa(instituicao, atual.etapaId()));
    }

    /** {@code ids} = todos os critérios da etapa, do 1º ao último. */
    @Transactional
    public List<CriterioDesempateResponse> reordenar(Instituicao instituicao, int etapaId, List<Integer> ids) {
        exigirEtapa(instituicao, etapaId);
        List<CriterioDesempate> atuais = criterios.daEtapa(instituicao, etapaId);
        Map<Integer, CriterioDesempate> porId = atuais.stream()
                .collect(Collectors.toMap(CriterioDesempate::id, Function.identity()));
        List<Integer> ordem = ids == null ? List.of() : ids;
        Set<Integer> vistos = new HashSet<>(ordem);
        if (ordem.size() != atuais.size() || vistos.size() != ordem.size() || !porId.keySet().equals(vistos)) {
            throw new RegraDeNegocioException("Envie todos os critérios da etapa, cada um uma vez, na nova ordem.");
        }
        return resposta(numerar(instituicao, ordem.stream().map(porId::get).toList()));
    }

    /** Troca os critérios de cada etapa de destino por cópias dos da origem, na mesma ordem. */
    @Transactional
    public void copiar(Instituicao instituicao, int origemId, List<Integer> destinoIds) {
        exigirEtapa(instituicao, origemId);
        List<Integer> destinos = destinoIds == null ? List.of() : destinoIds.stream().distinct()
                .filter(id -> id != origemId).toList();
        if (destinos.isEmpty()) {
            throw new RegraDeNegocioException("Escolha ao menos uma etapa de destino.");
        }
        destinos.forEach(id -> exigirEtapa(instituicao, id));
        List<CriterioDesempate> modelo = criterios.daEtapa(instituicao, origemId);
        for (int destino : destinos) {
            criterios.excluirDaEtapa(instituicao, destino);
            for (CriterioDesempate c : modelo) {
                criterios.salvar(instituicao, new CriterioDesempate(
                        null, destino, c.ordem(), c.label(), c.tipo(), c.direcao(),
                        c.condicao(), c.dataReferencia(), c.fonte(), c.baseLegal()));
            }
        }
    }

    private static CriterioDesempate de(Integer id, int etapaId, int ordem, CriterioDesempateRequest r) {
        return new CriterioDesempate(id, etapaId, ordem, r.label(), r.tipo(), r.direcao(),
                r.condicao(), r.dataReferencia(), r.fonte(), r.baseLegal());
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

    private void exigirEtapa(Instituicao instituicao, int etapaId) {
        if (etapas.buscar(instituicao, etapaId).isEmpty()) {
            throw new NaoEncontradoException("Etapa", etapaId);
        }
    }

    private static List<CriterioDesempateResponse> resposta(List<CriterioDesempate> lista) {
        return lista.stream().map(CriterioDesempateResponse::de).toList();
    }
}

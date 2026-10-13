package br.bioregistro.fase.application.use_cases;

import br.bioregistro.fase.application.dtos.CriterioCorrecaoDtos.CriterioCorrecaoRequest;
import br.bioregistro.fase.application.dtos.CriterioCorrecaoDtos.CriterioCorrecaoResponse;
import br.bioregistro.fase.domain.exception.NaoEncontradoException;
import br.bioregistro.fase.domain.exception.RegraDeNegocioException;
import br.bioregistro.fase.domain.model.CriterioCorrecao;
import br.bioregistro.fase.domain.model.Etapa;
import br.bioregistro.fase.domain.model.Instituicao;
import br.bioregistro.fase.domain.port.CriterioCorrecaoRepository;
import br.bioregistro.fase.domain.port.EtapaRepository;
import br.bioregistro.fase.domain.port.TipoRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Critérios de correção de uma prova discursiva, numerados de 1 a n. A prova objetiva não tem: a nota dela vem
 * das disciplinas e do valor de cada questão.
 */
@ApplicationScoped
public class CriteriosDeCorrecao {

    static final String PROVA_DISCURSIVA = "prova_discursiva";

    private final CriterioCorrecaoRepository criterios;
    private final EtapaRepository etapas;
    private final TipoRepository tipos;

    public CriteriosDeCorrecao(CriterioCorrecaoRepository criterios, EtapaRepository etapas, TipoRepository tipos) {
        this.criterios = criterios;
        this.etapas = etapas;
        this.tipos = tipos;
    }

    public List<CriterioCorrecaoResponse> listar(Instituicao instituicao, int etapaId) {
        exigirDiscursiva(instituicao, etapaId);
        return resposta(criterios.daEtapa(instituicao, etapaId));
    }

    @Transactional
    public CriterioCorrecaoResponse criar(Instituicao instituicao, int etapaId, CriterioCorrecaoRequest r) {
        exigirDiscursiva(instituicao, etapaId);
        int ordem = criterios.daEtapa(instituicao, etapaId).size() + 1;
        CriterioCorrecao novo = new CriterioCorrecao(null, etapaId, ordem, r.modulo(), r.criterio(),
                r.descontoPorErro(), r.pontuacaoMaxima());
        return CriterioCorrecaoResponse.de(criterios.salvar(instituicao, novo));
    }

    @Transactional
    public CriterioCorrecaoResponse atualizar(Instituicao instituicao, int id, CriterioCorrecaoRequest r) {
        CriterioCorrecao atual = buscar(instituicao, id);
        CriterioCorrecao alterado = new CriterioCorrecao(id, atual.etapaId(), atual.ordem(), r.modulo(),
                r.criterio(), r.descontoPorErro(), r.pontuacaoMaxima());
        return CriterioCorrecaoResponse.de(criterios.salvar(instituicao, alterado));
    }

    /** Os seguintes sobem uma posição. */
    @Transactional
    public void excluir(Instituicao instituicao, int id) {
        CriterioCorrecao atual = buscar(instituicao, id);
        criterios.excluir(instituicao, id);
        numerar(instituicao, criterios.daEtapa(instituicao, atual.etapaId()));
    }

    /** {@code ids} = todos os critérios da prova, do primeiro ao último. */
    @Transactional
    public List<CriterioCorrecaoResponse> reordenar(Instituicao instituicao, int etapaId, List<Integer> ids) {
        exigirDiscursiva(instituicao, etapaId);
        List<CriterioCorrecao> atuais = criterios.daEtapa(instituicao, etapaId);
        Map<Integer, CriterioCorrecao> porId = atuais.stream()
                .collect(Collectors.toMap(CriterioCorrecao::id, Function.identity()));
        List<Integer> ordem = ids == null ? List.of() : ids;
        Set<Integer> vistos = new HashSet<>(ordem);
        if (ordem.size() != atuais.size() || vistos.size() != ordem.size() || !porId.keySet().equals(vistos)) {
            throw new RegraDeNegocioException("Envie todos os critérios da prova, cada um uma vez, na nova ordem.");
        }
        return resposta(numerar(instituicao, ordem.stream().map(porId::get).toList()));
    }

    /** Troca os critérios de cada prova de destino por cópias dos critérios da prova de origem, na mesma ordem. */
    @Transactional
    public void copiar(Instituicao instituicao, int origemId, List<Integer> destinoIds) {
        exigirDiscursiva(instituicao, origemId);
        List<Integer> destinos = destinoIds == null ? List.of() : destinoIds.stream().distinct()
                .filter(id -> id != origemId).toList();
        if (destinos.isEmpty()) {
            throw new RegraDeNegocioException("Escolha ao menos uma prova discursiva de destino.");
        }
        destinos.forEach(id -> exigirDiscursiva(instituicao, id));
        List<CriterioCorrecao> modelo = criterios.daEtapa(instituicao, origemId);
        for (int destino : destinos) {
            criterios.excluirDaEtapa(instituicao, destino);
            for (CriterioCorrecao c : modelo) {
                criterios.salvar(instituicao, new CriterioCorrecao(null, destino, c.ordem(), c.modulo(),
                        c.criterio(), c.descontoPorErro(), c.pontuacaoMaxima()));
            }
        }
    }

    private List<CriterioCorrecao> numerar(Instituicao instituicao, List<CriterioCorrecao> naOrdem) {
        return IntStream.range(0, naOrdem.size())
                .mapToObj(i -> {
                    CriterioCorrecao c = naOrdem.get(i);
                    return c.ordem() == i + 1 ? c : criterios.salvar(instituicao, c.comOrdem(i + 1));
                })
                .toList();
    }

    private CriterioCorrecao buscar(Instituicao instituicao, int id) {
        return criterios.buscar(instituicao, id)
                .orElseThrow(() -> new NaoEncontradoException("Critério de correção", id));
    }

    private void exigirDiscursiva(Instituicao instituicao, int etapaId) {
        Etapa etapa = etapas.buscar(instituicao, etapaId)
                .orElseThrow(() -> new NaoEncontradoException("Etapa", etapaId));
        boolean discursiva = etapa.tipoId() != null && tipos.tipoEtapa(instituicao, etapa.tipoId())
                .map(t -> PROVA_DISCURSIVA.equals(t.valor()))
                .orElse(false);
        if (!discursiva) {
            throw new RegraDeNegocioException(
                    "Critérios de correção são da prova discursiva; na prova objetiva a nota vem das disciplinas.");
        }
    }

    private static List<CriterioCorrecaoResponse> resposta(List<CriterioCorrecao> lista) {
        return lista.stream().map(CriterioCorrecaoResponse::de).toList();
    }
}

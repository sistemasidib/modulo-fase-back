package br.bioregistro.fase.application.use_cases;

import br.bioregistro.fase.domain.exception.ConflitoException;
import br.bioregistro.fase.domain.exception.NaoEncontradoException;
import br.bioregistro.fase.domain.exception.RegraDeNegocioException;
import br.bioregistro.fase.domain.model.Etapa;
import br.bioregistro.fase.domain.model.Fase;
import br.bioregistro.fase.domain.model.Instituicao;
import br.bioregistro.fase.domain.model.TipoPublicacao;
import br.bioregistro.fase.domain.port.EtapaRepository;
import br.bioregistro.fase.domain.port.FaseRepository;
import br.bioregistro.fase.domain.port.PublicacaoRepository;
import br.bioregistro.fase.domain.port.VinculoCronogramaRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * O que é publicado em uma fase do concurso (local de prova, gabarito, resultado...): os tipos de
 * publicação das etapas daquele tipo nas fases do edital. Mudar vale para todas. Nas fases que
 * seguem o cronograma, quem define as publicações é o cronograma (spec 005, RN05).
 */
@ApplicationScoped
public class PublicacoesDaFaseDoConcurso {

    private final VinculoCronogramaRepository vinculos;
    private final FaseRepository fases;
    private final EtapaRepository etapas;
    private final PublicacaoRepository publicacoes;
    private final EspacosDePublicacao espacos;

    public PublicacoesDaFaseDoConcurso(VinculoCronogramaRepository vinculos, FaseRepository fases,
                                       EtapaRepository etapas, PublicacaoRepository publicacoes,
                                       EspacosDePublicacao espacos) {
        this.vinculos = vinculos;
        this.fases = fases;
        this.etapas = etapas;
        this.publicacoes = publicacoes;
        this.espacos = espacos;
    }

    /** Ids dos tipos de publicação presentes em alguma etapa da fase do concurso. */
    public List<Integer> listar(Instituicao instituicao, int editalId, int tipoEtapaId) {
        return etapasDaFase(instituicao, editalId, tipoEtapaId).stream()
                .flatMap(e -> publicacoes.listarPorEtapa(instituicao, e.id()).stream())
                .map(p -> p.tipo().id())
                .distinct()
                .sorted()
                .toList();
    }

    @Transactional
    public List<Integer> definir(Instituicao instituicao, int editalId, int tipoEtapaId, List<Integer> tipoPublicacaoIds) {
        if (tipoPublicacaoIds == null) {
            throw new RegraDeNegocioException("Informe os tipos de publicação (lista vazia para nenhum).");
        }
        List<TipoPublicacao> escolhidos = espacos.escolhidosComResultado(instituicao, tipoPublicacaoIds);
        List<Etapa> daFase = etapasDaFase(instituicao, editalId, tipoEtapaId);
        for (Etapa etapa : daFase) {
            vinculos.moduloDaFase(instituicao, etapa.faseId()).ifPresent(m -> {
                throw new ConflitoException("A fase " + etapa.faseId() + " segue o cronograma do edital: "
                        + "as publicações dela mudam lá.");
            });
        }
        Set<Integer> fasesAlteradas = new LinkedHashSet<>();
        for (Etapa etapa : daFase) {
            espacos.definirDaEtapa(instituicao, etapa.faseId(), etapa.id(), escolhidos);
            fasesAlteradas.add(etapa.faseId());
        }
        fasesAlteradas.forEach(faseId -> espacos.sincronizarResultadoDaFase(instituicao, faseId));
        return listar(instituicao, editalId, tipoEtapaId);
    }

    private List<Etapa> etapasDaFase(Instituicao instituicao, int editalId, int tipoEtapaId) {
        if (!fases.editalExiste(instituicao, editalId)) {
            throw new NaoEncontradoException("Edital", editalId);
        }
        List<Etapa> encontradas = new ArrayList<>();
        for (Fase fase : fases.listarPorEdital(instituicao, editalId)) {
            for (Etapa etapa : etapas.listarPorFase(instituicao, fase.id())) {
                if (etapa.tipoId() != null && etapa.tipoId() == tipoEtapaId) {
                    encontradas.add(etapa);
                }
            }
        }
        if (encontradas.isEmpty()) {
            throw new NaoEncontradoException("Fase do concurso do tipo de etapa", tipoEtapaId);
        }
        return encontradas;
    }
}

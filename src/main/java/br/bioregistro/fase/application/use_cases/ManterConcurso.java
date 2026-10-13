package br.bioregistro.fase.application.use_cases;

import br.bioregistro.fase.application.dtos.ConcursoDtos.ConcursoRequest;
import br.bioregistro.fase.application.dtos.ConcursoDtos.ConcursoResponse;
import br.bioregistro.fase.domain.exception.ConflitoException;
import br.bioregistro.fase.domain.exception.NaoEncontradoException;
import br.bioregistro.fase.domain.model.Concurso;
import br.bioregistro.fase.domain.model.Instituicao;
import br.bioregistro.fase.domain.port.ConcursoRepository;
import br.bioregistro.fase.domain.port.CronogramaRepository;
import br.bioregistro.fase.domain.port.FaseRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

/**
 * Escrita do concurso: criar, renomear, excluir e agrupar editais (RB01, RB02). Um edital fica
 * em no máximo um concurso; o banco de cada instituição garante que só se agrupa edital dela.
 */
@ApplicationScoped
public class ManterConcurso {

    private final ConcursoRepository concursos;
    private final CronogramaRepository cronogramas;
    private final FaseRepository fases;
    private final ConsultarConcursos consulta;

    public ManterConcurso(ConcursoRepository concursos, CronogramaRepository cronogramas, FaseRepository fases,
                          ConsultarConcursos consulta) {
        this.concursos = concursos;
        this.cronogramas = cronogramas;
        this.fases = fases;
        this.consulta = consulta;
    }

    @Transactional
    public ConcursoResponse criar(Instituicao instituicao, ConcursoRequest request) {
        Concurso concurso = new Concurso(null, request.nome());
        exigirNomeLivre(instituicao, concurso.nome(), null);
        return consulta.resposta(instituicao, concursos.salvar(instituicao, concurso));
    }

    @Transactional
    public ConcursoResponse renomear(Instituicao instituicao, int id, ConcursoRequest request) {
        ConsultarConcursos.exigir(concursos, instituicao, id);
        Concurso concurso = new Concurso(id, request.nome());
        exigirNomeLivre(instituicao, concurso.nome(), id);
        return consulta.resposta(instituicao, concursos.salvar(instituicao, concurso));
    }

    /**
     * O concurso só agrupa editais (spec 005, RN13b): os editais voltam a ficar soltos e continuam
     * seguindo o cronograma que seguiam, que fica sem concurso.
     */
    @Transactional
    public void excluir(Instituicao instituicao, int id) {
        ConsultarConcursos.exigir(concursos, instituicao, id);
        cronogramas.soltarDoConcurso(instituicao, id);
        concursos.excluir(instituicao, id);
    }

    @Transactional
    public ConcursoResponse incluirEdital(Instituicao instituicao, int id, int editalId) {
        Concurso concurso = ConsultarConcursos.exigir(concursos, instituicao, id);
        if (!fases.editalExiste(instituicao, editalId)) {
            throw new NaoEncontradoException("Edital", editalId);
        }
        var atual = concursos.concursoDoEdital(instituicao, editalId);
        if (atual.isPresent() && !atual.get().id().equals(id)) {
            throw new ConflitoException("O edital " + editalId + " já está no concurso " + atual.get().nome()
                    + ". Retire-o de lá antes.");
        }
        if (atual.isEmpty()) {
            concursos.incluirEdital(instituicao, id, editalId);
        }
        return consulta.resposta(instituicao, concurso);
    }

    /** Operação normal (spec 005, RN17): o edital continua seguindo o cronograma que seguia. */
    @Transactional
    public ConcursoResponse retirarEdital(Instituicao instituicao, int id, int editalId) {
        Concurso concurso = ConsultarConcursos.exigir(concursos, instituicao, id);
        boolean noConcurso = concursos.concursoDoEdital(instituicao, editalId)
                .map(c -> c.id().equals(id)).orElse(false);
        if (!noConcurso) {
            throw new NaoEncontradoException("Edital no concurso " + concurso.nome() + ":", editalId);
        }
        concursos.retirarEdital(instituicao, id, editalId);
        return consulta.resposta(instituicao, concurso);
    }

    private void exigirNomeLivre(Instituicao instituicao, String nome, Integer ignorar) {
        if (concursos.nomeEmUso(instituicao, nome, ignorar)) {
            throw new ConflitoException("Já existe um concurso chamado " + nome + ".");
        }
    }
}

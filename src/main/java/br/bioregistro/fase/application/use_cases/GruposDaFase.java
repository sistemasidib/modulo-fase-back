package br.bioregistro.fase.application.use_cases;

import br.bioregistro.fase.domain.exception.NaoEncontradoException;
import br.bioregistro.fase.domain.exception.RegraDeNegocioException;
import br.bioregistro.fase.domain.model.Fase;
import br.bioregistro.fase.domain.model.GrupoCargo;
import br.bioregistro.fase.domain.model.Instituicao;
import br.bioregistro.fase.domain.port.FaseRepository;
import br.bioregistro.fase.domain.port.GrupoCargoRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.List;

/**
 * Quem participa da fase (spec 005): a fase é do edital e é ela que define os grupos de cargos que
 * fazem aquela etapa. Um grupo participa de quantas fases do edital precisar.
 */
@ApplicationScoped
public class GruposDaFase {

    private final FaseRepository fases;
    private final GrupoCargoRepository grupos;

    public GruposDaFase(FaseRepository fases, GrupoCargoRepository grupos) {
        this.fases = fases;
        this.grupos = grupos;
    }

    public List<Integer> listar(Instituicao instituicao, int faseId) {
        exigirFase(instituicao, faseId);
        return grupos.gruposDaFase(instituicao, faseId);
    }

    @Transactional
    public List<Integer> incluir(Instituicao instituicao, int faseId, int grupoId) {
        Fase fase = exigirFase(instituicao, faseId);
        GrupoCargo grupo = exigirGrupo(instituicao, grupoId);
        if (!grupo.editalId().equals(fase.editalId())) {
            throw new RegraDeNegocioException("O grupo " + grupoId + " é de outro edital.");
        }
        if (!grupos.gruposDaFase(instituicao, faseId).contains(grupoId)) {
            grupos.vincularFase(instituicao, faseId, grupoId);
        }
        return grupos.gruposDaFase(instituicao, faseId);
    }

    @Transactional
    public List<Integer> retirar(Instituicao instituicao, int faseId, int grupoId) {
        exigirFase(instituicao, faseId);
        if (!grupos.gruposDaFase(instituicao, faseId).contains(grupoId)) {
            throw new NaoEncontradoException("Grupo na fase " + faseId + ":", grupoId);
        }
        grupos.desvincularFase(instituicao, faseId, grupoId);
        return grupos.gruposDaFase(instituicao, faseId);
    }

    private Fase exigirFase(Instituicao instituicao, int faseId) {
        return fases.buscar(instituicao, faseId).orElseThrow(() -> new NaoEncontradoException("Fase", faseId));
    }

    private GrupoCargo exigirGrupo(Instituicao instituicao, int grupoId) {
        return grupos.buscar(instituicao, grupoId)
                .orElseThrow(() -> new NaoEncontradoException("Grupo de cargos", grupoId));
    }
}

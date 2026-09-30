package br.bioregistro.fase.application.use_cases;

import br.bioregistro.fase.application.dtos.GrupoCargoRequest;
import br.bioregistro.fase.application.dtos.GrupoCargoResponse;
import br.bioregistro.fase.domain.exception.ConflitoException;
import br.bioregistro.fase.domain.exception.NaoEncontradoException;
import br.bioregistro.fase.domain.model.GrupoCargo;
import br.bioregistro.fase.domain.model.Instituicao;
import br.bioregistro.fase.domain.port.FaseRepository;
import br.bioregistro.fase.domain.port.GrupoCargoRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.List;

@ApplicationScoped
public class CriarGrupoCargo {

    private final FaseRepository fases;
    private final GrupoCargoRepository grupos;

    public CriarGrupoCargo(FaseRepository fases, GrupoCargoRepository grupos) {
        this.fases = fases;
        this.grupos = grupos;
    }

    @Transactional
    public GrupoCargoResponse executar(Instituicao instituicao, int editalId, GrupoCargoRequest request) {
        if (!fases.editalExiste(instituicao, editalId)) {
            throw new NaoEncontradoException("Edital", editalId);
        }
        GrupoCargo grupo = new GrupoCargo(null, editalId, request.nome());
        if (grupos.nomeEmUso(instituicao, editalId, grupo.nome(), null)) {
            throw new ConflitoException("Já existe um grupo chamado " + grupo.nome() + " neste edital.");
        }
        return GrupoCargoResponse.de(grupos.salvar(instituicao, grupo), List.of());
    }
}

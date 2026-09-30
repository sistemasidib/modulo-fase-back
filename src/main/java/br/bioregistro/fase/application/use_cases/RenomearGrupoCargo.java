package br.bioregistro.fase.application.use_cases;

import br.bioregistro.fase.application.dtos.GrupoCargoRequest;
import br.bioregistro.fase.application.dtos.GrupoCargoResponse;
import br.bioregistro.fase.domain.exception.ConflitoException;
import br.bioregistro.fase.domain.exception.NaoEncontradoException;
import br.bioregistro.fase.domain.model.GrupoCargo;
import br.bioregistro.fase.domain.model.Instituicao;
import br.bioregistro.fase.domain.port.GrupoCargoRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class RenomearGrupoCargo {

    private final GrupoCargoRepository grupos;

    public RenomearGrupoCargo(GrupoCargoRepository grupos) {
        this.grupos = grupos;
    }

    @Transactional
    public GrupoCargoResponse executar(Instituicao instituicao, int id, GrupoCargoRequest request) {
        GrupoCargo atual = grupos.buscar(instituicao, id)
                .orElseThrow(() -> new NaoEncontradoException("Grupo de cargos", id));
        GrupoCargo grupo = new GrupoCargo(id, atual.editalId(), request.nome());
        if (grupos.nomeEmUso(instituicao, grupo.editalId(), grupo.nome(), id)) {
            throw new ConflitoException("Já existe um grupo chamado " + grupo.nome() + " neste edital.");
        }
        return GrupoCargoResponse.de(grupos.salvar(instituicao, grupo), grupos.cargosDoGrupo(instituicao, id));
    }
}

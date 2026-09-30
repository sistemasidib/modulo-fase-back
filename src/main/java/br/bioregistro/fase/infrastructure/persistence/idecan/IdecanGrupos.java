package br.bioregistro.fase.infrastructure.persistence.idecan;

import br.bioregistro.fase.infrastructure.persistence.RestricaoDoBanco;
import br.com.bio.registro.core.runtime.entities.idecan.dbo.Cargo;
import br.com.bio.registro.core.runtime.entities.idecan.dbo.Edital;
import br.com.bio.registro.core.runtime.entities.idecan.dbo.Fase;
import br.com.bio.registro.core.runtime.entities.idecan.dbo.FaseGrupoCargo;
import br.com.bio.registro.core.runtime.entities.idecan.dbo.GrupoCargo;
import br.com.bio.registro.core.runtime.entities.idecan.dbo.GrupoCargoCargo;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/** Grupos de cargos no banco IDECAN ({@code grupo_cargo}, {@code grupo_cargo_cargo}, {@code fase_grupo_cargo}). */
@ApplicationScoped
public class IdecanGrupos {

    public Optional<br.bioregistro.fase.domain.model.GrupoCargo> buscar(int id) {
        return GrupoCargo.<GrupoCargo>findByIdOptional(id).map(IdecanGrupos::paraDominio);
    }

    public List<br.bioregistro.fase.domain.model.GrupoCargo> listarPorEdital(int editalId) {
        return GrupoCargo.<GrupoCargo>list("edital.ediId = ?1 order by nome", editalId).stream()
                .map(IdecanGrupos::paraDominio)
                .toList();
    }

    public boolean nomeEmUso(int editalId, String nome, Integer ignorarGrupoId) {
        if (ignorarGrupoId == null) {
            return GrupoCargo.count("edital.ediId = ?1 and nome = ?2", editalId, nome) > 0;
        }
        return GrupoCargo.count("edital.ediId = ?1 and nome = ?2 and id <> ?3", editalId, nome, ignorarGrupoId) > 0;
    }

    public br.bioregistro.fase.domain.model.GrupoCargo salvar(br.bioregistro.fase.domain.model.GrupoCargo grupo) {
        OffsetDateTime agora = OffsetDateTime.now();
        GrupoCargo entidade;
        if (grupo.id() == null) {
            entidade = new GrupoCargo();
            entidade.edital = Edital.findById(grupo.editalId());
            entidade.createdAt = agora;
        } else {
            entidade = GrupoCargo.findById(grupo.id());
        }
        entidade.nome = grupo.nome();
        entidade.updatedAt = agora;
        entidade.persist();
        RestricaoDoBanco.flush(GrupoCargo::flush, "Já existe um grupo chamado " + grupo.nome() + " neste edital.");
        return paraDominio(entidade);
    }

    public void excluir(int id) {
        GrupoCargo.deleteById(id);
        RestricaoDoBanco.flush(GrupoCargo::flush, "O grupo " + id + " ainda está em uso e não pode ser excluído.");
    }

    public List<br.bioregistro.fase.domain.model.Cargo> cargosDoEdital(int editalId) {
        return Cargo.<Cargo>list("ediId = ?1 order by carDescricao", editalId).stream()
                .map(c -> new br.bioregistro.fase.domain.model.Cargo(c.carId, c.carDescricao))
                .toList();
    }

    public List<Integer> cargosDoGrupo(int grupoId) {
        return GrupoCargoCargo.<GrupoCargoCargo>list("grupoCargo.id = ?1 order by cargoId", grupoId).stream()
                .map(v -> v.cargoId)
                .toList();
    }

    public Optional<Integer> grupoDoCargo(int cargoId) {
        return GrupoCargoCargo.<GrupoCargoCargo>find("cargoId", cargoId).firstResultOptional()
                .map(v -> v.grupoCargo.id);
    }

    public void adicionarCargo(int grupoId, int cargoId) {
        GrupoCargoCargo vinculo = new GrupoCargoCargo();
        vinculo.grupoCargo = GrupoCargo.findById(grupoId);
        vinculo.cargoId = cargoId;
        vinculo.persist();
        RestricaoDoBanco.flush(GrupoCargoCargo::flush, "O cargo " + cargoId + " já está em outro grupo.");
    }

    public void removerCargo(int grupoId, int cargoId) {
        GrupoCargoCargo.delete("grupoCargo.id = ?1 and cargoId = ?2", grupoId, cargoId);
    }

    public void vincularFase(int faseId, int grupoId) {
        FaseGrupoCargo vinculo = new FaseGrupoCargo();
        vinculo.fase = Fase.findById(faseId);
        vinculo.grupoCargo = GrupoCargo.findById(grupoId);
        vinculo.persist();
    }

    public void desvincularFase(int faseId) {
        FaseGrupoCargo.delete("fase.id", faseId);
    }

    public boolean possuiFases(int grupoId) {
        return FaseGrupoCargo.count("grupoCargo.id", grupoId) > 0;
    }

    private static br.bioregistro.fase.domain.model.GrupoCargo paraDominio(GrupoCargo e) {
        return new br.bioregistro.fase.domain.model.GrupoCargo(e.id, e.edital.ediId, e.nome);
    }
}

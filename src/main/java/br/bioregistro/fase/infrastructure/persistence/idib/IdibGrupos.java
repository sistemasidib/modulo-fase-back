package br.bioregistro.fase.infrastructure.persistence.idib;

import br.bioregistro.fase.infrastructure.persistence.RestricaoDoBanco;
import br.com.bio.registro.core.runtime.entities.idib.dbo.Cargo;
import br.com.bio.registro.core.runtime.entities.idib.dbo.CargoGrupo;
import br.com.bio.registro.core.runtime.entities.idib.dbo.Edital;
import br.com.bio.registro.core.runtime.entities.idib.dbo.Fase;
import br.com.bio.registro.core.runtime.entities.idib.dbo.FaseGrupoCargos;
import br.com.bio.registro.core.runtime.entities.idib.dbo.GrupoCargos;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/** Grupos de cargos no banco IDIB ({@code grupo_cargos}, {@code cargo_grupo}, {@code fase_grupo_cargos}). */
@ApplicationScoped
public class IdibGrupos {

    public Optional<br.bioregistro.fase.domain.model.GrupoCargo> buscar(int id) {
        return GrupoCargos.<GrupoCargos>findByIdOptional(id).map(IdibGrupos::paraDominio);
    }

    public List<br.bioregistro.fase.domain.model.GrupoCargo> listarPorEdital(int editalId) {
        return GrupoCargos.<GrupoCargos>list("edital.ediId = ?1 order by nome", editalId).stream()
                .map(IdibGrupos::paraDominio)
                .toList();
    }

    public boolean nomeEmUso(int editalId, String nome, Integer ignorarGrupoId) {
        if (ignorarGrupoId == null) {
            return GrupoCargos.count("edital.ediId = ?1 and nome = ?2", editalId, nome) > 0;
        }
        return GrupoCargos.count("edital.ediId = ?1 and nome = ?2 and id <> ?3", editalId, nome, ignorarGrupoId) > 0;
    }

    public br.bioregistro.fase.domain.model.GrupoCargo salvar(br.bioregistro.fase.domain.model.GrupoCargo grupo) {
        OffsetDateTime agora = OffsetDateTime.now();
        GrupoCargos entidade;
        if (grupo.id() == null) {
            entidade = new GrupoCargos();
            entidade.edital = Edital.findById(grupo.editalId());
            entidade.createdAt = agora;
        } else {
            entidade = GrupoCargos.findById(grupo.id());
        }
        entidade.nome = grupo.nome();
        entidade.updatedAt = agora;
        entidade.persist();
        RestricaoDoBanco.flush(() -> GrupoCargos.flush(), "Já existe um grupo chamado " + grupo.nome() + " neste edital.");
        return paraDominio(entidade);
    }

    public void excluir(int id) {
        GrupoCargos.deleteById(id);
        RestricaoDoBanco.flush(() -> GrupoCargos.flush(), "O grupo " + id + " ainda está em uso e não pode ser excluído.");
    }

    public List<br.bioregistro.fase.domain.model.Cargo> cargosDoEdital(int editalId) {
        return Cargo.<Cargo>list("ediId = ?1 order by carDescricao", editalId).stream()
                .map(c -> new br.bioregistro.fase.domain.model.Cargo(c.carId, c.carDescricao))
                .toList();
    }

    public List<Integer> cargosDoGrupo(int grupoId) {
        return CargoGrupo.<CargoGrupo>list("grupoCargos.id = ?1 order by cargoId", grupoId).stream()
                .map(v -> v.cargoId)
                .toList();
    }

    public Optional<Integer> grupoDoCargo(int cargoId) {
        return CargoGrupo.<CargoGrupo>find("cargoId", cargoId).firstResultOptional()
                .map(v -> v.grupoCargos.id);
    }

    public void adicionarCargo(int grupoId, int cargoId) {
        CargoGrupo vinculo = new CargoGrupo();
        vinculo.grupoCargos = GrupoCargos.findById(grupoId);
        vinculo.cargoId = cargoId;
        vinculo.persist();
        RestricaoDoBanco.flush(() -> CargoGrupo.flush(), "O cargo " + cargoId + " já está em outro grupo.");
    }

    public void removerCargo(int grupoId, int cargoId) {
        CargoGrupo.delete("grupoCargos.id = ?1 and cargoId = ?2", grupoId, cargoId);
    }

    public void vincularFase(int faseId, int grupoId) {
        FaseGrupoCargos vinculo = new FaseGrupoCargos();
        vinculo.fase = Fase.findById(faseId);
        vinculo.grupoCargos = GrupoCargos.findById(grupoId);
        vinculo.persist();
    }

    public void desvincularFase(int faseId) {
        FaseGrupoCargos.delete("fase.id", faseId);
    }

    public boolean possuiFases(int grupoId) {
        return FaseGrupoCargos.count("grupoCargos.id", grupoId) > 0;
    }

    private static br.bioregistro.fase.domain.model.GrupoCargo paraDominio(GrupoCargos e) {
        return new br.bioregistro.fase.domain.model.GrupoCargo(e.id, e.edital.ediId, e.nome);
    }
}

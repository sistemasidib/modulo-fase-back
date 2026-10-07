package br.bioregistro.fase.infrastructure.persistence.idib;

import br.bioregistro.fase.infrastructure.persistence.RestricaoDoBanco;
import br.com.bio.registro.core.runtime.entities.idib.dbo.Edital;
import br.com.bio.registro.core.runtime.entities.idib.dbo.Fase;
import br.com.bio.registro.core.runtime.entities.idib.dbo.FaseGrupoCargos;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/** Fases no banco IDIB ({@code dbo.fases}). */
@ApplicationScoped
public class IdibFases {

    public Optional<br.bioregistro.fase.domain.model.Fase> buscar(int id) {
        return Fase.<Fase>findByIdOptional(id).map(IdibFases::paraDominio);
    }

    public List<br.bioregistro.fase.domain.model.Fase> listarPorEdital(int editalId) {
        return Fase.<Fase>list("edital.ediId = ?1 order by ordem", editalId).stream()
                .map(IdibFases::paraDominio)
                .toList();
    }

    public List<br.bioregistro.fase.domain.model.Fase> listarPorGrupo(int grupoId) {
        return FaseGrupoCargos.<FaseGrupoCargos>list("grupoCargos.id = ?1 order by fase.ordem", grupoId).stream()
                .map(vinculo -> paraDominio(vinculo.fase))
                .toList();
    }

    public short maiorOrdem(int editalId) {
        Short maior = Fase.getEntityManager()
                .createQuery("select max(f.ordem) from Fase f where f.edital.ediId = :edital", Short.class)
                .setParameter("edital", editalId)
                .getSingleResult();
        return maior == null ? 0 : maior;
    }

    public boolean editalExiste(int editalId) {
        return Edital.count("ediId", editalId) > 0;
    }

    public boolean ordemEmUso(int editalId, short ordem, Integer ignorarFaseId) {
        if (ignorarFaseId == null) {
            return Fase.count("edital.ediId = ?1 and ordem = ?2", editalId, ordem) > 0;
        }
        return Fase.count("edital.ediId = ?1 and ordem = ?2 and id <> ?3", editalId, ordem, ignorarFaseId) > 0;
    }

    public br.bioregistro.fase.domain.model.Fase salvar(br.bioregistro.fase.domain.model.Fase fase) {
        OffsetDateTime agora = OffsetDateTime.now();
        Fase entidade;
        if (fase.id() == null) {
            entidade = new Fase();
            entidade.edital = Edital.findById(fase.editalId());
            entidade.createdAt = agora;
        } else {
            entidade = Fase.findById(fase.id());
        }
        entidade.nome = fase.nome();
        entidade.descricao = fase.descricao();
        entidade.ordem = fase.ordem();
        entidade.dataAbertura = fase.dataAbertura();
        entidade.dataFechamento = fase.dataFechamento();
        entidade.updatedAt = agora;
        entidade.persist();
        RestricaoDoBanco.flush(() -> Fase.flush(), "Já existe uma fase com a ordem " + fase.ordem() + " neste edital.");
        return paraDominio(entidade);
    }

    public void excluir(int id) {
        Fase.deleteById(id);
        RestricaoDoBanco.flush(() -> Fase.flush(),
                "A fase " + id + " tem etapas ou candidatos vinculados e não pode ser excluída.");
    }

    private static br.bioregistro.fase.domain.model.Fase paraDominio(Fase e) {
        return new br.bioregistro.fase.domain.model.Fase(
                e.id, e.edital.ediId, e.nome, e.descricao, e.ordem, e.dataAbertura, e.dataFechamento);
    }
}

package br.bioregistro.fase.infrastructure.persistence.idib;

import br.bioregistro.fase.infrastructure.persistence.RestricaoDoBanco;
import br.com.bio.registro.core.runtime.entities.idib.dbo.Etapa;
import br.com.bio.registro.core.runtime.entities.idib.dbo.Fase;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/** Etapas no banco IDIB ({@code dbo.etapa}). */
@ApplicationScoped
public class IdibEtapas {

    public Optional<br.bioregistro.fase.domain.model.Etapa> buscar(int id) {
        return Etapa.<Etapa>findByIdOptional(id).map(IdibEtapas::paraDominio);
    }

    public List<br.bioregistro.fase.domain.model.Etapa> listarPorFase(int faseId) {
        return Etapa.<Etapa>list("fase.id = ?1 order by id", faseId).stream()
                .map(IdibEtapas::paraDominio)
                .toList();
    }

    public br.bioregistro.fase.domain.model.Etapa salvar(br.bioregistro.fase.domain.model.Etapa etapa) {
        OffsetDateTime agora = OffsetDateTime.now();
        Etapa entidade;
        if (etapa.id() == null) {
            entidade = new Etapa();
            entidade.fase = Fase.findById(etapa.faseId());
            entidade.createdAt = agora;
        } else {
            entidade = Etapa.findById(etapa.id());
        }
        entidade.nome = etapa.nome();
        entidade.tipoId = etapa.tipoId();
        entidade.descricao = etapa.descricao();
        entidade.dataAbertura = etapa.dataAbertura();
        entidade.dataFechamento = etapa.dataFechamento();
        entidade.statusId = etapa.statusId();
        entidade.reprovavel = etapa.reprovavel();
        entidade.obrigatoriaParaFase = etapa.obrigatoriaParaFase();
        entidade.todosCandidatos = etapa.todosCandidatos();
        entidade.aceitaDocumentos = etapa.aceitaDocumentos();
        entidade.aceitaRecurso = etapa.aceitaRecurso();
        entidade.preliminar = etapa.preliminar();
        entidade.elegibilidadeEtapaId = etapa.elegibilidadeEtapaId();
        entidade.elegibilidadeStatusId = etapa.elegibilidadeStatusId();
        entidade.responsavelId = etapa.responsavelId();
        entidade.publicadaEm = etapa.publicadaEm();
        entidade.updatedAt = agora;
        entidade.persist();
        RestricaoDoBanco.flush(() -> Etapa.flush(), "A etapa viola uma restrição do banco (tipo ou status inexistente?).");
        return paraDominio(entidade);
    }

    public void excluir(int id) {
        Etapa.deleteById(id);
        RestricaoDoBanco.flush(() -> Etapa.flush(),
                "A etapa " + id + " tem candidatos, documentos ou recursos vinculados e não pode ser excluída.");
    }

    private static br.bioregistro.fase.domain.model.Etapa paraDominio(Etapa e) {
        return new br.bioregistro.fase.domain.model.Etapa(
                e.id, e.fase.id, e.nome, e.tipoId, e.descricao, e.dataAbertura, e.dataFechamento, e.statusId,
                Boolean.TRUE.equals(e.reprovavel), Boolean.TRUE.equals(e.obrigatoriaParaFase),
                Boolean.TRUE.equals(e.todosCandidatos), Boolean.TRUE.equals(e.aceitaDocumentos),
                e.aceitaRecurso, e.preliminar, e.elegibilidadeEtapaId, e.elegibilidadeStatusId, e.responsavelId,
                e.publicadaEm);
    }
}

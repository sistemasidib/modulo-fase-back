package br.bioregistro.fase.infrastructure.persistence.idib;

import br.bioregistro.fase.domain.model.TipoPublicacao;
import br.bioregistro.fase.infrastructure.persistence.RestricaoDoBanco;
import br.com.bio.registro.core.runtime.entities.idib.dbo.Etapa;
import br.com.bio.registro.core.runtime.entities.idib.dbo.Fase;
import br.com.bio.registro.core.runtime.entities.idib.dbo.Publicacao;
import br.com.bio.registro.core.runtime.entities.idib.dbo.faseDominio.DomTipoPublicacao;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/** Publicações no banco IDIB ({@code dbo.publicacao}). */
@ApplicationScoped
public class IdibPublicacoes {

    public Optional<br.bioregistro.fase.domain.model.Publicacao> buscar(int id) {
        return Publicacao.<Publicacao>findByIdOptional(id).map(IdibPublicacoes::paraDominio);
    }

    public List<br.bioregistro.fase.domain.model.Publicacao> listarPorFase(int faseId) {
        return Publicacao.<Publicacao>list("fase.id = ?1 order by id", faseId).stream()
                .map(IdibPublicacoes::paraDominio)
                .toList();
    }

    public List<br.bioregistro.fase.domain.model.Publicacao> listarPorEtapa(int etapaId) {
        return Publicacao.<Publicacao>list("etapa.id = ?1 order by id", etapaId).stream()
                .map(IdibPublicacoes::paraDominio)
                .toList();
    }

    public br.bioregistro.fase.domain.model.Publicacao salvar(br.bioregistro.fase.domain.model.Publicacao p) {
        OffsetDateTime agora = OffsetDateTime.now();
        Publicacao entidade;
        if (p.id() == null) {
            entidade = new Publicacao();
            entidade.fase = Fase.findById(p.faseId());
            entidade.etapa = p.etapaId() == null ? null : Etapa.findById(p.etapaId());
            entidade.createdAt = agora;
        } else {
            entidade = Publicacao.findById(p.id());
        }
        entidade.tipo = DomTipoPublicacao.findById(p.tipo().id());
        entidade.dataPrevista = p.dataPrevista();
        entidade.visualizacaoInicio = p.visualizacaoInicio();
        entidade.visualizacaoFim = p.visualizacaoFim();
        entidade.recursoInicio = p.recursoInicio();
        entidade.recursoFim = p.recursoFim();
        entidade.publicadaEm = p.publicadaEm();
        entidade.updatedAt = agora;
        entidade.persist();
        RestricaoDoBanco.flush(() -> Publicacao.flush(), "A publicação viola uma restrição do banco.");
        return paraDominio(entidade);
    }

    public void excluir(int id) {
        Publicacao.deleteById(id);
        RestricaoDoBanco.flush(() -> Publicacao.flush(), "A publicação " + id + " está em uso e não pode ser excluída.");
    }

    static TipoPublicacao tipo(DomTipoPublicacao t) {
        return new TipoPublicacao(t.id, t.valor, t.descricao, Boolean.TRUE.equals(t.abreRecurso));
    }

    private static br.bioregistro.fase.domain.model.Publicacao paraDominio(Publicacao e) {
        return new br.bioregistro.fase.domain.model.Publicacao(
                e.id, e.fase.id, e.etapa == null ? null : e.etapa.id, tipo(e.tipo), e.dataPrevista,
                e.visualizacaoInicio, e.visualizacaoFim, e.recursoInicio, e.recursoFim, e.publicadaEm);
    }
}

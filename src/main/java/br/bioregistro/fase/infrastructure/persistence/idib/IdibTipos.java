package br.bioregistro.fase.infrastructure.persistence.idib;

import br.bioregistro.fase.domain.model.TipoEtapa;
import br.bioregistro.fase.domain.model.TipoPublicacao;
import br.com.bio.registro.core.runtime.entities.idib.dbo.faseDominio.DomTipoEtapa;
import br.com.bio.registro.core.runtime.entities.idib.dbo.faseDominio.DomTipoEtapaPublicacao;
import br.com.bio.registro.core.runtime.entities.idib.dbo.faseDominio.DomTipoPublicacao;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;

/** Tabelas de domínio do banco IDIB. */
@ApplicationScoped
public class IdibTipos {

    public List<TipoEtapa> tiposEtapa() {
        return DomTipoEtapa.<DomTipoEtapa>listAll(Sort.by("id")).stream().map(IdibTipos::tipoEtapa).toList();
    }

    public Optional<TipoEtapa> tipoEtapa(int id) {
        return DomTipoEtapa.<DomTipoEtapa>findByIdOptional(id).map(IdibTipos::tipoEtapa);
    }

    public List<TipoPublicacao> tiposPublicacao() {
        return DomTipoPublicacao.<DomTipoPublicacao>listAll(Sort.by("id")).stream()
                .map(IdibPublicacoes::tipo)
                .toList();
    }

    public Optional<TipoPublicacao> tipoPublicacao(int id) {
        return DomTipoPublicacao.<DomTipoPublicacao>findByIdOptional(id).map(IdibPublicacoes::tipo);
    }

    public Optional<TipoPublicacao> tipoPublicacao(String valor) {
        return DomTipoPublicacao.<DomTipoPublicacao>find("valor", valor).firstResultOptional()
                .map(IdibPublicacoes::tipo);
    }

    public List<TipoPublicacao> publicacoesPadrao(int tipoEtapaId) {
        return DomTipoEtapaPublicacao.<DomTipoEtapaPublicacao>list(
                        "tipoEtapaId = ?1 order by tipoPublicacao.id", tipoEtapaId).stream()
                .map(padrao -> IdibPublicacoes.tipo(padrao.tipoPublicacao))
                .toList();
    }

    private static TipoEtapa tipoEtapa(DomTipoEtapa t) {
        return new TipoEtapa(t.id, t.valor, t.descricao);
    }
}

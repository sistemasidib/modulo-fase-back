package br.bioregistro.fase.infrastructure.persistence;

import br.bioregistro.fase.domain.model.CriterioDesempate;
import br.bioregistro.fase.domain.model.CriterioDesempate.FaseAplicacao;
import br.bioregistro.fase.domain.model.CriterioDesempate.Regra;
import br.bioregistro.fase.domain.model.CriterioDesempate.Tipo;
import br.bioregistro.fase.domain.model.Instituicao;
import br.bioregistro.fase.domain.port.CriterioDesempateRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class CriterioDesempateRepositoryAdapter implements CriterioDesempateRepository {

    private static final String SELECT = """
            SELECT id, fase_id, ordem, criterio, tipo, regra, referencia, fase_aplicacao, fundamentacao, observacao
            FROM dbo.criterio_desempate
            """;

    private final Sql sql;

    public CriterioDesempateRepositoryAdapter(Sql sql) {
        this.sql = sql;
    }

    @Override
    public List<CriterioDesempate> daFase(Instituicao instituicao, int faseId) {
        return sql.linhas(instituicao, SELECT + " WHERE fase_id = ?1 ORDER BY ordem, id", faseId).stream()
                .map(CriterioDesempateRepositoryAdapter::criterio)
                .toList();
    }

    @Override
    public Optional<CriterioDesempate> buscar(Instituicao instituicao, int id) {
        return sql.linhas(instituicao, SELECT + " WHERE id = ?1", id).stream()
                .findFirst()
                .map(CriterioDesempateRepositoryAdapter::criterio);
    }

    @Override
    public CriterioDesempate salvar(Instituicao instituicao, CriterioDesempate c) {
        if (c.id() == null) {
            Object id = sql.valor(instituicao, """
                    INSERT INTO dbo.criterio_desempate
                        (fase_id, ordem, criterio, tipo, regra, referencia, fase_aplicacao, fundamentacao, observacao,
                         created_at, updated_at)
                    OUTPUT INSERTED.id
                    VALUES (?1, ?2, ?3, ?4, ?5, ?6, ?7, ?8, ?9, SYSDATETIMEOFFSET(), SYSDATETIMEOFFSET())""",
                    c.faseId(), c.ordem(), c.criterio(), c.tipo().name(), c.regra().name(), Sql.texto(c.referencia()),
                    c.faseAplicacao().name(), Sql.texto(c.fundamentacao()), Sql.texto(c.observacao()));
            return c.comId(Sql.comoInteiro(id));
        }
        sql.executar(instituicao, """
                UPDATE dbo.criterio_desempate
                SET ordem = ?1, criterio = ?2, tipo = ?3, regra = ?4, referencia = ?5, fase_aplicacao = ?6,
                    fundamentacao = ?7, observacao = ?8, updated_at = SYSDATETIMEOFFSET()
                WHERE id = ?9""",
                c.ordem(), c.criterio(), c.tipo().name(), c.regra().name(), Sql.texto(c.referencia()),
                c.faseAplicacao().name(), Sql.texto(c.fundamentacao()), Sql.texto(c.observacao()), c.id());
        return c;
    }

    @Override
    public void excluir(Instituicao instituicao, int id) {
        sql.executar(instituicao, "DELETE FROM dbo.criterio_desempate WHERE id = ?1", id);
    }

    @Override
    public void excluirDaFase(Instituicao instituicao, int faseId) {
        sql.executar(instituicao, "DELETE FROM dbo.criterio_desempate WHERE fase_id = ?1", faseId);
    }

    private static CriterioDesempate criterio(Object[] l) {
        return new CriterioDesempate(
                Sql.comoInteiro(l[0]), Sql.comoInteiro(l[1]), Sql.comoInteiro(l[2]), texto(l[3]),
                Tipo.valueOf(texto(l[4])), Regra.valueOf(texto(l[5])), texto(l[6]),
                FaseAplicacao.valueOf(texto(l[7])), texto(l[8]), texto(l[9]));
    }

    private static String texto(Object valor) {
        return valor == null ? null : valor.toString().trim();
    }
}

package br.bioregistro.fase.infrastructure.persistence;

import br.bioregistro.fase.domain.model.CriterioCorrecao;
import br.bioregistro.fase.domain.model.Instituicao;
import br.bioregistro.fase.domain.port.CriterioCorrecaoRepository;
import jakarta.enterprise.context.ApplicationScoped;
import org.hibernate.query.TypedParameterValue;
import org.hibernate.type.StandardBasicTypes;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class CriterioCorrecaoRepositoryAdapter implements CriterioCorrecaoRepository {

    private static final String SELECT = """
            SELECT id, etapa_id, ordem, modulo, criterio, desconto_por_erro, pontuacao_maxima
            FROM dbo.criterio_correcao
            """;

    private final Sql sql;

    public CriterioCorrecaoRepositoryAdapter(Sql sql) {
        this.sql = sql;
    }

    @Override
    public List<CriterioCorrecao> daEtapa(Instituicao instituicao, int etapaId) {
        return sql.linhas(instituicao, SELECT + " WHERE etapa_id = ?1 ORDER BY ordem, id", etapaId).stream()
                .map(CriterioCorrecaoRepositoryAdapter::criterio)
                .toList();
    }

    @Override
    public Optional<CriterioCorrecao> buscar(Instituicao instituicao, int id) {
        return sql.linhas(instituicao, SELECT + " WHERE id = ?1", id).stream()
                .findFirst()
                .map(CriterioCorrecaoRepositoryAdapter::criterio);
    }

    @Override
    public CriterioCorrecao salvar(Instituicao instituicao, CriterioCorrecao c) {
        if (c.id() == null) {
            Object id = sql.valor(instituicao, """
                    INSERT INTO dbo.criterio_correcao
                        (etapa_id, ordem, modulo, criterio, desconto_por_erro, pontuacao_maxima, created_at, updated_at)
                    OUTPUT INSERTED.id
                    VALUES (?1, ?2, ?3, ?4, ?5, ?6, SYSDATETIMEOFFSET(), SYSDATETIMEOFFSET())""",
                    c.etapaId(), c.ordem(), c.modulo(), c.criterio(), decimal(c.descontoPorErro()),
                    c.pontuacaoMaxima());
            return c.comId(Sql.comoInteiro(id));
        }
        sql.executar(instituicao, """
                UPDATE dbo.criterio_correcao
                SET ordem = ?1, modulo = ?2, criterio = ?3, desconto_por_erro = ?4, pontuacao_maxima = ?5,
                    updated_at = SYSDATETIMEOFFSET()
                WHERE id = ?6""",
                c.ordem(), c.modulo(), c.criterio(), decimal(c.descontoPorErro()), c.pontuacaoMaxima(), c.id());
        return c;
    }

    @Override
    public void excluir(Instituicao instituicao, int id) {
        sql.executar(instituicao, "DELETE FROM dbo.criterio_correcao WHERE id = ?1", id);
    }

    @Override
    public void excluirDaEtapa(Instituicao instituicao, int etapaId) {
        sql.executar(instituicao, "DELETE FROM dbo.criterio_correcao WHERE etapa_id = ?1", etapaId);
    }

    private static TypedParameterValue<BigDecimal> decimal(BigDecimal valor) {
        return new TypedParameterValue<>(StandardBasicTypes.BIG_DECIMAL, valor);
    }

    private static CriterioCorrecao criterio(Object[] l) {
        return new CriterioCorrecao(
                Sql.comoInteiro(l[0]), Sql.comoInteiro(l[1]), Sql.comoInteiro(l[2]), texto(l[3]), texto(l[4]),
                l[5] == null ? null : new BigDecimal(l[5].toString()), new BigDecimal(l[6].toString()));
    }

    private static String texto(Object valor) {
        return valor == null ? null : valor.toString().trim();
    }
}

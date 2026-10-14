package br.bioregistro.fase.infrastructure.persistence;

import br.bioregistro.fase.domain.model.EditalResumo;
import br.bioregistro.fase.domain.model.Entidade;
import br.bioregistro.fase.domain.model.Instituicao;
import br.bioregistro.fase.domain.port.EntidadeRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;

/** Tabelas {@code entidade} e {@code entidade_edital} ({@code db/017}), iguais nos dois bancos. */
@ApplicationScoped
public class EntidadeRepositoryAdapter implements EntidadeRepository {

    private final Sql sql;

    public EntidadeRepositoryAdapter(Sql sql) {
        this.sql = sql;
    }

    @Override
    public List<Entidade> listar(Instituicao instituicao) {
        return sql.linhas(instituicao, "SELECT id, nome, cnpj FROM dbo.entidade ORDER BY nome").stream()
                .map(EntidadeRepositoryAdapter::entidade)
                .toList();
    }

    @Override
    public Optional<Entidade> buscar(Instituicao instituicao, int id) {
        return sql.linhas(instituicao, "SELECT id, nome, cnpj FROM dbo.entidade WHERE id = ?1", id).stream()
                .findFirst()
                .map(EntidadeRepositoryAdapter::entidade);
    }

    @Override
    public boolean nomeEmUso(Instituicao instituicao, String nome, Integer ignorarEntidadeId) {
        Object total = sql.valor(instituicao,
                "SELECT COUNT(*) FROM dbo.entidade WHERE nome = ?1 AND (?2 IS NULL OR id <> ?2)",
                nome, Sql.inteiro(ignorarEntidadeId));
        return Sql.comoInteiro(total) > 0;
    }

    @Override
    public boolean cnpjEmUso(Instituicao instituicao, String cnpj, Integer ignorarEntidadeId) {
        Object total = sql.valor(instituicao,
                "SELECT COUNT(*) FROM dbo.entidade WHERE cnpj = ?1 AND (?2 IS NULL OR id <> ?2)",
                cnpj, Sql.inteiro(ignorarEntidadeId));
        return Sql.comoInteiro(total) > 0;
    }

    @Override
    public Entidade salvar(Instituicao instituicao, Entidade entidade) {
        if (entidade.id() == null) {
            Object id = sql.valor(instituicao, """
                    INSERT INTO dbo.entidade (nome, cnpj, created_at, updated_at)
                    OUTPUT INSERTED.id
                    VALUES (?1, ?2, SYSDATETIMEOFFSET(), SYSDATETIMEOFFSET())""",
                    entidade.nome(), Sql.texto(entidade.cnpj()));
            return entidade.comId(Sql.comoInteiro(id));
        }
        sql.executar(instituicao,
                "UPDATE dbo.entidade SET nome = ?1, cnpj = ?2, updated_at = SYSDATETIMEOFFSET() WHERE id = ?3",
                entidade.nome(), Sql.texto(entidade.cnpj()), entidade.id());
        return entidade;
    }

    @Override
    public void excluir(Instituicao instituicao, int id) {
        sql.executar(instituicao, "DELETE FROM dbo.entidade_edital WHERE entidade_id = ?1", id);
        sql.executar(instituicao, "DELETE FROM dbo.entidade WHERE id = ?1", id);
    }

    @Override
    public List<EditalResumo> editais(Instituicao instituicao, int entidadeId) {
        return sql.linhas(instituicao, """
                        SELECT e.ediId, e.ediNomeConcurso
                        FROM dbo.entidade_edital ee JOIN dbo.Edital e ON e.ediId = ee.edital_id
                        WHERE ee.entidade_id = ?1
                        ORDER BY e.ediId""", entidadeId).stream()
                .map(EntidadeRepositoryAdapter::edital)
                .toList();
    }

    @Override
    public Optional<Entidade> entidadeDoEdital(Instituicao instituicao, int editalId) {
        return sql.linhas(instituicao, """
                        SELECT en.id, en.nome, en.cnpj
                        FROM dbo.entidade_edital ee JOIN dbo.entidade en ON en.id = ee.entidade_id
                        WHERE ee.edital_id = ?1""", editalId).stream()
                .findFirst()
                .map(EntidadeRepositoryAdapter::entidade);
    }

    @Override
    public void incluirEdital(Instituicao instituicao, int entidadeId, int editalId) {
        sql.executar(instituicao, "INSERT INTO dbo.entidade_edital (entidade_id, edital_id) VALUES (?1, ?2)",
                entidadeId, editalId);
    }

    @Override
    public void retirarEdital(Instituicao instituicao, int entidadeId, int editalId) {
        sql.executar(instituicao, "DELETE FROM dbo.entidade_edital WHERE entidade_id = ?1 AND edital_id = ?2",
                entidadeId, editalId);
    }

    @Override
    public List<EditalResumo> editaisSemEntidade(Instituicao instituicao) {
        return sql.linhas(instituicao, """
                        SELECT e.ediId, e.ediNomeConcurso
                        FROM dbo.Edital e
                        LEFT JOIN dbo.entidade_edital ee ON ee.edital_id = e.ediId
                        WHERE ee.id IS NULL AND e.ediDtExclusao IS NULL
                        ORDER BY e.ediId DESC""").stream()
                .map(EntidadeRepositoryAdapter::edital)
                .toList();
    }

    private static Entidade entidade(Object[] linha) {
        return new Entidade(Sql.comoInteiro(linha[0]), (String) linha[1], (String) linha[2]);
    }

    private static EditalResumo edital(Object[] linha) {
        return new EditalResumo(Sql.comoInteiro(linha[0]), (String) linha[1]);
    }
}

package br.bioregistro.fase.infrastructure.persistence;

import br.bioregistro.fase.domain.model.Concurso;
import br.bioregistro.fase.domain.model.EditalResumo;
import br.bioregistro.fase.domain.model.Instituicao;
import br.bioregistro.fase.domain.port.ConcursoRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;

/** Tabelas {@code concurso} e {@code concurso_edital}, iguais nos dois bancos. */
@ApplicationScoped
public class ConcursoRepositoryAdapter implements ConcursoRepository {

    private final Sql sql;

    public ConcursoRepositoryAdapter(Sql sql) {
        this.sql = sql;
    }

    @Override
    public List<Concurso> listar(Instituicao instituicao) {
        return sql.linhas(instituicao, "SELECT id, nome FROM dbo.concurso ORDER BY nome").stream()
                .map(ConcursoRepositoryAdapter::concurso)
                .toList();
    }

    @Override
    public Optional<Concurso> buscar(Instituicao instituicao, int id) {
        return sql.linhas(instituicao, "SELECT id, nome FROM dbo.concurso WHERE id = ?1", id).stream()
                .findFirst()
                .map(ConcursoRepositoryAdapter::concurso);
    }

    @Override
    public boolean nomeEmUso(Instituicao instituicao, String nome, Integer ignorarConcursoId) {
        Object total = sql.valor(instituicao,
                "SELECT COUNT(*) FROM dbo.concurso WHERE nome = ?1 AND (?2 IS NULL OR id <> ?2)",
                nome, Sql.inteiro(ignorarConcursoId));
        return Sql.comoInteiro(total) > 0;
    }

    @Override
    public Concurso salvar(Instituicao instituicao, Concurso concurso) {
        if (concurso.id() == null) {
            Object id = sql.valor(instituicao, """
                    INSERT INTO dbo.concurso (nome, created_at, updated_at)
                    OUTPUT INSERTED.id
                    VALUES (?1, SYSDATETIMEOFFSET(), SYSDATETIMEOFFSET())""", concurso.nome());
            return concurso.comId(Sql.comoInteiro(id));
        }
        sql.executar(instituicao, "UPDATE dbo.concurso SET nome = ?1, updated_at = SYSDATETIMEOFFSET() WHERE id = ?2",
                concurso.nome(), concurso.id());
        return concurso;
    }

    @Override
    public void excluir(Instituicao instituicao, int id) {
        sql.executar(instituicao, "DELETE FROM dbo.concurso_edital WHERE concurso_id = ?1", id);
        sql.executar(instituicao, "DELETE FROM dbo.concurso WHERE id = ?1", id);
    }

    @Override
    public List<EditalResumo> editais(Instituicao instituicao, int concursoId) {
        return sql.linhas(instituicao, """
                        SELECT e.ediId, e.ediNomeConcurso
                        FROM dbo.concurso_edital ce JOIN dbo.Edital e ON e.ediId = ce.edital_id
                        WHERE ce.concurso_id = ?1
                        ORDER BY e.ediId""", concursoId).stream()
                .map(ConcursoRepositoryAdapter::edital)
                .toList();
    }

    @Override
    public Optional<Concurso> concursoDoEdital(Instituicao instituicao, int editalId) {
        return sql.linhas(instituicao, """
                        SELECT c.id, c.nome
                        FROM dbo.concurso_edital ce JOIN dbo.concurso c ON c.id = ce.concurso_id
                        WHERE ce.edital_id = ?1""", editalId).stream()
                .findFirst()
                .map(ConcursoRepositoryAdapter::concurso);
    }

    @Override
    public void incluirEdital(Instituicao instituicao, int concursoId, int editalId) {
        sql.executar(instituicao, "INSERT INTO dbo.concurso_edital (concurso_id, edital_id) VALUES (?1, ?2)",
                concursoId, editalId);
    }

    @Override
    public void retirarEdital(Instituicao instituicao, int concursoId, int editalId) {
        sql.executar(instituicao, "DELETE FROM dbo.concurso_edital WHERE concurso_id = ?1 AND edital_id = ?2",
                concursoId, editalId);
    }

    @Override
    public List<EditalResumo> editaisSemConcurso(Instituicao instituicao) {
        return sql.linhas(instituicao, """
                        SELECT e.ediId, e.ediNomeConcurso
                        FROM dbo.Edital e
                        LEFT JOIN dbo.concurso_edital ce ON ce.edital_id = e.ediId
                        WHERE ce.id IS NULL AND e.ediDtExclusao IS NULL
                        ORDER BY e.ediId DESC""").stream()
                .map(ConcursoRepositoryAdapter::edital)
                .toList();
    }

    private static Concurso concurso(Object[] linha) {
        return new Concurso(Sql.comoInteiro(linha[0]), (String) linha[1]);
    }

    private static EditalResumo edital(Object[] linha) {
        return new EditalResumo(Sql.comoInteiro(linha[0]), (String) linha[1]);
    }
}

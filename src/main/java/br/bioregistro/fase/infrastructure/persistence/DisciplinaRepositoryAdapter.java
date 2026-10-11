package br.bioregistro.fase.infrastructure.persistence;

import br.bioregistro.fase.domain.model.DisciplinaDaProva;
import br.bioregistro.fase.domain.model.Instituicao;
import br.bioregistro.fase.domain.port.DisciplinaRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * {@code Disciplinas}, {@code DisciplinasEditais} e {@code DisciplinasEditaisQuestoes} (iguais nos
 * dois bancos). {@code peso} vem de {@code db/007} e {@code etapa_id} de {@code db/009}.
 */
@ApplicationScoped
public class DisciplinaRepositoryAdapter implements DisciplinaRepository {

    private static final String SELECT = """
            SELECT deq.disciplinaEditalQuestaoId, de.disciplinaEditalId, d.disciplinaId, d.nome,
                   deq.questaoInicio, deq.questaoFim, deq.cargo_id, c.carDescricao, deq.etapa_id, deq.peso
            FROM dbo.DisciplinasEditaisQuestoes deq
            JOIN dbo.DisciplinasEditais de ON de.disciplinaEditalId = deq.disciplinaEditalId AND de.ativo = 1
            JOIN dbo.Disciplinas d ON d.disciplinaId = de.disciplinaId
            LEFT JOIN dbo.Cargo c ON c.carId = deq.cargo_id
            WHERE deq.ativo = 1
            """;

    private final Sql sql;

    public DisciplinaRepositoryAdapter(Sql sql) {
        this.sql = sql;
    }

    @Override
    public List<DisciplinaDaProva> daEtapa(Instituicao instituicao, int editalId, int etapaId, boolean incluirSemEtapa) {
        String daEtapa = incluirSemEtapa ? "(deq.etapa_id = ?2 OR deq.etapa_id IS NULL)" : "deq.etapa_id = ?2";
        return sql.linhas(instituicao, SELECT + " AND de.editalId = ?1 AND " + daEtapa
                        + " ORDER BY deq.questaoInicio, d.nome", editalId, etapaId)
                .stream().map(DisciplinaRepositoryAdapter::disciplina).toList();
    }

    @Override
    public Optional<DisciplinaDaProva> buscar(Instituicao instituicao, int faixaId) {
        return sql.linhas(instituicao, SELECT + " AND deq.disciplinaEditalQuestaoId = ?1", faixaId)
                .stream().findFirst().map(DisciplinaRepositoryAdapter::disciplina);
    }

    @Override
    public void definirPeso(Instituicao instituicao, int faixaId, BigDecimal peso) {
        sql.executar(instituicao,
                "UPDATE dbo.DisciplinasEditaisQuestoes SET peso = ?1 WHERE disciplinaEditalQuestaoId = ?2 AND ativo = 1",
                peso, faixaId);
    }

    @Override
    public int disciplinaDoEdital(Instituicao instituicao, int editalId, String nome) {
        Integer disciplinaId = Sql.comoInteiro(sql.valor(instituicao, """
                SELECT TOP 1 disciplinaId FROM dbo.Disciplinas
                WHERE LOWER(nome) = LOWER(?1) AND ativo = 1 ORDER BY disciplinaId""", nome));
        if (disciplinaId == null) {
            disciplinaId = Sql.comoInteiro(sql.valor(instituicao,
                    "INSERT INTO dbo.Disciplinas (nome, ativo) OUTPUT INSERTED.disciplinaId VALUES (?1, 1)", nome));
        }
        List<Object[]> vinculo = sql.linhas(instituicao, """
                SELECT TOP 1 disciplinaEditalId, ativo FROM dbo.DisciplinasEditais
                WHERE disciplinaId = ?1 AND editalId = ?2 ORDER BY ativo DESC, disciplinaEditalId""",
                disciplinaId, editalId);
        if (vinculo.isEmpty()) {
            return Sql.comoInteiro(sql.valor(instituicao, """
                    INSERT INTO dbo.DisciplinasEditais (editalId, disciplinaId, ativo)
                    OUTPUT INSERTED.disciplinaEditalId VALUES (?1, ?2, 1)""", editalId, disciplinaId));
        }
        int disciplinaEditalId = Sql.comoInteiro(vinculo.get(0)[0]);
        if (!Sql.comoBooleano(vinculo.get(0)[1])) {
            sql.executar(instituicao, "UPDATE dbo.DisciplinasEditais SET ativo = 1 WHERE disciplinaEditalId = ?1",
                    disciplinaEditalId);
        }
        return disciplinaEditalId;
    }

    @Override
    public int inserirFaixa(Instituicao instituicao, int disciplinaEditalId, int etapaId, int inicio, int fim, int cargoId) {
        return Sql.comoInteiro(sql.valor(instituicao, """
                INSERT INTO dbo.DisciplinasEditaisQuestoes
                    (disciplinaEditalId, questaoInicio, questaoFim, ativo, dataCadastro, cargo_id, etapa_id)
                OUTPUT INSERTED.disciplinaEditalQuestaoId
                VALUES (?1, ?2, ?3, 1, DATEADD(hh, -3, GETDATE()), ?4, ?5)""",
                disciplinaEditalId, inicio, fim, cargoId, etapaId));
    }

    @Override
    public void removerFaixa(Instituicao instituicao, int faixaId) {
        sql.executar(instituicao,
                "UPDATE dbo.DisciplinasEditaisQuestoes SET ativo = 0 WHERE disciplinaEditalQuestaoId = ?1", faixaId);
    }

    @Override
    public void removerDaEtapa(Instituicao instituicao, int etapaId) {
        sql.executar(instituicao,
                "UPDATE dbo.DisciplinasEditaisQuestoes SET ativo = 0, etapa_id = NULL WHERE etapa_id = ?1", etapaId);
    }

    private static DisciplinaDaProva disciplina(Object[] l) {
        return new DisciplinaDaProva(
                Sql.comoInteiro(l[0]), Sql.comoInteiro(l[1]), Sql.comoInteiro(l[2]),
                l[3] == null ? null : l[3].toString().trim(),
                Sql.comoInteiro(l[4]), Sql.comoInteiro(l[5]), Sql.comoInteiro(l[6]),
                l[7] == null ? null : l[7].toString().trim(),
                Sql.comoInteiro(l[8]),
                l[9] == null ? null : new BigDecimal(l[9].toString()));
    }
}

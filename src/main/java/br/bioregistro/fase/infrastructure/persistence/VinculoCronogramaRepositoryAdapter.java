package br.bioregistro.fase.infrastructure.persistence;

import br.bioregistro.fase.domain.model.Instituicao;
import br.bioregistro.fase.domain.model.cronograma.LinhaDoCronograma;
import br.bioregistro.fase.domain.model.cronograma.ModuloDeExecucao;
import br.bioregistro.fase.domain.model.cronograma.StatusEtapa;
import br.bioregistro.fase.domain.port.VinculoCronogramaRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * {@code fase_cronograma} e as FKs {@code publicacao.cronograma_etapa_id/cronograma_recurso_id}
 * ({@code db/015}), iguais nos dois bancos. Colunas sem entidade no sys-core: SQL nativo.
 */
@ApplicationScoped
public class VinculoCronogramaRepositoryAdapter implements VinculoCronogramaRepository {

    private static final String DATA = "CONVERT(CHAR(10), %s, 23)";
    private static final String LINHAS = "SELECT p.id, d.codigo, d.ativa, " + DATA.formatted("d.inicio") + ", "
            + DATA.formatted("d.fim") + ", d.status_efetivo, r.codigo, r.ativa, " + DATA.formatted("r.inicio") + ", "
            + DATA.formatted("r.fim") + ", r.status_efetivo"
            + " FROM dbo.publicacao p"
            + " JOIN dbo.cronograma_edital_etapa d ON d.id = p.cronograma_etapa_id"
            + " LEFT JOIN dbo.cronograma_edital_etapa r ON r.id = p.cronograma_recurso_id";

    private final Sql sql;

    public VinculoCronogramaRepositoryAdapter(Sql sql) {
        this.sql = sql;
    }

    @Override
    public Optional<Integer> faseDoModulo(Instituicao instituicao, int editalDoCronogramaId, ModuloDeExecucao modulo) {
        return Optional.ofNullable(Sql.comoInteiro(sql.valor(instituicao,
                "SELECT fases_id FROM dbo.fase_cronograma WHERE cronograma_edital_id = ?1 AND modulo = ?2",
                editalDoCronogramaId, modulo.name())));
    }

    @Override
    public Optional<ModuloDeExecucao> moduloDaFase(Instituicao instituicao, int faseId) {
        return Optional.ofNullable((String) sql.valor(instituicao,
                        "SELECT modulo FROM dbo.fase_cronograma WHERE fases_id = ?1", faseId))
                .map(ModuloDeExecucao::valueOf);
    }

    @Override
    public void vincularFase(Instituicao instituicao, int faseId, int editalDoCronogramaId, ModuloDeExecucao modulo) {
        sql.executar(instituicao,
                "INSERT INTO dbo.fase_cronograma (fases_id, cronograma_edital_id, modulo) VALUES (?1, ?2, ?3)",
                faseId, editalDoCronogramaId, modulo.name());
    }

    @Override
    public void desvincularFase(Instituicao instituicao, int faseId) {
        sql.executar(instituicao, "DELETE FROM dbo.fase_cronograma WHERE fases_id = ?1", faseId);
    }

    @Override
    public void vincularPublicacao(Instituicao instituicao, int publicacaoId, int linhaId, Integer recursoLinhaId) {
        sql.executar(instituicao,
                "UPDATE dbo.publicacao SET cronograma_etapa_id = ?1, cronograma_recurso_id = ?2 WHERE id = ?3",
                linhaId, Sql.inteiro(recursoLinhaId), publicacaoId);
    }

    @Override
    public Map<Integer, LinhaDoCronograma.DaPublicacao> publicacoesDaFase(Instituicao instituicao, int faseId) {
        Map<Integer, LinhaDoCronograma.DaPublicacao> vinculadas = new HashMap<>();
        sql.linhas(instituicao, LINHAS + " WHERE p.fases_id = ?1", faseId)
                .forEach(l -> vinculadas.put(Sql.comoInteiro(l[0]), daPublicacao(l)));
        return vinculadas;
    }

    @Override
    public Optional<LinhaDoCronograma.DaPublicacao> daPublicacao(Instituicao instituicao, int publicacaoId) {
        List<Object[]> linhas = sql.linhas(instituicao, LINHAS + " WHERE p.id = ?1", publicacaoId);
        return linhas.stream().findFirst().map(VinculoCronogramaRepositoryAdapter::daPublicacao);
    }

    private static LinhaDoCronograma.DaPublicacao daPublicacao(Object[] l) {
        LinhaDoCronograma data = new LinhaDoCronograma((String) l[1], Sql.comoBooleano(l[2]), Sql.comoData(l[3]),
                Sql.comoData(l[4]), StatusEtapa.valueOf((String) l[5]));
        LinhaDoCronograma recurso = l[6] == null ? null : new LinhaDoCronograma((String) l[6],
                Sql.comoBooleano(l[7]), Sql.comoData(l[8]), Sql.comoData(l[9]), StatusEtapa.valueOf((String) l[10]));
        return new LinhaDoCronograma.DaPublicacao(data, recurso);
    }
}

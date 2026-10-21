package br.bioregistro.fase.infrastructure.persistence;

import br.bioregistro.fase.domain.model.Instituicao;
import br.bioregistro.fase.domain.model.cronograma.ItemCronogramaPublico;
import br.bioregistro.fase.domain.port.CronogramaPublicoItemRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class CronogramaPublicoItemRepositoryAdapter implements CronogramaPublicoItemRepository {

    private final Sql sql;

    public CronogramaPublicoItemRepositoryAdapter(Sql sql) {
        this.sql = sql;
    }

    @Override
    public List<ItemCronogramaPublico> listar(Instituicao instituicao, int editalId) {
        List<Object[]> linhas = sql.linhas(instituicao, """
                SELECT ordem, descricao, CONVERT(CHAR(10), inicio, 23), CONVERT(CHAR(10), termino, 23), destaque
                FROM dbo.cronograma_publico_item
                WHERE edital_id = ?1
                ORDER BY ordem
                """, editalId);
        List<ItemCronogramaPublico> itens = new ArrayList<>();
        for (Object[] r : linhas) {
            itens.add(new ItemCronogramaPublico(
                    Sql.comoInteiro(r[0]),
                    (String) r[1],
                    Sql.comoData(r[2]),
                    Sql.comoData(r[3]),
                    Sql.comoBooleano(r[4])));
        }
        return itens;
    }

    @Override
    @Transactional
    public List<ItemCronogramaPublico> substituir(Instituicao instituicao, int editalId,
                                                  List<ItemCronogramaPublico> itens) {
        sql.executar(instituicao, "DELETE FROM dbo.cronograma_publico_item WHERE edital_id = ?1", editalId);
        OffsetDateTime agora = OffsetDateTime.now();
        int ordem = 1;
        List<ItemCronogramaPublico> gravados = new ArrayList<>();
        for (ItemCronogramaPublico item : itens) {
            ItemCronogramaPublico comOrdem = new ItemCronogramaPublico(
                    ordem, item.descricao(), item.inicio(), item.termino(), item.destaque());
            sql.executar(instituicao, """
                    INSERT INTO dbo.cronograma_publico_item
                    (edital_id, ordem, descricao, inicio, termino, destaque, created_at, updated_at)
                    VALUES (?1, ?2, ?3, ?4, ?5, ?6, ?7, ?7)
                    """, editalId, comOrdem.ordem(), comOrdem.descricao(),
                    Sql.data(comOrdem.inicio()), Sql.data(comOrdem.termino()),
                    Sql.booleano(comOrdem.destaque()), agora);
            gravados.add(comOrdem);
            ordem++;
        }
        return List.copyOf(gravados);
    }
}

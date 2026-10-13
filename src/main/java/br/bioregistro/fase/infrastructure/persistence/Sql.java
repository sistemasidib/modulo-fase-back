package br.bioregistro.fase.infrastructure.persistence;

import br.bioregistro.fase.domain.model.Instituicao;
import io.quarkus.hibernate.orm.PersistenceUnit;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.hibernate.query.TypedParameterValue;
import org.hibernate.type.StandardBasicTypes;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * SQL nativo nas tabelas do cronograma ({@code db/004-cronograma.sql}), que não têm entidade no
 * sys-core. As tabelas são iguais nos dois bancos, então o mesmo SQL serve para os dois.
 *
 * <p>Parâmetros que podem ser nulos vão por {@link #inteiro}/{@link #texto}/{@link #data}: o
 * SQL Server recusa nulo sem tipo. Datas voltam como texto ISO (CONVERT no SELECT) para não
 * depender do tipo que o driver devolve.
 */
@ApplicationScoped
public class Sql {

    private final EntityManager idecan;
    private final EntityManager idib;

    public Sql(@PersistenceUnit("idecan") EntityManager idecan, @PersistenceUnit("idib") EntityManager idib) {
        this.idecan = idecan;
        this.idib = idib;
    }

    public List<Object[]> linhas(Instituicao instituicao, String sql, Object... parametros) {
        List<?> resultado = query(instituicao, sql, parametros).getResultList();
        return resultado.stream()
                .map(linha -> linha instanceof Object[] colunas ? colunas : new Object[]{linha})
                .toList();
    }

    public Object valor(Instituicao instituicao, String sql, Object... parametros) {
        List<Object[]> linhas = linhas(instituicao, sql, parametros);
        return linhas.isEmpty() ? null : linhas.get(0)[0];
    }

    public int executar(Instituicao instituicao, String sql, Object... parametros) {
        return query(instituicao, sql, parametros).executeUpdate();
    }

    public static TypedParameterValue<Integer> inteiro(Integer valor) {
        return new TypedParameterValue<>(StandardBasicTypes.INTEGER, valor);
    }

    public static TypedParameterValue<Boolean> booleano(Boolean valor) {
        return new TypedParameterValue<>(StandardBasicTypes.BOOLEAN, valor);
    }

    public static TypedParameterValue<String> texto(String valor) {
        return new TypedParameterValue<>(StandardBasicTypes.STRING, valor);
    }

    public static TypedParameterValue<LocalDate> data(LocalDate valor) {
        return new TypedParameterValue<>(StandardBasicTypes.LOCAL_DATE, valor);
    }

    public static Integer comoInteiro(Object valor) {
        return valor == null ? null : ((Number) valor).intValue();
    }

    public static Long comoLongo(Object valor) {
        return valor == null ? null : ((Number) valor).longValue();
    }

    public static boolean comoBooleano(Object valor) {
        return valor instanceof Boolean b ? b : valor != null && ((Number) valor).intValue() != 0;
    }

    /** Coluna DATE lida com {@code CONVERT(CHAR(10), coluna, 23)}. */
    public static LocalDate comoData(Object valor) {
        return valor == null ? null : LocalDate.parse(valor.toString().trim());
    }

    /** Coluna DATETIMEOFFSET lida com {@code CONVERT(VARCHAR(33), coluna, 127)}. */
    public static OffsetDateTime comoInstante(Object valor) {
        return valor == null ? null : OffsetDateTime.parse(valor.toString().trim());
    }

    private Query query(Instituicao instituicao, String sql, Object... parametros) {
        EntityManager em = switch (instituicao) {
            case IDECAN -> idecan;
            case IDIB -> idib;
        };
        Query query = em.createNativeQuery(sql);
        for (int i = 0; i < parametros.length; i++) {
            query.setParameter(i + 1, parametros[i]);
        }
        return query;
    }
}

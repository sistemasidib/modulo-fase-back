package br.bioregistro.fase.infrastructure.persistence;

import br.bioregistro.fase.domain.model.CriterioDesempate;
import br.bioregistro.fase.domain.model.CriterioDesempate.Condicao;
import br.bioregistro.fase.domain.model.CriterioDesempate.Direcao;
import br.bioregistro.fase.domain.model.CriterioDesempate.Tipo;
import br.bioregistro.fase.domain.model.Instituicao;
import br.bioregistro.fase.domain.port.CriterioDesempateRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Persiste critérios de desempate por etapa ({@code db/030}). Aceita o schema novo ({@code db/027})
 * e o legado ({@code criterio}/{@code regra}/…) até a migration de colunas rodar nos bancos.
 */
@ApplicationScoped
public class CriterioDesempateRepositoryAdapter implements CriterioDesempateRepository {

    private static final String SELECT_NOVO = """
            SELECT id, etapa_id, ordem, label, tipo, direcao,
                   condicao_operador, condicao_valor, condicao_unidade,
                   data_referencia, fonte, base_legal
            FROM dbo.criterio_desempate
            """;

    private static final String SELECT_LEGADO = """
            SELECT id, etapa_id, ordem, criterio, tipo, regra, referencia,
                   fase_aplicacao, fundamentacao, observacao
            FROM dbo.criterio_desempate
            """;

    private final Sql sql;
    private final Map<Instituicao, Boolean> schemaNovo = new EnumMap<>(Instituicao.class);

    public CriterioDesempateRepositoryAdapter(Sql sql) {
        this.sql = sql;
    }

    @Override
    public List<CriterioDesempate> daEtapa(Instituicao instituicao, int etapaId) {
        if (usaSchemaNovo(instituicao)) {
            return sql.linhas(instituicao, SELECT_NOVO + " WHERE etapa_id = ?1 ORDER BY ordem, id", etapaId).stream()
                    .map(CriterioDesempateRepositoryAdapter::deNovo)
                    .toList();
        }
        return sql.linhas(instituicao, SELECT_LEGADO + " WHERE etapa_id = ?1 ORDER BY ordem, id", etapaId).stream()
                .map(CriterioDesempateRepositoryAdapter::deLegado)
                .toList();
    }

    @Override
    public List<CriterioDesempate> daFase(Instituicao instituicao, int faseId) {
        String filtro = """
                 WHERE etapa_id IN (SELECT id FROM dbo.etapa WHERE fases_id = ?1)
                 ORDER BY etapa_id, ordem, id""";
        if (usaSchemaNovo(instituicao)) {
            return sql.linhas(instituicao, SELECT_NOVO + filtro, faseId).stream()
                    .map(CriterioDesempateRepositoryAdapter::deNovo)
                    .toList();
        }
        return sql.linhas(instituicao, SELECT_LEGADO + filtro, faseId).stream()
                .map(CriterioDesempateRepositoryAdapter::deLegado)
                .toList();
    }

    @Override
    public Optional<CriterioDesempate> buscar(Instituicao instituicao, int id) {
        if (usaSchemaNovo(instituicao)) {
            return sql.linhas(instituicao, SELECT_NOVO + " WHERE id = ?1", id).stream()
                    .findFirst()
                    .map(CriterioDesempateRepositoryAdapter::deNovo);
        }
        return sql.linhas(instituicao, SELECT_LEGADO + " WHERE id = ?1", id).stream()
                .findFirst()
                .map(CriterioDesempateRepositoryAdapter::deLegado);
    }

    @Override
    public CriterioDesempate salvar(Instituicao instituicao, CriterioDesempate c) {
        return usaSchemaNovo(instituicao) ? salvarNovo(instituicao, c) : salvarLegado(instituicao, c);
    }

    @Override
    public void excluir(Instituicao instituicao, int id) {
        sql.executar(instituicao, "DELETE FROM dbo.criterio_desempate WHERE id = ?1", id);
    }

    @Override
    public void excluirDaEtapa(Instituicao instituicao, int etapaId) {
        sql.executar(instituicao, "DELETE FROM dbo.criterio_desempate WHERE etapa_id = ?1", etapaId);
    }

    @Override
    public void excluirDaFase(Instituicao instituicao, int faseId) {
        sql.executar(instituicao, """
                DELETE FROM dbo.criterio_desempate
                WHERE etapa_id IN (SELECT id FROM dbo.etapa WHERE fases_id = ?1)""", faseId);
    }

    private boolean usaSchemaNovo(Instituicao instituicao) {
        return schemaNovo.computeIfAbsent(instituicao, i -> {
            Object col = sql.valor(i, "SELECT COL_LENGTH('dbo.criterio_desempate', 'label')");
            return col != null;
        });
    }

    private CriterioDesempate salvarNovo(Instituicao instituicao, CriterioDesempate c) {
        String operador = c.condicao() == null ? null : c.condicao().operador();
        String valor = c.condicao() == null || c.condicao().valor() == null ? null
                : String.valueOf(c.condicao().valor());
        String unidade = c.condicao() == null ? null : c.condicao().unidade();
        if (c.id() == null) {
            Object id = sql.valor(instituicao, """
                    INSERT INTO dbo.criterio_desempate
                        (etapa_id, ordem, label, tipo, direcao,
                         condicao_operador, condicao_valor, condicao_unidade,
                         data_referencia, fonte, base_legal, created_at, updated_at)
                    OUTPUT INSERTED.id
                    VALUES (?1, ?2, ?3, ?4, ?5, ?6, ?7, ?8, ?9, ?10, ?11,
                            SYSDATETIMEOFFSET(), SYSDATETIMEOFFSET())""",
                    c.etapaId(), c.ordem(), c.label(), c.tipo().name(), c.direcao().name(),
                    Sql.texto(operador), Sql.texto(valor), Sql.texto(unidade),
                    Sql.texto(c.dataReferencia()), Sql.texto(c.fonte()), Sql.texto(c.baseLegal()));
            return c.comId(Sql.comoInteiro(id));
        }
        sql.executar(instituicao, """
                UPDATE dbo.criterio_desempate
                SET ordem = ?1, label = ?2, tipo = ?3, direcao = ?4,
                    condicao_operador = ?5, condicao_valor = ?6, condicao_unidade = ?7,
                    data_referencia = ?8, fonte = ?9, base_legal = ?10,
                    updated_at = SYSDATETIMEOFFSET()
                WHERE id = ?11""",
                c.ordem(), c.label(), c.tipo().name(), c.direcao().name(),
                Sql.texto(operador), Sql.texto(valor), Sql.texto(unidade),
                Sql.texto(c.dataReferencia()), Sql.texto(c.fonte()), Sql.texto(c.baseLegal()), c.id());
        return c;
    }

    private CriterioDesempate salvarLegado(Instituicao instituicao, CriterioDesempate c) {
        String tipoLegado = switch (c.tipo()) {
            case idade -> "IDADE";
            case nota -> c.fonte() != null && !"prova_objetiva".equals(c.fonte()) ? "DISCIPLINA" : "NOTA";
            case booleano -> "OUTRO";
        };
        String regra = c.direcao() == Direcao.asc ? "MENOR" : (c.tipo() == Tipo.booleano ? "SIM_NAO" : "MAIOR");
        String referencia = c.fonte();
        String fundamentacao = c.baseLegal();
        String observacao = montarObservacaoLegado(c);
        if (c.id() == null) {
            Object id = sql.valor(instituicao, """
                    INSERT INTO dbo.criterio_desempate
                        (etapa_id, ordem, criterio, tipo, regra, referencia, fase_aplicacao,
                         fundamentacao, observacao, created_at, updated_at)
                    OUTPUT INSERTED.id
                    VALUES (?1, ?2, ?3, ?4, ?5, ?6, 'RESULTADO_FINAL', ?7, ?8,
                            SYSDATETIMEOFFSET(), SYSDATETIMEOFFSET())""",
                    c.etapaId(), c.ordem(), c.label(), tipoLegado, regra,
                    Sql.texto(referencia), Sql.texto(fundamentacao), Sql.texto(observacao));
            return c.comId(Sql.comoInteiro(id));
        }
        sql.executar(instituicao, """
                UPDATE dbo.criterio_desempate
                SET ordem = ?1, criterio = ?2, tipo = ?3, regra = ?4, referencia = ?5,
                    fundamentacao = ?6, observacao = ?7, updated_at = SYSDATETIMEOFFSET()
                WHERE id = ?8""",
                c.ordem(), c.label(), tipoLegado, regra, Sql.texto(referencia),
                Sql.texto(fundamentacao), Sql.texto(observacao), c.id());
        return c;
    }

    private static String montarObservacaoLegado(CriterioDesempate c) {
        StringBuilder sb = new StringBuilder();
        if (c.dataReferencia() != null) {
            sb.append("dataReferencia=").append(c.dataReferencia());
        }
        if (c.condicao() != null) {
            if (!sb.isEmpty()) sb.append('|');
            sb.append("condicao=")
                    .append(c.condicao().operador()).append(';')
                    .append(c.condicao().valor());
            if (c.condicao().unidade() != null) {
                sb.append(';').append(c.condicao().unidade());
            }
        }
        return sb.isEmpty() ? null : sb.toString();
    }

    private static CriterioDesempate deNovo(Object[] l) {
        return new CriterioDesempate(
                Sql.comoInteiro(l[0]), Sql.comoInteiro(l[1]), Sql.comoInteiro(l[2]),
                texto(l[3]), Tipo.de(texto(l[4])), Direcao.de(texto(l[5])),
                condicao(texto(l[6]), texto(l[7]), texto(l[8])),
                texto(l[9]), texto(l[10]), texto(l[11]));
    }

    private static CriterioDesempate deLegado(Object[] l) {
        String label = texto(l[3]);
        Tipo tipo = Tipo.de(texto(l[4]));
        Direcao direcao = direcaoDeRegra(texto(l[5]));
        String fonte = texto(l[6]);
        String fundamentacao = texto(l[8]);
        String observacao = texto(l[9]);
        Condicao condicao = condicaoDeObservacao(observacao);
        String dataReferencia = dataReferenciaDeObservacao(observacao);
        String baseLegal = fundamentacao;
        if (tipo == Tipo.idade && (baseLegal == null || baseLegal.isBlank())) {
            baseLegal = "idoso";
        }
        if (tipo == Tipo.nota && (fonte == null || fonte.isBlank())) {
            fonte = "prova_objetiva";
        }
        return new CriterioDesempate(
                Sql.comoInteiro(l[0]), Sql.comoInteiro(l[1]), Sql.comoInteiro(l[2]),
                label, tipo, direcao, condicao, dataReferencia, fonte, baseLegal);
    }

    private static Direcao direcaoDeRegra(String regra) {
        if (regra != null && "MENOR".equalsIgnoreCase(regra.trim())) {
            return Direcao.asc;
        }
        return Direcao.desc;
    }

    private static Condicao condicaoDeObservacao(String observacao) {
        if (observacao == null) return null;
        for (String parte : observacao.split("\\|")) {
            if (parte.startsWith("condicao=")) {
                String[] bits = parte.substring("condicao=".length()).split(";", 3);
                if (bits.length >= 2) {
                    return new Condicao(bits[0], parseValor(bits[1]), bits.length > 2 ? bits[2] : null);
                }
            }
        }
        return null;
    }

    private static String dataReferenciaDeObservacao(String observacao) {
        if (observacao == null) return null;
        for (String parte : observacao.split("\\|")) {
            if (parte.startsWith("dataReferencia=")) {
                return parte.substring("dataReferencia=".length()).trim();
            }
        }
        if (observacao.toLowerCase().contains("último dia") || observacao.toLowerCase().contains("ultimo dia")) {
            return "ultimo_dia_inscricao";
        }
        return null;
    }

    private static Condicao condicao(String operador, String valor, String unidade) {
        if (operador == null || operador.isBlank() || valor == null || valor.isBlank()) {
            return null;
        }
        return new Condicao(operador, parseValor(valor), unidade);
    }

    private static Object parseValor(String valor) {
        if ("true".equalsIgnoreCase(valor) || "false".equalsIgnoreCase(valor)) {
            return Boolean.parseBoolean(valor);
        }
        try {
            if (valor.contains(".")) {
                return Double.parseDouble(valor);
            }
            return Integer.parseInt(valor);
        } catch (NumberFormatException e) {
            return valor;
        }
    }

    private static String texto(Object valor) {
        return valor == null ? null : valor.toString().trim();
    }
}

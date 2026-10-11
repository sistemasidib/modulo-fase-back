package br.bioregistro.fase.infrastructure.persistence;

import br.bioregistro.fase.domain.exception.ConflitoException;
import br.bioregistro.fase.domain.model.Instituicao;
import br.bioregistro.fase.domain.model.cronograma.Acao;
import br.bioregistro.fase.domain.model.cronograma.Cronograma;
import br.bioregistro.fase.domain.model.cronograma.CronogramaCalculado;
import br.bioregistro.fase.domain.model.cronograma.Edicao;
import br.bioregistro.fase.domain.model.cronograma.EtapaCalculada;
import br.bioregistro.fase.domain.model.cronograma.Parametros;
import br.bioregistro.fase.domain.model.cronograma.Particularidade;
import br.bioregistro.fase.domain.model.cronograma.Previa;
import br.bioregistro.fase.domain.model.cronograma.StatusEtapa;
import br.bioregistro.fase.domain.port.CronogramaRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Tabelas {@code cronograma*} de {@code db/004-cronograma.sql}, iguais nos dois bancos. */
@ApplicationScoped
public class CronogramaRepositoryAdapter implements CronogramaRepository {

    private static final String INSTANTE = "CONVERT(VARCHAR(33), %s, 127)";
    private static final String DATA = "CONVERT(CHAR(10), %s, 23)";

    private final Sql sql;

    public CronogramaRepositoryAdapter(Sql sql) {
        this.sql = sql;
    }

    @Override
    public Parametros parametrosPadrao(Instituicao instituicao) {
        Map<String, Integer> valores = new HashMap<>();
        sql.linhas(instituicao, "SELECT chave, valor FROM dbo.cronograma_parametro_padrao")
                .forEach(l -> {
                    if (Parametros.PADRAO.containsKey((String) l[0])) {
                        valores.put((String) l[0], Sql.comoInteiro(l[1]));
                    }
                });
        return new Parametros(valores);
    }

    @Override
    public Optional<Integer> idDoConcurso(Instituicao instituicao, int concursoId) {
        return Optional.ofNullable(Sql.comoInteiro(
                sql.valor(instituicao, "SELECT id FROM dbo.cronograma WHERE concurso_id = ?1", concursoId)));
    }

    @Override
    public int criar(Instituicao instituicao, Cronograma cronograma, CronogramaCalculado calculado) {
        Edicao edicao = cronograma.edicao();
        int id = Sql.comoInteiro(sql.valor(instituicao, """
                        INSERT INTO dbo.cronograma (concurso_id, esqueleto, data_edital, modulos, versao, created_at, updated_at)
                        OUTPUT INSERTED.id
                        VALUES (?1, ?2, ?3, ?4, 0, SYSDATETIMEOFFSET(), SYSDATETIMEOFFSET())""",
                cronograma.concursoId(), cronograma.esqueleto(), Sql.data(edicao.dataEdital()), edicao.modulosTexto()));
        cronograma.parametros().ordenados().forEach((chave, valor) -> sql.executar(instituicao,
                "INSERT INTO dbo.cronograma_parametro (cronograma_id, chave, valor, excecao) VALUES (?1, ?2, ?3, ?4)",
                id, chave, valor, edicao.excecoes().contains(chave)));
        for (EtapaCalculada etapa : calculado.etapas()) {
            Edicao.Ajuste ajuste = edicao.ajuste(etapa.codigo());
            sql.executar(instituicao, """
                            INSERT INTO dbo.cronograma_etapa
                                (cronograma_id, codigo, dias, duracao, removida, inicio_rel, fim_rel, inicio, fim)
                            VALUES (?1, ?2, ?3, ?4, ?5, ?6, ?7, ?8, ?9)""",
                    id, etapa.codigo(), Sql.inteiro(ajuste == null ? null : ajuste.dias()),
                    Sql.inteiro(ajuste == null ? null : ajuste.duracao()), etapa.removida(),
                    etapa.inicioRelativo(), etapa.fimRelativo(), Sql.data(etapa.inicio()), Sql.data(etapa.fim()));
        }
        return id;
    }

    @Override
    public Optional<Cronograma> buscar(Instituicao instituicao, int id) {
        List<Object[]> linhas = sql.linhas(instituicao,
                "SELECT concurso_id, esqueleto, " + DATA.formatted("data_edital")
                        + ", modulos, justificativa, versao FROM dbo.cronograma WHERE id = ?1", id);
        if (linhas.isEmpty()) {
            return Optional.empty();
        }
        Object[] c = linhas.get(0);

        Map<String, Integer> parametros = new HashMap<>();
        Set<String> excecoes = new HashSet<>();
        sql.linhas(instituicao, "SELECT chave, valor, excecao FROM dbo.cronograma_parametro WHERE cronograma_id = ?1", id)
                .forEach(l -> {
                    String chave = (String) l[0];
                    if (Parametros.PADRAO.containsKey(chave)) {
                        parametros.put(chave, Sql.comoInteiro(l[1]));
                        if (Sql.comoBooleano(l[2])) {
                            excecoes.add(chave);
                        }
                    }
                });

        Map<String, Edicao.Ajuste> ajustes = new HashMap<>();
        Set<String> removidas = new HashSet<>();
        Map<String, StatusEtapa> status = new HashMap<>();
        sql.linhas(instituicao, """
                        SELECT codigo, dias, duracao, removida, status_manual
                        FROM dbo.cronograma_etapa WHERE cronograma_id = ?1""", id)
                .forEach(l -> {
                    String codigo = (String) l[0];
                    Integer dias = Sql.comoInteiro(l[1]);
                    Integer duracao = Sql.comoInteiro(l[2]);
                    if (dias != null || duracao != null) {
                        ajustes.put(codigo, new Edicao.Ajuste(dias, duracao));
                    }
                    if (Sql.comoBooleano(l[3])) {
                        removidas.add(codigo);
                    }
                    StatusEtapa s = StatusEtapa.valueOf((String) l[4]);
                    if (s != StatusEtapa.A_FAZER) {
                        status.put(codigo, s);
                    }
                });

        String modulos = (String) c[3];
        Set<String> ligados = modulos == null || modulos.isBlank()
                ? Set.of() : new HashSet<>(Arrays.asList(modulos.split(",")));
        Edicao edicao = new Edicao(ligados, Sql.comoData(c[2]), ajustes, removidas, excecoes);
        return Optional.of(new Cronograma(id, Sql.comoInteiro(c[0]), (String) c[1], new Parametros(parametros),
                edicao, status, (String) c[4], Sql.comoInteiro(c[5])));
    }

    @Override
    public void salvar(Instituicao instituicao, Cronograma cronograma, CronogramaCalculado calculado) {
        Edicao edicao = cronograma.edicao();
        int alteradas = sql.executar(instituicao, """
                        UPDATE dbo.cronograma
                        SET data_edital = ?1, modulos = ?2, justificativa = ?3, versao = versao + 1,
                            updated_at = SYSDATETIMEOFFSET()
                        WHERE id = ?4 AND versao = ?5""",
                Sql.data(edicao.dataEdital()), edicao.modulosTexto(), Sql.texto(cronograma.justificativa()),
                cronograma.id(), cronograma.versao());
        if (alteradas == 0) {
            throw new ConflitoException("Outra pessoa salvou este cronograma depois que você o abriu. "
                    + "Recarregue para ver a versão atual.");
        }
        sql.executar(instituicao, "UPDATE dbo.cronograma_parametro SET excecao = 0 WHERE cronograma_id = ?1",
                cronograma.id());
        for (String chave : edicao.excecoes()) {
            sql.executar(instituicao,
                    "UPDATE dbo.cronograma_parametro SET excecao = 1 WHERE cronograma_id = ?1 AND chave = ?2",
                    cronograma.id(), chave);
        }
        for (EtapaCalculada etapa : calculado.etapas()) {
            Edicao.Ajuste ajuste = edicao.ajuste(etapa.codigo());
            sql.executar(instituicao, """
                            UPDATE dbo.cronograma_etapa
                            SET dias = ?1, duracao = ?2, removida = ?3, inicio_rel = ?4, fim_rel = ?5,
                                inicio = ?6, fim = ?7
                            WHERE cronograma_id = ?8 AND codigo = ?9""",
                    Sql.inteiro(ajuste == null ? null : ajuste.dias()),
                    Sql.inteiro(ajuste == null ? null : ajuste.duracao()), etapa.removida(),
                    etapa.inicioRelativo(), etapa.fimRelativo(), Sql.data(etapa.inicio()), Sql.data(etapa.fim()),
                    cronograma.id(), etapa.codigo());
        }
    }

    @Override
    public void mudarStatus(Instituicao instituicao, int cronogramaId, String codigo, StatusEtapa status) {
        sql.executar(instituicao,
                "UPDATE dbo.cronograma_etapa SET status_manual = ?1 WHERE cronograma_id = ?2 AND codigo = ?3",
                status.name(), cronogramaId, codigo);
    }

    @Override
    public void registrar(Instituicao instituicao, int cronogramaId, Acao acao) {
        long id = Sql.comoLongo(sql.valor(instituicao, """
                        INSERT INTO dbo.cronograma_acao (cronograma_id, autor, criado_em, acao, detalhe, origem, justificativa)
                        OUTPUT INSERTED.id
                        VALUES (?1, ?2, SYSDATETIMEOFFSET(), ?3, ?4, ?5, ?6)""",
                cronogramaId, acao.autor(), acao.acao(), Sql.texto(acao.detalhe()), acao.origem(),
                Sql.texto(acao.justificativa())));
        for (Previa.Item item : acao.itens()) {
            sql.executar(instituicao, """
                            INSERT INTO dbo.cronograma_acao_item (acao_id, etapa_codigo, campo, antes, depois)
                            VALUES (?1, ?2, ?3, ?4, ?5)""",
                    id, Sql.texto(item.etapaCodigo()), item.campo(), Sql.texto(item.antes()), Sql.texto(item.depois()));
        }
    }

    @Override
    public List<Acao> historico(Instituicao instituicao, int cronogramaId) {
        Map<Long, List<Previa.Item>> itens = new HashMap<>();
        sql.linhas(instituicao, """
                        SELECT i.acao_id, i.etapa_codigo, i.campo, i.antes, i.depois
                        FROM dbo.cronograma_acao_item i JOIN dbo.cronograma_acao a ON a.id = i.acao_id
                        WHERE a.cronograma_id = ?1 ORDER BY i.id""", cronogramaId)
                .forEach(l -> itens.computeIfAbsent(Sql.comoLongo(l[0]), k -> new ArrayList<>())
                        .add(new Previa.Item((String) l[1], (String) l[2], (String) l[3], (String) l[4])));

        Map<Long, Acao> acoes = new LinkedHashMap<>();
        sql.linhas(instituicao, "SELECT id, autor, " + INSTANTE.formatted("criado_em")
                        + ", acao, detalhe, origem, justificativa FROM dbo.cronograma_acao"
                        + " WHERE cronograma_id = ?1 ORDER BY id DESC", cronogramaId)
                .forEach(l -> {
                    long id = Sql.comoLongo(l[0]);
                    acoes.put(id, new Acao(id, (String) l[1], Sql.comoInstante(l[2]), (String) l[3], (String) l[4],
                            (String) l[5], (String) l[6], itens.getOrDefault(id, List.of())));
                });
        return List.copyOf(acoes.values());
    }

    @Override
    public List<Particularidade> particularidades(Instituicao instituicao, int cronogramaId) {
        return sql.linhas(instituicao, "SELECT id, texto, assunto, etapa_codigo, autor, "
                        + INSTANTE.formatted("criado_em")
                        + " FROM dbo.cronograma_particularidade WHERE cronograma_id = ?1 ORDER BY id", cronogramaId)
                .stream()
                .map(CronogramaRepositoryAdapter::particularidade)
                .toList();
    }

    @Override
    public Particularidade adicionarParticularidade(Instituicao instituicao, int cronogramaId, Particularidade p) {
        int id = Sql.comoInteiro(sql.valor(instituicao, """
                        INSERT INTO dbo.cronograma_particularidade (cronograma_id, texto, assunto, etapa_codigo, autor, criado_em)
                        OUTPUT INSERTED.id
                        VALUES (?1, ?2, ?3, ?4, ?5, SYSDATETIMEOFFSET())""",
                cronogramaId, p.texto(), p.assunto(), Sql.texto(p.etapaCodigo()), p.autor()));
        return particularidade(instituicao, cronogramaId, id).orElseThrow();
    }

    @Override
    public Optional<Particularidade> particularidade(Instituicao instituicao, int cronogramaId, int id) {
        return sql.linhas(instituicao, "SELECT id, texto, assunto, etapa_codigo, autor, "
                        + INSTANTE.formatted("criado_em")
                        + " FROM dbo.cronograma_particularidade WHERE cronograma_id = ?1 AND id = ?2", cronogramaId, id)
                .stream()
                .findFirst()
                .map(CronogramaRepositoryAdapter::particularidade);
    }

    @Override
    public void removerParticularidade(Instituicao instituicao, int cronogramaId, int id) {
        sql.executar(instituicao, "DELETE FROM dbo.cronograma_particularidade WHERE cronograma_id = ?1 AND id = ?2",
                cronogramaId, id);
    }

    private static Particularidade particularidade(Object[] l) {
        return new Particularidade(Sql.comoInteiro(l[0]), (String) l[1], (String) l[2], (String) l[3], (String) l[4],
                Sql.comoInstante(l[5]));
    }
}

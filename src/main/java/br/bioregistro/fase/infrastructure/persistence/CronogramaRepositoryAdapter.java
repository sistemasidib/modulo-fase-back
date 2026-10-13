package br.bioregistro.fase.infrastructure.persistence;

import br.bioregistro.fase.domain.exception.ConflitoException;
import br.bioregistro.fase.domain.model.Instituicao;
import br.bioregistro.fase.domain.model.cronograma.Acao;
import br.bioregistro.fase.domain.model.cronograma.Cronograma;
import br.bioregistro.fase.domain.model.cronograma.CronogramaCalculado;
import br.bioregistro.fase.domain.model.cronograma.Edicao;
import br.bioregistro.fase.domain.model.cronograma.EditalDoCronograma;
import br.bioregistro.fase.domain.model.cronograma.EtapaCalculada;
import br.bioregistro.fase.domain.model.cronograma.ItemPersonalizado;
import br.bioregistro.fase.domain.model.cronograma.Parametros;
import br.bioregistro.fase.domain.model.cronograma.Particularidade;
import br.bioregistro.fase.domain.model.cronograma.Previa;
import br.bioregistro.fase.domain.model.cronograma.Referencia;
import br.bioregistro.fase.domain.model.cronograma.Sobreposicao;
import br.bioregistro.fase.domain.model.cronograma.StatusEtapa;
import br.bioregistro.fase.domain.model.cronograma.TipoStatus;
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
import java.util.TreeSet;

/**
 * Tabelas {@code cronograma*} de {@code db/004}, {@code db/012} e {@code db/015}, iguais nos dois
 * bancos.
 */
@ApplicationScoped
public class CronogramaRepositoryAdapter implements CronogramaRepository {

    private static final String INSTANTE = "CONVERT(VARCHAR(33), %s, 127)";
    private static final String DATA = "CONVERT(CHAR(10), %s, 23)";
    private static final String CONFLITO = "Outra pessoa salvou este cronograma depois que você o abriu. "
            + "Recarregue para ver a versão atual.";

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
        return Optional.ofNullable(Sql.comoInteiro(sql.valor(instituicao,
                "SELECT id FROM dbo.cronograma WHERE concurso_id = ?1", concursoId)));
    }

    @Override
    public Map<Integer, Integer> cronogramasDosEditais(Instituicao instituicao, int concursoId) {
        Map<Integer, Integer> ids = new LinkedHashMap<>();
        sql.linhas(instituicao, """
                        SELECT ce.edital_id, x.cronograma_id
                        FROM dbo.concurso_edital ce
                        JOIN dbo.cronograma_edital x ON x.edital_id = ce.edital_id
                        WHERE ce.concurso_id = ?1 ORDER BY ce.edital_id""", concursoId)
                .forEach(l -> ids.put(Sql.comoInteiro(l[0]), Sql.comoInteiro(l[1])));
        return ids;
    }

    @Override
    public void soltarDoConcurso(Instituicao instituicao, int concursoId) {
        sql.executar(instituicao, "UPDATE dbo.cronograma SET concurso_id = NULL WHERE concurso_id = ?1", concursoId);
    }

    @Override
    public int criar(Instituicao instituicao, Cronograma cronograma, CronogramaCalculado calculado) {
        Edicao edicao = cronograma.edicao();
        int id = Sql.comoInteiro(sql.valor(instituicao, """
                        INSERT INTO dbo.cronograma
                            (concurso_id, esqueleto, data_edital, data_prova, modulos, justificativa, versao,
                             created_at, updated_at)
                        OUTPUT INSERTED.id
                        VALUES (?1, ?2, ?3, ?4, ?5, ?6, 0, SYSDATETIMEOFFSET(), SYSDATETIMEOFFSET())""",
                Sql.inteiro(cronograma.concursoId()), cronograma.esqueleto(), Sql.data(edicao.dataEdital()),
                Sql.data(edicao.dataProva()), edicao.modulosTexto(), Sql.texto(cronograma.justificativa())));
        cronograma.parametros().ordenados().forEach((chave, valor) -> sql.executar(instituicao,
                "INSERT INTO dbo.cronograma_parametro (cronograma_id, chave, valor, excecao) VALUES (?1, ?2, ?3, ?4)",
                id, chave, valor, edicao.excecoes().contains(chave)));
        for (EtapaCalculada etapa : calculado.etapas()) {
            if (etapa.personalizada()) {
                gravarItem(instituicao, id, edicao.item(etapa.codigo()).orElseThrow(), etapa);
            } else {
                Edicao.Ajuste ajuste = edicao.ajuste(etapa.codigo());
                sql.executar(instituicao, """
                                INSERT INTO dbo.cronograma_etapa
                                    (cronograma_id, codigo, dias, duracao, removida, inicio_rel, fim_rel, inicio, fim)
                                VALUES (?1, ?2, ?3, ?4, ?5, ?6, ?7, ?8, ?9)""",
                        id, etapa.codigo(), Sql.inteiro(ajuste == null ? null : ajuste.dias()),
                        Sql.inteiro(ajuste == null ? null : ajuste.duracao()), etapa.removida(),
                        etapa.inicioRelativo(), etapa.fimRelativo(), Sql.data(etapa.inicio()), Sql.data(etapa.fim()));
            }
            StatusEtapa status = cronograma.status(etapa.codigo());
            if (status != StatusEtapa.A_FAZER) {
                mudarStatus(instituicao, id, etapa.codigo(), status);
            }
        }
        return id;
    }

    @Override
    public Optional<Cronograma> buscar(Instituicao instituicao, int id) {
        List<Object[]> linhas = sql.linhas(instituicao,
                "SELECT concurso_id, esqueleto, " + DATA.formatted("data_edital")
                        + ", modulos, justificativa, versao, " + DATA.formatted("data_prova")
                        + " FROM dbo.cronograma WHERE id = ?1", id);
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
        List<ItemPersonalizado> itens = new ArrayList<>();
        sql.linhas(instituicao, """
                        SELECT codigo, dias, duracao, removida, status_manual, personalizada, descricao, grupo,
                            referencia_tipo, referencia_codigo, tipo_status
                        FROM dbo.cronograma_etapa WHERE cronograma_id = ?1 ORDER BY codigo""", id)
                .forEach(l -> {
                    String codigo = (String) l[0];
                    Integer dias = Sql.comoInteiro(l[1]);
                    Integer duracao = Sql.comoInteiro(l[2]);
                    if (Sql.comoBooleano(l[5])) {
                        itens.add(new ItemPersonalizado(codigo, (String) l[6], (String) l[7],
                                new Referencia(Referencia.Tipo.valueOf((String) l[8]), (String) l[9]),
                                dias, duracao, TipoStatus.valueOf((String) l[10])));
                    } else if (dias != null || duracao != null) {
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

        Edicao edicao = new Edicao(modulos((String) c[3]), Sql.comoData(c[2]), ajustes, removidas, excecoes, itens,
                Sql.comoData(c[6]));
        return Optional.of(new Cronograma(id, Sql.comoInteiro(c[0]), (String) c[1], new Parametros(parametros),
                edicao, status, (String) c[4], Sql.comoInteiro(c[5])));
    }

    @Override
    public void salvar(Instituicao instituicao, Cronograma cronograma, CronogramaCalculado calculado) {
        Edicao edicao = cronograma.edicao();
        int alteradas = sql.executar(instituicao, """
                        UPDATE dbo.cronograma
                        SET data_edital = ?1, data_prova = ?2, modulos = ?3, justificativa = ?4, versao = versao + 1,
                            updated_at = SYSDATETIMEOFFSET()
                        WHERE id = ?5 AND versao = ?6""",
                Sql.data(edicao.dataEdital()), Sql.data(edicao.dataProva()), edicao.modulosTexto(),
                Sql.texto(cronograma.justificativa()), cronograma.id(), cronograma.versao());
        if (alteradas == 0) {
            throw new ConflitoException(CONFLITO);
        }
        sql.executar(instituicao, "UPDATE dbo.cronograma_parametro SET excecao = 0 WHERE cronograma_id = ?1",
                cronograma.id());
        for (String chave : edicao.excecoes()) {
            sql.executar(instituicao,
                    "UPDATE dbo.cronograma_parametro SET excecao = 1 WHERE cronograma_id = ?1 AND chave = ?2",
                    cronograma.id(), chave);
        }
        Set<String> itensAtuais = new HashSet<>();
        edicao.itens().forEach(i -> itensAtuais.add(i.codigo()));
        sql.linhas(instituicao,
                        "SELECT codigo FROM dbo.cronograma_etapa WHERE cronograma_id = ?1 AND personalizada = 1",
                        cronograma.id()).stream()
                .map(l -> (String) l[0])
                .filter(codigo -> !itensAtuais.contains(codigo))
                .forEach(codigo -> sql.executar(instituicao,
                        "DELETE FROM dbo.cronograma_etapa WHERE cronograma_id = ?1 AND codigo = ?2 AND personalizada = 1",
                        cronograma.id(), codigo));
        for (EtapaCalculada etapa : calculado.etapas()) {
            if (etapa.personalizada()) {
                gravarItem(instituicao, cronograma.id(), edicao.item(etapa.codigo()).orElseThrow(), etapa);
                continue;
            }
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

    private void gravarItem(Instituicao instituicao, int cronogramaId, ItemPersonalizado item, EtapaCalculada etapa) {
        Referencia ref = item.referencia();
        int alteradas = sql.executar(instituicao, """
                        UPDATE dbo.cronograma_etapa
                        SET dias = ?1, duracao = ?2, descricao = ?3, grupo = ?4, referencia_tipo = ?5,
                            referencia_codigo = ?6, tipo_status = ?7, inicio_rel = ?8, fim_rel = ?9,
                            inicio = ?10, fim = ?11
                        WHERE cronograma_id = ?12 AND codigo = ?13 AND personalizada = 1""",
                item.dias(), item.duracao(), item.descricao(), item.grupo(), ref.tipo().name(),
                Sql.texto(ref.codigo()), item.tipoStatus().name(), etapa.inicioRelativo(), etapa.fimRelativo(),
                Sql.data(etapa.inicio()), Sql.data(etapa.fim()), cronogramaId, item.codigo());
        if (alteradas == 0) {
            sql.executar(instituicao, """
                            INSERT INTO dbo.cronograma_etapa
                                (cronograma_id, codigo, dias, duracao, removida, personalizada, descricao, grupo,
                                 referencia_tipo, referencia_codigo, tipo_status, inicio_rel, fim_rel, inicio, fim)
                            VALUES (?1, ?2, ?3, ?4, 0, 1, ?5, ?6, ?7, ?8, ?9, ?10, ?11, ?12, ?13)""",
                    cronogramaId, item.codigo(), item.dias(), item.duracao(), item.descricao(), item.grupo(),
                    ref.tipo().name(), Sql.texto(ref.codigo()), item.tipoStatus().name(), etapa.inicioRelativo(),
                    etapa.fimRelativo(), Sql.data(etapa.inicio()), Sql.data(etapa.fim()));
        }
    }

    @Override
    public void mudarStatus(Instituicao instituicao, int cronogramaId, String codigo, StatusEtapa status) {
        sql.executar(instituicao,
                "UPDATE dbo.cronograma_etapa SET status_manual = ?1 WHERE cronograma_id = ?2 AND codigo = ?3",
                status.name(), cronogramaId, codigo);
    }

    // ── editais que seguem o cronograma ───────────────────────────────────────

    @Override
    public Optional<EditalDoCronograma> doEdital(Instituicao instituicao, int editalId) {
        return vinculos(instituicao, "x.edital_id = ?1", editalId).stream().findFirst();
    }

    @Override
    public List<EditalDoCronograma> editais(Instituicao instituicao, int cronogramaId) {
        return vinculos(instituicao, "x.cronograma_id = ?1", cronogramaId);
    }

    private List<EditalDoCronograma> vinculos(Instituicao instituicao, String filtro, int valor) {
        List<EditalDoCronograma> encontrados = new ArrayList<>();
        for (Object[] l : sql.linhas(instituicao, "SELECT x.id, x.cronograma_id, x.edital_id, "
                + DATA.formatted("x.data_edital") + ", " + DATA.formatted("x.data_prova")
                + ", x.modulos, x.versao FROM dbo.cronograma_edital x WHERE " + filtro + " ORDER BY x.edital_id", valor)) {
            int id = Sql.comoInteiro(l[0]);
            Map<String, Edicao.Ajuste> ajustes = new HashMap<>();
            Map<String, Boolean> removidas = new HashMap<>();
            Map<String, StatusEtapa> status = new HashMap<>();
            sql.linhas(instituicao, """
                            SELECT codigo, ajustado, dias, duracao, removida, status
                            FROM dbo.cronograma_edital_etapa
                            WHERE cronograma_edital_id = ?1
                              AND (ajustado = 1 OR removida IS NOT NULL OR status IS NOT NULL)""", id)
                    .forEach(e -> {
                        String codigo = (String) e[0];
                        if (Sql.comoBooleano(e[1])) {
                            ajustes.put(codigo, new Edicao.Ajuste(Sql.comoInteiro(e[2]), Sql.comoInteiro(e[3])));
                        }
                        if (e[4] != null) {
                            removidas.put(codigo, Sql.comoBooleano(e[4]));
                        }
                        if (e[5] != null) {
                            status.put(codigo, StatusEtapa.valueOf((String) e[5]));
                        }
                    });
            String modulos = (String) l[5];
            Sobreposicao sobreposicao = new Sobreposicao(Sql.comoData(l[3]), Sql.comoData(l[4]),
                    modulos == null ? null : modulos(modulos), ajustes, removidas, status);
            encontrados.add(new EditalDoCronograma(id, Sql.comoInteiro(l[1]), Sql.comoInteiro(l[2]), sobreposicao,
                    Sql.comoInteiro(l[6])));
        }
        return encontrados;
    }

    @Override
    public EditalDoCronograma ligarEdital(Instituicao instituicao, int cronogramaId, int editalId) {
        Optional<EditalDoCronograma> atual = doEdital(instituicao, editalId);
        if (atual.isEmpty()) {
            sql.executar(instituicao, """
                            INSERT INTO dbo.cronograma_edital (cronograma_id, edital_id, versao, created_at, updated_at)
                            VALUES (?1, ?2, 0, SYSDATETIMEOFFSET(), SYSDATETIMEOFFSET())""",
                    cronogramaId, editalId);
        } else {
            sql.executar(instituicao, """
                            UPDATE dbo.cronograma_edital
                            SET cronograma_id = ?1, data_edital = NULL, data_prova = NULL, modulos = NULL,
                                versao = versao + 1, updated_at = SYSDATETIMEOFFSET()
                            WHERE id = ?2""",
                    cronogramaId, atual.get().id());
            limparSobreposicao(instituicao, atual.get().id());
        }
        return doEdital(instituicao, editalId).orElseThrow();
    }

    @Override
    public void salvarSobreposicao(Instituicao instituicao, EditalDoCronograma edital) {
        Sobreposicao s = edital.sobreposicao();
        int alteradas = sql.executar(instituicao, """
                        UPDATE dbo.cronograma_edital
                        SET data_edital = ?1, data_prova = ?2, modulos = ?3, versao = versao + 1,
                            updated_at = SYSDATETIMEOFFSET()
                        WHERE id = ?4 AND versao = ?5""",
                Sql.data(s.dataEdital()), Sql.data(s.dataProva()),
                Sql.texto(s.modulos() == null ? null : String.join(",", new TreeSet<>(s.modulos()))),
                edital.id(), edital.versao());
        if (alteradas == 0) {
            throw new ConflitoException(CONFLITO);
        }
        limparSobreposicao(instituicao, edital.id());
        Set<String> codigos = new HashSet<>(s.ajustes().keySet());
        codigos.addAll(s.removidas().keySet());
        codigos.addAll(s.status().keySet());
        Map<String, Integer> linhas = linhasDoEdital(instituicao, edital.id());
        for (String codigo : codigos) {
            Edicao.Ajuste ajuste = s.ajustes().get(codigo);
            Boolean removida = s.removidas().get(codigo);
            StatusEtapa status = s.status().get(codigo);
            Object[] valores = {ajuste != null, Sql.inteiro(ajuste == null ? null : ajuste.dias()),
                    Sql.inteiro(ajuste == null ? null : ajuste.duracao()),
                    Sql.booleano(removida),
                    Sql.texto(status == null ? null : status.name())};
            if (linhas.containsKey(codigo)) {
                sql.executar(instituicao, """
                                UPDATE dbo.cronograma_edital_etapa
                                SET ajustado = ?1, dias = ?2, duracao = ?3, removida = ?4, status = ?5
                                WHERE id = ?6""",
                        valores[0], valores[1], valores[2], valores[3], valores[4], linhas.get(codigo));
            } else {
                sql.executar(instituicao, """
                                INSERT INTO dbo.cronograma_edital_etapa
                                    (cronograma_edital_id, codigo, ajustado, dias, duracao, removida, status)
                                VALUES (?1, ?2, ?3, ?4, ?5, ?6, ?7)""",
                        edital.id(), codigo, valores[0], valores[1], valores[2], valores[3], valores[4]);
            }
        }
    }

    private void limparSobreposicao(Instituicao instituicao, int editalDoCronogramaId) {
        sql.executar(instituicao, """
                        UPDATE dbo.cronograma_edital_etapa
                        SET ajustado = 0, dias = NULL, duracao = NULL, removida = NULL, status = NULL
                        WHERE cronograma_edital_id = ?1""",
                editalDoCronogramaId);
    }

    @Override
    public void materializar(Instituicao instituicao, EditalDoCronograma edital, CronogramaCalculado calculado,
                             Map<String, StatusEtapa> status) {
        Map<String, Integer> linhas = linhasDoEdital(instituicao, edital.id());
        Set<String> atuais = new HashSet<>();
        for (EtapaCalculada etapa : calculado.etapas()) {
            atuais.add(etapa.codigo());
            String efetivo = status.getOrDefault(etapa.codigo(), StatusEtapa.A_FAZER).name();
            Integer linha = linhas.get(etapa.codigo());
            if (linha != null) {
                sql.executar(instituicao, """
                                UPDATE dbo.cronograma_edital_etapa
                                SET ativa = ?1, inicio = ?2, fim = ?3, status_efetivo = ?4 WHERE id = ?5""",
                        etapa.ativa(), Sql.data(etapa.inicio()), Sql.data(etapa.fim()), efetivo, linha);
            } else {
                sql.executar(instituicao, """
                                INSERT INTO dbo.cronograma_edital_etapa
                                    (cronograma_edital_id, codigo, ativa, inicio, fim, status_efetivo)
                                VALUES (?1, ?2, ?3, ?4, ?5, ?6)""",
                        edital.id(), etapa.codigo(), etapa.ativa(), Sql.data(etapa.inicio()), Sql.data(etapa.fim()),
                        efetivo);
            }
        }
        linhas.forEach((codigo, linha) -> {
            if (!atuais.contains(codigo)) {
                sql.executar(instituicao, """
                                DELETE FROM dbo.cronograma_edital_etapa
                                WHERE id = ?1 AND NOT EXISTS (SELECT 1 FROM dbo.publicacao p
                                    WHERE p.cronograma_etapa_id = ?1 OR p.cronograma_recurso_id = ?1)""",
                        linha);
            }
        });
    }

    @Override
    public Map<String, Integer> linhasDoEdital(Instituicao instituicao, int editalDoCronogramaId) {
        Map<String, Integer> linhas = new HashMap<>();
        sql.linhas(instituicao,
                        "SELECT codigo, id FROM dbo.cronograma_edital_etapa WHERE cronograma_edital_id = ?1",
                        editalDoCronogramaId)
                .forEach(l -> linhas.put((String) l[0], Sql.comoInteiro(l[1])));
        return linhas;
    }

    // ── histórico e particularidades ──────────────────────────────────────────

    @Override
    public void registrar(Instituicao instituicao, int cronogramaId, Acao acao) {
        long id = Sql.comoLongo(sql.valor(instituicao, """
                        INSERT INTO dbo.cronograma_acao
                            (cronograma_id, autor, criado_em, acao, detalhe, origem, justificativa, edital_id)
                        OUTPUT INSERTED.id
                        VALUES (?1, ?2, SYSDATETIMEOFFSET(), ?3, ?4, ?5, ?6, ?7)""",
                cronogramaId, acao.autor(), acao.acao(), Sql.texto(acao.detalhe()), acao.origem(),
                Sql.texto(acao.justificativa()), Sql.inteiro(acao.editalId())));
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
                        + ", acao, detalhe, origem, justificativa, edital_id FROM dbo.cronograma_acao"
                        + " WHERE cronograma_id = ?1 ORDER BY id DESC", cronogramaId)
                .forEach(l -> {
                    long id = Sql.comoLongo(l[0]);
                    acoes.put(id, new Acao(id, (String) l[1], Sql.comoInstante(l[2]), (String) l[3], (String) l[4],
                            (String) l[5], (String) l[6], itens.getOrDefault(id, List.of()), Sql.comoInteiro(l[7])));
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

    private static Set<String> modulos(String texto) {
        return texto == null || texto.isBlank() ? Set.of() : new HashSet<>(Arrays.asList(texto.split(",")));
    }

    private static Particularidade particularidade(Object[] l) {
        return new Particularidade(Sql.comoInteiro(l[0]), (String) l[1], (String) l[2], (String) l[3], (String) l[4],
                Sql.comoInstante(l[5]));
    }
}

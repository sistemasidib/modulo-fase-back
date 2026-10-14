/*
 * Especificação 005 — o cronograma é o único dono das datas; a fase é do edital.
 *
 * Rodar o script inteiro em CADA banco: IDECAN_v2 e IDIB_v2 (sqlcmd -I -f 65001: índices
 * filtrados exigem QUOTED_IDENTIFIER ON; arquivo em UTF-8). É idempotente.
 *
 * dbo.cronograma
 *   + data_prova        data da prova objetiva informada (NULL = sugestão provisória); nos
 *                       cronogramas existentes, preenchida com a última data calculada da E15
 *   concurso_id         passa a aceitar NULL: cronograma separado de um edital, ou de concurso excluído
 *   edital_id (db/013)  sai: quem diz que edital segue qual cronograma é dbo.cronograma_edital
 *
 * dbo.cronograma_edital        edital que segue um cronograma (no máximo um por edital), com a
 *                              sobreposição do edital (NULL = herda do compartilhado)
 * dbo.cronograma_edital_etapa  uma linha por etapa por edital:
 *                                sobreposição  ajustado/dias/duracao, removida, status
 *                                leitura       ativa, inicio, fim, status_efetivo (gravadas pelo
 *                                              modulo-fase a cada salvamento; outros módulos leem daqui)
 * dbo.fase_cronograma          fase do edital ligada a um módulo do cronograma (NUCLEO, S12, S14...)
 * dbo.publicacao
 *   + cronograma_etapa_id      FK da linha de leitura da publicação (datas e status vêm dela)
 *   + cronograma_recurso_id    FK da linha do recurso contra a publicação
 * dbo.fase_grupo_cargos        UNIQUE(fases_id) → UNIQUE(fases_id, grupo_cargos_id): vários grupos por fase
 * dbo.cronograma_acao
 *   + edital_id                ação na sobreposição de um edital (NULL = no compartilhado)
 *
 * Conversão dos dados da db/013 (nada é apagado):
 *   - cronograma próprio de edital → vira cronograma separado (sem concurso) que o edital segue;
 *   - cronograma compartilhado → todos os editais do concurso que ainda não seguem nenhum passam a segui-lo.
 * As linhas de leitura nascem no próximo salvamento ou em POST /editais/{id}/cronograma/sincronizar.
 */

SET XACT_ABORT ON;
BEGIN TRANSACTION;

/* ------------------------------------------------------------------ */
/* dbo.cronograma                                                      */
/* ------------------------------------------------------------------ */
IF COL_LENGTH('dbo.cronograma', 'data_prova') IS NULL
    ALTER TABLE dbo.cronograma ADD data_prova DATE NULL;

/* Cronogramas já com data do edital mantêm a prova onde está: a última data calculada da E15 (que
   ia ao domingo) vira a data informada. Sem isso, a prova andaria até 6 dias para trás. */
EXEC(N'
    UPDATE c SET c.data_prova = e.inicio
    FROM dbo.cronograma c
    JOIN dbo.cronograma_etapa e ON e.cronograma_id = c.id AND e.codigo = N''E15''
    WHERE c.data_prova IS NULL AND c.data_edital IS NOT NULL AND e.inicio IS NOT NULL
      AND e.inicio > c.data_edital;');

/* ------------------------------------------------------------------ */
/* dbo.cronograma_edital e dbo.cronograma_edital_etapa                 */
/* ------------------------------------------------------------------ */
IF OBJECT_ID('dbo.cronograma_edital', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.cronograma_edital (
        id            INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_cronograma_edital PRIMARY KEY,
        cronograma_id INT            NOT NULL CONSTRAINT FK_cronograma_edital_cronograma REFERENCES dbo.cronograma (id),
        edital_id     INT            NOT NULL CONSTRAINT FK_cronograma_edital_edital REFERENCES dbo.Edital (ediId),
        data_edital   DATE           NULL,
        data_prova    DATE           NULL,
        modulos       NVARCHAR(100)  NULL,
        versao        INT            NOT NULL CONSTRAINT DF_cronograma_edital_versao DEFAULT (0),
        created_at    DATETIMEOFFSET NOT NULL,
        updated_at    DATETIMEOFFSET NOT NULL,
        CONSTRAINT UQ_cronograma_edital_edital UNIQUE (edital_id)
    );
    CREATE INDEX IX_cronograma_edital_cronograma ON dbo.cronograma_edital (cronograma_id);
END;

IF OBJECT_ID('dbo.cronograma_edital_etapa', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.cronograma_edital_etapa (
        id                   INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_cronograma_edital_etapa PRIMARY KEY,
        cronograma_edital_id INT           NOT NULL
            CONSTRAINT FK_cronograma_edital_etapa_edital REFERENCES dbo.cronograma_edital (id),
        codigo               NVARCHAR(10)  NOT NULL,
        ajustado             BIT           NOT NULL CONSTRAINT DF_cronograma_edital_etapa_ajustado DEFAULT (0),
        dias                 INT           NULL,
        duracao              INT           NULL,
        removida             BIT           NULL,
        status               NVARCHAR(30)  NULL,
        ativa                BIT           NOT NULL CONSTRAINT DF_cronograma_edital_etapa_ativa DEFAULT (0),
        inicio               DATE          NULL,
        fim                  DATE          NULL,
        status_efetivo       NVARCHAR(30)  NOT NULL CONSTRAINT DF_cronograma_edital_etapa_status DEFAULT (N'A_FAZER'),
        CONSTRAINT UQ_cronograma_edital_etapa_codigo UNIQUE (cronograma_edital_id, codigo)
    );
END;

/* ------------------------------------------------------------------ */
/* Histórico por edital                                                */
/* ------------------------------------------------------------------ */
IF COL_LENGTH('dbo.cronograma_acao', 'edital_id') IS NULL
    ALTER TABLE dbo.cronograma_acao ADD edital_id INT NULL;

/* ------------------------------------------------------------------ */
/* Conversão da db/013 (que precisa ter rodado antes)                  */
/* Os índices sobre concurso_id saem antes do ALTER COLUMN e voltam    */
/* no fim: o SQL Server não muda a nulidade de coluna indexada.        */
/* Depois da db/017 (concurso_id já saiu), este bloco não faz nada.    */
/* ------------------------------------------------------------------ */
IF COL_LENGTH('dbo.cronograma', 'concurso_id') IS NOT NULL
BEGIN
    IF EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID('dbo.cronograma')
               AND name = 'UX_cronograma_concurso_compartilhado')
        DROP INDEX UX_cronograma_concurso_compartilhado ON dbo.cronograma;
    IF EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID('dbo.cronograma')
               AND name = 'UX_cronograma_concurso')
        DROP INDEX UX_cronograma_concurso ON dbo.cronograma;
    IF EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID('dbo.cronograma')
               AND name = 'IX_cronograma_concurso')
        DROP INDEX IX_cronograma_concurso ON dbo.cronograma;

    EXEC(N'ALTER TABLE dbo.cronograma ALTER COLUMN concurso_id INT NULL;');

    IF COL_LENGTH('dbo.cronograma', 'edital_id') IS NOT NULL
    BEGIN
        EXEC(N'
            INSERT INTO dbo.cronograma_edital (cronograma_id, edital_id, versao, created_at, updated_at)
            SELECT c.id, c.edital_id, 0, SYSDATETIMEOFFSET(), SYSDATETIMEOFFSET()
            FROM dbo.cronograma c
            WHERE c.edital_id IS NOT NULL
              AND NOT EXISTS (SELECT 1 FROM dbo.cronograma_edital x WHERE x.edital_id = c.edital_id);

            UPDATE dbo.cronograma SET concurso_id = NULL, edital_id = NULL WHERE edital_id IS NOT NULL;');

        IF EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID('dbo.cronograma')
                   AND name = 'UX_cronograma_edital')
            DROP INDEX UX_cronograma_edital ON dbo.cronograma;
        IF OBJECT_ID('dbo.FK_cronograma_edital', 'F') IS NOT NULL
            ALTER TABLE dbo.cronograma DROP CONSTRAINT FK_cronograma_edital;
        ALTER TABLE dbo.cronograma DROP COLUMN edital_id;
    END;

    /* No máximo um cronograma aberto por concurso. */
    EXEC(N'CREATE UNIQUE INDEX UX_cronograma_concurso ON dbo.cronograma (concurso_id)
        WHERE concurso_id IS NOT NULL');

    EXEC(N'
        INSERT INTO dbo.cronograma_edital (cronograma_id, edital_id, versao, created_at, updated_at)
        SELECT c.id, ce.edital_id, 0, SYSDATETIMEOFFSET(), SYSDATETIMEOFFSET()
        FROM dbo.cronograma c
        JOIN dbo.concurso_edital ce ON ce.concurso_id = c.concurso_id
        WHERE NOT EXISTS (SELECT 1 FROM dbo.cronograma_edital x WHERE x.edital_id = ce.edital_id);');
END;

/* ------------------------------------------------------------------ */
/* Fase do edital: vários grupos por fase e vínculo com o cronograma   */
/* ------------------------------------------------------------------ */
IF OBJECT_ID('dbo.UQ_fase_grupo_cargos_fase', 'UQ') IS NOT NULL
    ALTER TABLE dbo.fase_grupo_cargos DROP CONSTRAINT UQ_fase_grupo_cargos_fase;

IF OBJECT_ID('dbo.UQ_fase_grupo_cargos_fase_grupo', 'UQ') IS NULL
    ALTER TABLE dbo.fase_grupo_cargos
        ADD CONSTRAINT UQ_fase_grupo_cargos_fase_grupo UNIQUE (fases_id, grupo_cargos_id);

IF OBJECT_ID('dbo.fase_cronograma', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.fase_cronograma (
        fases_id             INT          NOT NULL CONSTRAINT PK_fase_cronograma PRIMARY KEY
            CONSTRAINT FK_fase_cronograma_fase REFERENCES dbo.fases (id),
        cronograma_edital_id INT          NOT NULL
            CONSTRAINT FK_fase_cronograma_edital REFERENCES dbo.cronograma_edital (id),
        modulo               NVARCHAR(20) NOT NULL,
        CONSTRAINT UQ_fase_cronograma_modulo UNIQUE (cronograma_edital_id, modulo)
    );
END;

/* ------------------------------------------------------------------ */
/* Publicação aponta para a linha do cronograma                        */
/* ------------------------------------------------------------------ */
IF COL_LENGTH('dbo.publicacao', 'cronograma_etapa_id') IS NULL
BEGIN
    ALTER TABLE dbo.publicacao ADD
        cronograma_etapa_id   INT NULL CONSTRAINT FK_publicacao_cronograma_etapa
            REFERENCES dbo.cronograma_edital_etapa (id),
        cronograma_recurso_id INT NULL CONSTRAINT FK_publicacao_cronograma_recurso
            REFERENCES dbo.cronograma_edital_etapa (id);
END;

COMMIT TRANSACTION;

SELECT 'cronograma_edital' AS tabela, COUNT(*) AS linhas FROM dbo.cronograma_edital;
IF COL_LENGTH('dbo.cronograma', 'concurso_id') IS NOT NULL
    EXEC(N'SELECT ''cronograma sem concurso'' AS tabela, COUNT(*) AS linhas
           FROM dbo.cronograma WHERE concurso_id IS NULL;');

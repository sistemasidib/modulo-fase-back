/*
 * Concurso vira entidade: o órgão responsável pelos editais (ex.: uma prefeitura). Uma entidade
 * pode ter vários concursos ao mesmo tempo, cada um com o seu cronograma, então o cronograma deixa
 * de pertencer ao agrupador: nasce do edital e outros editais podem segui-lo (dbo.cronograma_edital).
 *
 * Rodar DEPOIS da db/015 e da db/016, em CADA banco: IDECAN_v2 e IDIB_v2 (sqlcmd -I -f 65001:
 * índice filtrado exige QUOTED_IDENTIFIER ON; arquivo em UTF-8). É idempotente.
 *
 * dbo.concurso                → dbo.entidade         + cnpj NVARCHAR(14) NULL (único quando preenchido;
 *                                                      sem pontuação, aceita o CNPJ alfanumérico)
 * dbo.concurso_edital         → dbo.entidade_edital  concurso_id → entidade_id
 * dbo.cronograma.concurso_id  sai. Os editais que seguiam o cronograma do concurso continuam
 *                             seguindo (dbo.cronograma_edital); nada mais é apagado.
 *
 * Não confundir com dbo.concurso do banco ENSALAMENTO, que é outra tabela e não muda.
 */

SET XACT_ABORT ON;
BEGIN TRANSACTION;

IF OBJECT_ID('dbo.cronograma_edital', 'U') IS NULL
    THROW 50017, N'Rode a db/015 antes desta.', 1;

/* ------------------------------------------------------------------ */
/* dbo.concurso → dbo.entidade                                         */
/* ------------------------------------------------------------------ */
IF OBJECT_ID('dbo.entidade', 'U') IS NULL AND OBJECT_ID('dbo.concurso', 'U') IS NOT NULL
BEGIN
    EXEC sp_rename 'dbo.concurso', 'entidade';
    IF OBJECT_ID('dbo.PK_concurso', 'PK') IS NOT NULL
        EXEC sp_rename 'dbo.PK_concurso', 'PK_entidade', 'OBJECT';
    IF OBJECT_ID('dbo.UQ_concurso_nome', 'UQ') IS NOT NULL
        EXEC sp_rename 'dbo.UQ_concurso_nome', 'UQ_entidade_nome', 'OBJECT';
END;

IF OBJECT_ID('dbo.entidade', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.entidade (
        id         INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_entidade PRIMARY KEY,
        nome       NVARCHAR(200)     NOT NULL CONSTRAINT UQ_entidade_nome UNIQUE,
        created_at DATETIMEOFFSET    NOT NULL,
        updated_at DATETIMEOFFSET    NOT NULL
    );
END;

IF COL_LENGTH('dbo.entidade', 'cnpj') IS NULL
    ALTER TABLE dbo.entidade ADD cnpj NVARCHAR(14) NULL;

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID('dbo.entidade') AND name = 'UX_entidade_cnpj')
    EXEC(N'CREATE UNIQUE INDEX UX_entidade_cnpj ON dbo.entidade (cnpj) WHERE cnpj IS NOT NULL');

/* ------------------------------------------------------------------ */
/* dbo.concurso_edital → dbo.entidade_edital                           */
/* ------------------------------------------------------------------ */
IF OBJECT_ID('dbo.entidade_edital', 'U') IS NULL AND OBJECT_ID('dbo.concurso_edital', 'U') IS NOT NULL
BEGIN
    EXEC sp_rename 'dbo.concurso_edital', 'entidade_edital';
    EXEC sp_rename 'dbo.entidade_edital.concurso_id', 'entidade_id', 'COLUMN';
    IF OBJECT_ID('dbo.PK_concurso_edital', 'PK') IS NOT NULL
        EXEC sp_rename 'dbo.PK_concurso_edital', 'PK_entidade_edital', 'OBJECT';
    IF OBJECT_ID('dbo.UQ_concurso_edital_edital', 'UQ') IS NOT NULL
        EXEC sp_rename 'dbo.UQ_concurso_edital_edital', 'UQ_entidade_edital_edital', 'OBJECT';
    IF OBJECT_ID('dbo.FK_concurso_edital_concurso', 'F') IS NOT NULL
        EXEC sp_rename 'dbo.FK_concurso_edital_concurso', 'FK_entidade_edital_entidade', 'OBJECT';
    IF OBJECT_ID('dbo.FK_concurso_edital_edital', 'F') IS NOT NULL
        EXEC sp_rename 'dbo.FK_concurso_edital_edital', 'FK_entidade_edital_edital', 'OBJECT';
    IF EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID('dbo.entidade_edital')
               AND name = 'IX_concurso_edital_concurso')
        EXEC sp_rename 'dbo.entidade_edital.IX_concurso_edital_concurso', 'IX_entidade_edital_entidade', 'INDEX';
END;

IF OBJECT_ID('dbo.entidade_edital', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.entidade_edital (
        id          INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_entidade_edital PRIMARY KEY,
        entidade_id INT NOT NULL CONSTRAINT FK_entidade_edital_entidade REFERENCES dbo.entidade (id),
        edital_id   INT NOT NULL CONSTRAINT FK_entidade_edital_edital REFERENCES dbo.Edital (ediId),
        CONSTRAINT UQ_entidade_edital_edital UNIQUE (edital_id)
    );
    CREATE INDEX IX_entidade_edital_entidade ON dbo.entidade_edital (entidade_id);
END;

/* ------------------------------------------------------------------ */
/* dbo.cronograma sem dono                                             */
/* ------------------------------------------------------------------ */
IF COL_LENGTH('dbo.cronograma', 'concurso_id') IS NOT NULL
BEGIN
    /* O que valia antes: edital do concurso sem cronograma seguia o do concurso. */
    EXEC(N'
        INSERT INTO dbo.cronograma_edital (cronograma_id, edital_id, versao, created_at, updated_at)
        SELECT c.id, ee.edital_id, 0, SYSDATETIMEOFFSET(), SYSDATETIMEOFFSET()
        FROM dbo.cronograma c
        JOIN dbo.entidade_edital ee ON ee.entidade_id = c.concurso_id
        WHERE NOT EXISTS (SELECT 1 FROM dbo.cronograma_edital x WHERE x.edital_id = ee.edital_id);');

    IF EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID('dbo.cronograma') AND name = 'UX_cronograma_concurso')
        DROP INDEX UX_cronograma_concurso ON dbo.cronograma;
    IF EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID('dbo.cronograma')
               AND name = 'UX_cronograma_concurso_compartilhado')
        DROP INDEX UX_cronograma_concurso_compartilhado ON dbo.cronograma;
    IF EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID('dbo.cronograma') AND name = 'IX_cronograma_concurso')
        DROP INDEX IX_cronograma_concurso ON dbo.cronograma;
    IF OBJECT_ID('dbo.UQ_cronograma_concurso', 'UQ') IS NOT NULL
        ALTER TABLE dbo.cronograma DROP CONSTRAINT UQ_cronograma_concurso;

    DECLARE @fk SYSNAME = (
        SELECT TOP 1 fk.name
        FROM sys.foreign_keys fk
        JOIN sys.foreign_key_columns fc ON fc.constraint_object_id = fk.object_id
        JOIN sys.columns col ON col.object_id = fc.parent_object_id AND col.column_id = fc.parent_column_id
        WHERE fk.parent_object_id = OBJECT_ID('dbo.cronograma') AND col.name = 'concurso_id');
    IF @fk IS NOT NULL
    BEGIN
        DECLARE @drop NVARCHAR(400) = N'ALTER TABLE dbo.cronograma DROP CONSTRAINT ' + QUOTENAME(@fk);
        EXEC(@drop);
    END;

    EXEC(N'ALTER TABLE dbo.cronograma DROP COLUMN concurso_id;');
END;

COMMIT TRANSACTION;

SELECT 'entidade' AS tabela, COUNT(*) AS linhas FROM dbo.entidade
UNION ALL SELECT 'entidade_edital', COUNT(*) FROM dbo.entidade_edital
UNION ALL SELECT 'cronograma_edital', COUNT(*) FROM dbo.cronograma_edital
UNION ALL SELECT 'cronograma sem edital', COUNT(*) FROM dbo.cronograma c
    WHERE NOT EXISTS (SELECT 1 FROM dbo.cronograma_edital x WHERE x.cronograma_id = c.id);

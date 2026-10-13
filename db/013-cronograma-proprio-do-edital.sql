/*
 * Cronograma próprio do edital: editais do mesmo concurso podem ter cronogramas diferentes.
 *
 * Rodar o script inteiro em CADA banco: IDECAN_v2 e IDIB_v2 (sqlcmd -I: índices filtrados
 * exigem QUOTED_IDENTIFIER ON). É idempotente e não altera dados existentes.
 *
 * dbo.cronograma.edital_id
 *   NULL      cronograma compartilhado do concurso: vale para os editais sem cronograma próprio
 *             (no máximo um por concurso)
 *   preenchido cronograma próprio daquele edital, criado como cópia do compartilhado (no máximo
 *             um por edital); concurso_id continua sendo o concurso do edital
 */

SET XACT_ABORT ON;
BEGIN TRANSACTION;

IF COL_LENGTH('dbo.cronograma', 'edital_id') IS NULL
BEGIN
    ALTER TABLE dbo.cronograma ADD
        edital_id INT NULL CONSTRAINT FK_cronograma_edital REFERENCES dbo.Edital (ediId);
END;

IF OBJECT_ID('dbo.UQ_cronograma_concurso', 'UQ') IS NOT NULL
BEGIN
    ALTER TABLE dbo.cronograma DROP CONSTRAINT UQ_cronograma_concurso;
END;

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID('dbo.cronograma')
               AND name = 'IX_cronograma_concurso')
BEGIN
    CREATE INDEX IX_cronograma_concurso ON dbo.cronograma (concurso_id);
END;

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID('dbo.cronograma')
               AND name = 'UX_cronograma_concurso_compartilhado')
BEGIN
    EXEC(N'CREATE UNIQUE INDEX UX_cronograma_concurso_compartilhado ON dbo.cronograma (concurso_id)
        WHERE edital_id IS NULL');
END;

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID('dbo.cronograma')
               AND name = 'UX_cronograma_edital')
BEGIN
    EXEC(N'CREATE UNIQUE INDEX UX_cronograma_edital ON dbo.cronograma (edital_id)
        WHERE edital_id IS NOT NULL');
END;

COMMIT TRANSACTION;

SELECT i.name, i.is_unique, i.filter_definition
FROM sys.indexes i
WHERE i.object_id = OBJECT_ID('dbo.cronograma') AND i.name IS NOT NULL
ORDER BY i.name;

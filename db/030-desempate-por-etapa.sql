/*
 * Critérios de desempate passam a pertencer à etapa (não mais à fase).
 * Cada etapa tem a própria lista; não há herança entre etapas.
 *
 * Backfill: critérios existentes vão para a etapa Resultado Final da fase;
 * se a fase não tiver RF, vão para a primeira etapa. Sem etapa → descartados.
 *
 * Rodar em CADA banco: IDECAN_v2 e IDIB_v2 (sqlcmd -f 65001). Idempotente.
 */

SET XACT_ABORT ON;
BEGIN TRANSACTION;

IF OBJECT_ID('dbo.criterio_desempate', 'U') IS NOT NULL
   AND COL_LENGTH('dbo.criterio_desempate', 'etapa_id') IS NULL
BEGIN
    ALTER TABLE dbo.criterio_desempate ADD etapa_id INT NULL;

    /* Preferência: Resultado Final da fase; senão a primeira etapa. */
    IF COL_LENGTH('dbo.criterio_desempate', 'fase_id') IS NOT NULL
    EXEC('
        UPDATE c
        SET c.etapa_id = e.id
        FROM dbo.criterio_desempate c
        CROSS APPLY (
            SELECT TOP 1 et.id
            FROM dbo.etapa et
            INNER JOIN dbo.dom_tipo_etapa t ON t.id = et.tipo_id
            WHERE et.fases_id = c.fase_id
            ORDER BY CASE WHEN t.valor = N''resultado_final'' THEN 0 ELSE 1 END, et.id
        ) e
        WHERE c.etapa_id IS NULL AND c.fase_id IS NOT NULL;

        DELETE FROM dbo.criterio_desempate WHERE etapa_id IS NULL;
    ');

    IF EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_criterio_desempate_fase'
               AND object_id = OBJECT_ID('dbo.criterio_desempate'))
        DROP INDEX IX_criterio_desempate_fase ON dbo.criterio_desempate;

    IF OBJECT_ID('dbo.FK_criterio_desempate_fase', 'F') IS NOT NULL
        ALTER TABLE dbo.criterio_desempate DROP CONSTRAINT FK_criterio_desempate_fase;

    IF COL_LENGTH('dbo.criterio_desempate', 'fase_id') IS NOT NULL
        ALTER TABLE dbo.criterio_desempate DROP COLUMN fase_id;

    EXEC('ALTER TABLE dbo.criterio_desempate ALTER COLUMN etapa_id INT NOT NULL');
    EXEC('ALTER TABLE dbo.criterio_desempate
              ADD CONSTRAINT FK_criterio_desempate_etapa FOREIGN KEY (etapa_id) REFERENCES dbo.etapa (id)');
    EXEC('CREATE INDEX IX_criterio_desempate_etapa ON dbo.criterio_desempate (etapa_id, ordem)');
END;

COMMIT TRANSACTION;

SELECT name AS coluna FROM sys.columns
WHERE object_id = OBJECT_ID('dbo.criterio_desempate') AND name IN ('etapa_id', 'fase_id')
ORDER BY name;

EXEC('SELECT etapa_id, COUNT(*) AS criterios FROM dbo.criterio_desempate GROUP BY etapa_id ORDER BY etapa_id');

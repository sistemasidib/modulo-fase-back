/*
 * RN09 — cláusula de barreira por fase.
 * Rodar em CADA banco: IDECAN_v2 e IDIB_v2. Idempotente.
 */

SET XACT_ABORT ON;
BEGIN TRANSACTION;

IF OBJECT_ID('dbo.fases', 'U') IS NOT NULL
   AND COL_LENGTH('dbo.fases', 'clausula_barreira') IS NULL
BEGIN
    ALTER TABLE dbo.fases ADD clausula_barreira BIT NOT NULL
        CONSTRAINT DF_fases_clausula_barreira DEFAULT (0);
END

COMMIT TRANSACTION;

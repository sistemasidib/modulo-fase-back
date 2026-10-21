/*
 * Spec 006 — modalidade Remota no S05 (hetero/biopsico).
 * ONLINE permanece aceito (alias legado); o domínio grava REMOTA.
 * Aplicar nos bancos IDECAN e IDIB. Idempotente.
 */

IF OBJECT_ID('dbo.CHK_etapa_modalidade', 'C') IS NOT NULL
    ALTER TABLE dbo.etapa DROP CONSTRAINT CHK_etapa_modalidade;
GO

ALTER TABLE dbo.etapa ADD CONSTRAINT CHK_etapa_modalidade
    CHECK (modalidade IS NULL OR modalidade IN (N'ONLINE', N'PRESENCIAL', N'REMOTA'));
GO

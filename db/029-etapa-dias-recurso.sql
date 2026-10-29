/*
 * RN10 — duração do recurso: padrão por tipo de etapa + override na etapa.
 * Rodar em CADA banco: IDECAN_v2 e IDIB_v2. Idempotente.
 */

SET XACT_ABORT ON;
BEGIN TRANSACTION;

IF OBJECT_ID('dbo.dom_tipo_etapa', 'U') IS NOT NULL
   AND COL_LENGTH('dbo.dom_tipo_etapa', 'dias_recurso_padrao') IS NULL
BEGIN
    ALTER TABLE dbo.dom_tipo_etapa ADD dias_recurso_padrao INT NULL;
END

IF OBJECT_ID('dbo.etapa', 'U') IS NOT NULL
   AND COL_LENGTH('dbo.etapa', 'dias_recurso') IS NULL
BEGIN
    ALTER TABLE dbo.etapa ADD dias_recurso INT NULL;
END

COMMIT TRANSACTION;
GO

/* Seed: duração padrão 2 dias para tipos de execução que abrem recurso (comportamento atual). */
UPDATE dbo.dom_tipo_etapa
SET dias_recurso_padrao = 2
WHERE valor IN (
    'prova_objetiva', 'prova_discursiva', 'taf', 'the',
    'heteroidentificacao', 'biopsicossocial', 'prova_titulos'
)
  AND dias_recurso_padrao IS NULL;

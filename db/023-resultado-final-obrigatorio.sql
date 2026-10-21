/*
 * Etapa "Resultado Final" obrigatória em toda fase (não aparece nas opções de fase do grupo).
 * Rodar em CADA banco: IDECAN_V2 e IDIB_V2. Idempotente.
 */
SET XACT_ABORT ON;
BEGIN TRANSACTION;

INSERT INTO dbo.dom_tipo_etapa (valor, descricao)
SELECT N'resultado', N'Resultado Final'
WHERE NOT EXISTS (SELECT 1 FROM dbo.dom_tipo_etapa WHERE valor = N'resultado');

UPDATE dbo.dom_tipo_etapa
SET descricao = N'Resultado Final'
WHERE valor = N'resultado'
  AND (descricao IS NULL OR descricao <> N'Resultado Final');

/* Espaços padrão (opcional): a etapa estrutural pode existir sem publicação própria. */
INSERT INTO dbo.dom_tipo_etapa_publicacao (tipo_etapa_id, tipo_publicacao_id)
SELECT te.id, tp.id
FROM dbo.dom_tipo_etapa te
JOIN dbo.dom_tipo_publicacao tp ON tp.valor IN (N'resultado_preliminar', N'resultado_oficial')
WHERE te.valor = N'resultado'
  AND NOT EXISTS (
        SELECT 1 FROM dbo.dom_tipo_etapa_publicacao x
        WHERE x.tipo_etapa_id = te.id AND x.tipo_publicacao_id = tp.id);

COMMIT TRANSACTION;

/*
 * Spec 006 P-08 / T024 — seed de config_publicacao_padrao.
 * Referência = valor de dom_tipo_etapa (não código E* do esqueleto); NULL = própria etapa.
 * Valores iniciais ajustáveis via PUT /editais/{id}/config-publicacoes/{tipoId}.
 * Offsets Karen Bloco 1 (docs/validacao-karen-cronograma.md); updates em bancos já
 * seedados: db/026. Inscrição 30d / isenção 2d → db/025.
 *
 * Rodar em CADA banco: IDECAN_v2 e IDIB_v2 (sqlcmd -I -f 65001). Idempotente.
 * Requer db/001 (dom_tipo_publicacao, dom_tipo_etapa) e db/018 (tabela).
 */

SET XACT_ABORT ON;
GO

/* Referência passa a ser valor de tipo de etapa (ex. prova_objetiva), não código E*. */
IF COL_LENGTH('dbo.config_publicacao_padrao', 'referencia_codigo') IS NOT NULL
    ALTER TABLE dbo.config_publicacao_padrao ALTER COLUMN referencia_codigo NVARCHAR(40) NULL;
GO

IF COL_LENGTH('dbo.config_publicacao_edital', 'referencia_codigo') IS NOT NULL
    ALTER TABLE dbo.config_publicacao_edital ALTER COLUMN referencia_codigo NVARCHAR(40) NULL;
GO

/* local_prova → 4 dias antes da prova (norma 3–4), 7 dias no ar */
MERGE dbo.config_publicacao_padrao AS t
USING (
    SELECT tp.id AS tipo_publicacao_id,
           CAST(N'prova_objetiva' AS NVARCHAR(40)) AS referencia_codigo,
           CAST(-4 AS INT) AS dias_apos_referencia,
           CAST(7 AS INT) AS dias_no_ar,
           CAST(N'TABELA_LINK' AS NVARCHAR(20)) AS formato
    FROM dbo.dom_tipo_publicacao tp
    WHERE tp.valor = N'local_prova'
) AS s ON t.tipo_publicacao_id = s.tipo_publicacao_id
WHEN NOT MATCHED THEN
    INSERT (tipo_publicacao_id, referencia_codigo, dias_apos_referencia, dias_no_ar, formato)
    VALUES (s.tipo_publicacao_id, s.referencia_codigo, s.dias_apos_referencia, s.dias_no_ar, s.formato);
GO

/* link_acesso → própria etapa S05 */
MERGE dbo.config_publicacao_padrao AS t
USING (
    SELECT tp.id AS tipo_publicacao_id,
           CAST(NULL AS NVARCHAR(40)) AS referencia_codigo,
           CAST(0 AS INT) AS dias_apos_referencia,
           CAST(7 AS INT) AS dias_no_ar,
           CAST(N'TABELA_LINK' AS NVARCHAR(20)) AS formato
    FROM dbo.dom_tipo_publicacao tp
    WHERE tp.valor = N'link_acesso'
) AS s ON t.tipo_publicacao_id = s.tipo_publicacao_id
WHEN NOT MATCHED THEN
    INSERT (tipo_publicacao_id, referencia_codigo, dias_apos_referencia, dias_no_ar, formato)
    VALUES (s.tipo_publicacao_id, s.referencia_codigo, s.dias_apos_referencia, s.dias_no_ar, s.formato);
GO

/* gabarito_preliminar → +7 dias (norma 1–2 semanas), 14 dias no ar */
MERGE dbo.config_publicacao_padrao AS t
USING (
    SELECT tp.id AS tipo_publicacao_id,
           CAST(N'prova_objetiva' AS NVARCHAR(40)) AS referencia_codigo,
           CAST(7 AS INT) AS dias_apos_referencia,
           CAST(14 AS INT) AS dias_no_ar,
           CAST(N'LISTA_PDF' AS NVARCHAR(20)) AS formato
    FROM dbo.dom_tipo_publicacao tp
    WHERE tp.valor = N'gabarito_preliminar'
) AS s ON t.tipo_publicacao_id = s.tipo_publicacao_id
WHEN NOT MATCHED THEN
    INSERT (tipo_publicacao_id, referencia_codigo, dias_apos_referencia, dias_no_ar, formato)
    VALUES (s.tipo_publicacao_id, s.referencia_codigo, s.dias_apos_referencia, s.dias_no_ar, s.formato);
GO

/* gabarito_definitivo → +12 dias após prova (~7 + recurso 5), 14 dias no ar */
MERGE dbo.config_publicacao_padrao AS t
USING (
    SELECT tp.id AS tipo_publicacao_id,
           CAST(N'prova_objetiva' AS NVARCHAR(40)) AS referencia_codigo,
           CAST(12 AS INT) AS dias_apos_referencia,
           CAST(14 AS INT) AS dias_no_ar,
           CAST(N'LISTA_PDF' AS NVARCHAR(20)) AS formato
    FROM dbo.dom_tipo_publicacao tp
    WHERE tp.valor = N'gabarito_definitivo'
) AS s ON t.tipo_publicacao_id = s.tipo_publicacao_id
WHEN NOT MATCHED THEN
    INSERT (tipo_publicacao_id, referencia_codigo, dias_apos_referencia, dias_no_ar, formato)
    VALUES (s.tipo_publicacao_id, s.referencia_codigo, s.dias_apos_referencia, s.dias_no_ar, s.formato);
GO

/* resultado_preliminar → própria etapa, no dia do fechamento, 5 dias no ar */
MERGE dbo.config_publicacao_padrao AS t
USING (
    SELECT tp.id AS tipo_publicacao_id,
           CAST(NULL AS NVARCHAR(40)) AS referencia_codigo,
           CAST(0 AS INT) AS dias_apos_referencia,
           CAST(5 AS INT) AS dias_no_ar,
           CAST(N'LISTA_PDF' AS NVARCHAR(20)) AS formato
    FROM dbo.dom_tipo_publicacao tp
    WHERE tp.valor = N'resultado_preliminar'
) AS s ON t.tipo_publicacao_id = s.tipo_publicacao_id
WHEN NOT MATCHED THEN
    INSERT (tipo_publicacao_id, referencia_codigo, dias_apos_referencia, dias_no_ar, formato)
    VALUES (s.tipo_publicacao_id, s.referencia_codigo, s.dias_apos_referencia, s.dias_no_ar, s.formato);
GO

/* resultado_oficial → própria etapa, +5 dias (faixa 2–7 do recurso), 30 dias no ar */
MERGE dbo.config_publicacao_padrao AS t
USING (
    SELECT tp.id AS tipo_publicacao_id,
           CAST(NULL AS NVARCHAR(40)) AS referencia_codigo,
           CAST(5 AS INT) AS dias_apos_referencia,
           CAST(30 AS INT) AS dias_no_ar,
           CAST(N'LISTA_PDF' AS NVARCHAR(20)) AS formato
    FROM dbo.dom_tipo_publicacao tp
    WHERE tp.valor = N'resultado_oficial'
) AS s ON t.tipo_publicacao_id = s.tipo_publicacao_id
WHEN NOT MATCHED THEN
    INSERT (tipo_publicacao_id, referencia_codigo, dias_apos_referencia, dias_no_ar, formato)
    VALUES (s.tipo_publicacao_id, s.referencia_codigo, s.dias_apos_referencia, s.dias_no_ar, s.formato);
GO

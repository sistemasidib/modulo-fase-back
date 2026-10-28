/*
 * Karen Bloco 1 (27/10/2026): gabarito 1–2 semanas → default 7;
 * recurso 2–7 → default 5; locais 3–4 dias antes → -4.
 * Atualiza cronograma_parametro_padrao e config_publicacao_padrao (seed 024).
 *
 * Rodar em CADA banco: IDECAN_v2 e IDIB_v2 (sqlcmd -I -f 65001). Idempotente.
 */

SET XACT_ABORT ON;
BEGIN TRANSACTION;

UPDATE p SET p.valor = v.valor
FROM dbo.cronograma_parametro_padrao p
JOIN (VALUES
    (N'gabarito_apos_prova', 7),
    (N'recurso_gabarito',    5),
    (N'recurso_demais',      5)
) AS v (chave, valor) ON v.chave = p.chave;

UPDATE c SET
    c.referencia_codigo    = s.referencia_codigo,
    c.dias_apos_referencia = s.dias_apos_referencia,
    c.dias_no_ar           = s.dias_no_ar,
    c.formato              = s.formato
FROM dbo.config_publicacao_padrao c
JOIN dbo.dom_tipo_publicacao tp ON tp.id = c.tipo_publicacao_id
JOIN (VALUES
    (N'local_prova',          N'prova_objetiva', -4,  7, N'TABELA_LINK'),
    (N'gabarito_preliminar',  N'prova_objetiva',  7, 14, N'LISTA_PDF'),
    (N'gabarito_definitivo',  N'prova_objetiva', 12, 14, N'LISTA_PDF'),
    (N'resultado_preliminar', NULL,               0,  5, N'LISTA_PDF'),
    (N'resultado_oficial',    NULL,               5, 30, N'LISTA_PDF')
) AS s (valor, referencia_codigo, dias_apos_referencia, dias_no_ar, formato)
  ON s.valor = tp.valor;

COMMIT TRANSACTION;

SELECT chave, valor FROM dbo.cronograma_parametro_padrao
WHERE chave IN (N'gabarito_apos_prova', N'recurso_gabarito', N'recurso_demais')
ORDER BY chave;

SELECT tp.valor, c.referencia_codigo, c.dias_apos_referencia, c.dias_no_ar, c.formato
FROM dbo.config_publicacao_padrao c
JOIN dbo.dom_tipo_publicacao tp ON tp.id = c.tipo_publicacao_id
ORDER BY tp.valor;

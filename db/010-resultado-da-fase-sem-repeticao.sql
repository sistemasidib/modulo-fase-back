/*
 * Resultado da fase sem repetir o da etapa.
 *
 * Rodar o script inteiro em CADA banco: IDECAN_v2 e IDIB_v2 (sqlcmd -f 65001: arquivo em UTF-8).
 * É idempotente e só mexe em dados.
 *
 * Quando uma única etapa da fase publica resultado, o resultado dela é o da fase. Os espaços
 * próprios da fase (etapa_id nulo) de resultado preliminar e oficial criados antes dessa regra
 * são removidos, desde que ainda vazios (sem datas, não publicados, sem documento nem recurso).
 */

SET XACT_ABORT ON;
BEGIN TRANSACTION;

WITH fases_de_uma_etapa AS (
    SELECT p.fases_id
    FROM dbo.publicacao p
    INNER JOIN dbo.dom_tipo_publicacao tp ON tp.id = p.tipo_publicacao_id
    WHERE p.etapa_id IS NOT NULL
      AND tp.valor IN (N'resultado_preliminar', N'resultado_oficial')
    GROUP BY p.fases_id
    HAVING COUNT(DISTINCT p.etapa_id) = 1
)
DELETE p
FROM dbo.publicacao p
INNER JOIN dbo.dom_tipo_publicacao tp ON tp.id = p.tipo_publicacao_id
INNER JOIN fases_de_uma_etapa f ON f.fases_id = p.fases_id
WHERE p.etapa_id IS NULL
  AND tp.valor IN (N'resultado_preliminar', N'resultado_oficial')
  AND p.data_prevista IS NULL
  AND p.visualizacao_inicio IS NULL AND p.visualizacao_fim IS NULL
  AND p.recurso_inicio IS NULL AND p.recurso_fim IS NULL
  AND p.publicada_em IS NULL
  AND p.documento_arquivo_id IS NULL
  AND NOT EXISTS (SELECT 1 FROM dbo.Recurso r WHERE r.publicacao_id = p.id);

SELECT @@ROWCOUNT AS espacos_removidos;

COMMIT TRANSACTION;

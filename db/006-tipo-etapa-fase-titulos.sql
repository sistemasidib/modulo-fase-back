/*
 * Tipo de etapa "Fase de títulos" (prova_titulos): fase de execução, como prova objetiva e TAF.
 * Não confundir com analise_prova_titulos, que é análise de solicitações e não é fase.
 *
 * Rodar o script inteiro em CADA banco: IDECAN_v2 e IDIB_v2.
 * É idempotente: pode rodar de novo sem duplicar linhas.
 * Requer db/001-fases-e-etapas.sql (dom_tipo_etapa, dom_tipo_publicacao, dom_tipo_etapa_publicacao).
 * O backend precisa da versão com "prova_titulos" em TipoEtapa.EXECUCAO para oferecê-la como fase.
 */

SET XACT_ABORT ON;
BEGIN TRANSACTION;

IF NOT EXISTS (SELECT 1 FROM dbo.dom_tipo_etapa WHERE valor = N'prova_titulos')
    INSERT INTO dbo.dom_tipo_etapa (valor, descricao) VALUES (N'prova_titulos', N'Fase de títulos');

/* Espaços criados com cada etapa do tipo: resultado preliminar (abre recurso) e oficial. */
INSERT INTO dbo.dom_tipo_etapa_publicacao (tipo_etapa_id, tipo_publicacao_id)
SELECT te.id, tp.id
FROM dbo.dom_tipo_etapa te
JOIN dbo.dom_tipo_publicacao tp ON tp.valor IN (N'resultado_preliminar', N'resultado_oficial')
WHERE te.valor = N'prova_titulos'
  AND NOT EXISTS (
        SELECT 1 FROM dbo.dom_tipo_etapa_publicacao x
        WHERE x.tipo_etapa_id = te.id AND x.tipo_publicacao_id = tp.id);

COMMIT TRANSACTION;

/* Conferência */
SELECT te.id, te.valor, te.descricao, COUNT(tep.id) AS publicacoes
FROM dbo.dom_tipo_etapa te
LEFT JOIN dbo.dom_tipo_etapa_publicacao tep ON tep.tipo_etapa_id = te.id
WHERE te.valor = N'prova_titulos'
GROUP BY te.id, te.valor, te.descricao;

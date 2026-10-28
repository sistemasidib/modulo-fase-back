/*
 * Validação Karen (27/10/2026): inscrição padrão 30 dias; isenção 2 dias.
 * Impugnação (2 dias) no Anexo II é item de data absoluta — sem parâmetro dedicado no motor.
 *
 * Rodar em CADA banco: IDECAN_v2 e IDIB_v2 (sqlcmd -I -f 65001). Idempotente.
 * Só altera cronograma_parametro_padrao; cronogramas já criados mantêm a cópia própria.
 */

SET XACT_ABORT ON;
BEGIN TRANSACTION;

UPDATE p SET p.valor = v.valor, p.descricao = COALESCE(v.descricao, p.descricao)
FROM dbo.cronograma_parametro_padrao p
JOIN (VALUES
    (N'duracao_inscricoes', 30, N'Duração das inscrições (padrão operacional Karen / 27-10-2026)'),
    (N'isencao',            2,  N'Pedido de isenção da taxa (duração; padrão 2 dias)')
) AS v (chave, valor, descricao) ON v.chave = p.chave;

COMMIT TRANSACTION;

SELECT chave, valor, descricao FROM dbo.cronograma_parametro_padrao
WHERE chave IN (N'duracao_inscricoes', N'isencao')
ORDER BY chave;

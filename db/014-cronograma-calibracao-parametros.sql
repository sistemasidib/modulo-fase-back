/*
 * Recalibra os valores padrão do cronograma (RN17) com 127 editais IDECAN de 2018–2026 que têm
 * gabarito preliminar publicado (dbo.Documento), medidos em out/2026:
 *
 * duracao_inscricoes   31 → 41   mediana 2025–2026 (P25–P75 34–49)
 * intervalo_prova      43 → 34   fim das inscrições → prova: mediana 34 (P25–P75 27–40)
 * gabarito_apos_prova  15 → 1    gabarito no dia seguinte à prova em 96 de 127 editais; máximo 7
 * rm01_minimo          45 → 30   com 45, mais de 75% dos concursos reais violariam a RM-01
 *
 * Só vale para cronogramas criados depois: os existentes guardam a própria cópia dos parâmetros.
 * Acompanha a mudança no código em que a E14 (locais de prova) passa a contar 9 dias antes da
 * prova (início de E15 − 9) em vez de fim de E04 + 35; por isso o script para se algum cronograma
 * tiver ajuste de dias na E14, que mudaria de sentido.
 *
 * Rodar o script inteiro em CADA banco: IDECAN_v2 e IDIB_v2 (sqlcmd -f 65001: arquivo em UTF-8).
 * É idempotente.
 */

SET XACT_ABORT ON;
BEGIN TRANSACTION;

IF EXISTS (SELECT 1 FROM dbo.cronograma_etapa WHERE codigo = N'E14' AND dias IS NOT NULL AND ISNULL(personalizada, 0) = 0)
    THROW 50014, N'Há cronograma com ajuste de dias na E14: revise antes de aplicar (a referência mudou para a prova).', 1;

UPDATE p SET p.valor = v.valor
FROM dbo.cronograma_parametro_padrao p
JOIN (VALUES
    (N'duracao_inscricoes',  41),
    (N'intervalo_prova',     34),
    (N'gabarito_apos_prova', 1),
    (N'rm01_minimo',         30)
) AS v (chave, valor) ON v.chave = p.chave;

COMMIT TRANSACTION;

SELECT chave, valor FROM dbo.cronograma_parametro_padrao ORDER BY chave;

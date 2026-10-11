/*
 * Etapa sem data: dbo.etapa.data_abertura passa a aceitar NULL.
 *
 * CA10 da especificação 001: a etapa nasce sem data e só a recebe depois, no cadastro do
 * edital. A geração por grupo (POST /grupos-cargo/{id}/gerar-fases) cria etapas sem data, e
 * com a coluna NOT NULL o INSERT falhava.
 *
 * Rodar em CADA banco: IDECAN_v2 e IDIB_v2. É idempotente: se a coluna já aceita NULL,
 * não faz nada.
 *
 * O índice idx_etapa_abertura e o CHECK CHK_etapa_datas dependem da coluna e são recriados
 * com a mesma definição. Com data_abertura NULL o CHECK continua passando, porque a
 * comparação resulta em UNKNOWN.
 *
 * O modulo-dados (fluxo antigo) já trata data_abertura nula ao ler etapas.
 */

SET XACT_ABORT ON;
BEGIN TRANSACTION;

IF EXISTS (SELECT 1 FROM sys.columns
           WHERE object_id = OBJECT_ID('dbo.etapa') AND name = 'data_abertura' AND is_nullable = 0)
BEGIN
    IF EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID('dbo.etapa') AND name = 'idx_etapa_abertura')
        DROP INDEX idx_etapa_abertura ON dbo.etapa;

    IF OBJECT_ID('dbo.CHK_etapa_datas', 'C') IS NOT NULL
        ALTER TABLE dbo.etapa DROP CONSTRAINT CHK_etapa_datas;

    ALTER TABLE dbo.etapa ALTER COLUMN data_abertura DATETIMEOFFSET(7) NULL;

    ALTER TABLE dbo.etapa WITH CHECK ADD CONSTRAINT CHK_etapa_datas
        CHECK (data_fechamento IS NULL OR data_fechamento > data_abertura);

    CREATE NONCLUSTERED INDEX idx_etapa_abertura ON dbo.etapa (data_abertura);
END;

COMMIT TRANSACTION;

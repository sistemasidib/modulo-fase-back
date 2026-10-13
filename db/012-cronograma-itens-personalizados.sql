/*
 * Itens personalizados do cronograma: linhas que o usuário acrescenta além das 54 etapas do
 * esqueleto (ex.: "Reunião com a comissão"). Ficam em dbo.cronograma_etapa, com personalizada = 1.
 *
 * Rodar o script inteiro em CADA banco: IDECAN_v2 e IDIB_v2 (sqlcmd -f 65001: arquivo em UTF-8).
 * É idempotente. O modulo-fase acessa a tabela por SQL nativo.
 *
 * personalizada      1 = item do usuário; 0 ou NULL = etapa do esqueleto (as linhas que já existem).
 *                    Aceita NULL para não quebrar quem insere em dbo.cronograma_etapa sem conhecê-la.
 * descricao          o que acontece (só nos itens personalizados)
 * grupo              bloco ou fase onde o item aparece (código do grupo do esqueleto)
 * referencia_tipo    de onde o item conta: MARCO_ZERO, INICIO ou FIM
 * referencia_codigo  etapa do esqueleto de referência (nulo no MARCO_ZERO)
 * tipo_status        ATIVIDADE, RECURSO ou PUBLICACAO (fluxo de status da etapa)
 *
 * Nos itens personalizados, dias e duracao são os valores do item, não ajustes do modelo.
 */

SET XACT_ABORT ON;
BEGIN TRANSACTION;

IF COL_LENGTH('dbo.cronograma_etapa', 'personalizada') IS NULL
BEGIN
    ALTER TABLE dbo.cronograma_etapa ADD
        personalizada     BIT           NULL CONSTRAINT DF_cronograma_etapa_personalizada DEFAULT (0) WITH VALUES,
        descricao         NVARCHAR(300) NULL,
        grupo             NVARCHAR(20)  NULL,
        referencia_tipo   NVARCHAR(20)  NULL,
        referencia_codigo NVARCHAR(10)  NULL,
        tipo_status       NVARCHAR(20)  NULL;
END;

/* Bancos em que a versão anterior deste script criou a coluna NOT NULL: o padrão e o CHECK abaixo
   dependem dela e precisam sair antes do ALTER COLUMN (voltam logo em seguida). */
IF EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('dbo.cronograma_etapa')
           AND name = 'personalizada' AND is_nullable = 0)
BEGIN
    IF OBJECT_ID('dbo.CK_cronograma_etapa_personalizada', 'C') IS NOT NULL
        ALTER TABLE dbo.cronograma_etapa DROP CONSTRAINT CK_cronograma_etapa_personalizada;
    IF OBJECT_ID('dbo.DF_cronograma_etapa_personalizada', 'D') IS NOT NULL
        ALTER TABLE dbo.cronograma_etapa DROP CONSTRAINT DF_cronograma_etapa_personalizada;
    ALTER TABLE dbo.cronograma_etapa ALTER COLUMN personalizada BIT NULL;
    ALTER TABLE dbo.cronograma_etapa
        ADD CONSTRAINT DF_cronograma_etapa_personalizada DEFAULT (0) FOR personalizada;
END;

IF OBJECT_ID('dbo.CK_cronograma_etapa_personalizada', 'C') IS NULL
BEGIN
    EXEC(N'ALTER TABLE dbo.cronograma_etapa ADD CONSTRAINT CK_cronograma_etapa_personalizada CHECK (
        personalizada = 0
        OR (descricao IS NOT NULL AND grupo IS NOT NULL AND referencia_tipo IS NOT NULL
            AND tipo_status IS NOT NULL AND dias IS NOT NULL AND duracao IS NOT NULL))');
END;

COMMIT TRANSACTION;

SELECT name FROM sys.columns WHERE object_id = OBJECT_ID('dbo.cronograma_etapa') ORDER BY column_id;

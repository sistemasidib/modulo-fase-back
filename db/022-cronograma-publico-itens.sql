/*
 * Spec 006 — Cronograma Público (Anexo II): itens com datas absolutas por edital.
 * Aplicar nos bancos IDECAN e IDIB. Idempotente.
 */

IF OBJECT_ID('dbo.cronograma_publico_item', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.cronograma_publico_item (
        id         INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_cronograma_publico_item PRIMARY KEY,
        edital_id  INT               NOT NULL CONSTRAINT FK_cronograma_publico_item_edital
            REFERENCES dbo.Edital (ediId),
        ordem      INT               NOT NULL,
        descricao  NVARCHAR(500)     NOT NULL,
        inicio     DATE              NOT NULL,
        termino    DATE              NOT NULL,
        destaque   BIT               NOT NULL CONSTRAINT DF_cronograma_publico_item_destaque DEFAULT (0),
        created_at DATETIMEOFFSET    NOT NULL CONSTRAINT DF_cronograma_publico_item_created DEFAULT (SYSDATETIMEOFFSET()),
        updated_at DATETIMEOFFSET    NOT NULL CONSTRAINT DF_cronograma_publico_item_updated DEFAULT (SYSDATETIMEOFFSET()),
        CONSTRAINT CK_cronograma_publico_item_datas CHECK (termino >= inicio),
        CONSTRAINT CK_cronograma_publico_item_ordem CHECK (ordem >= 1),
        CONSTRAINT UQ_cronograma_publico_item_ordem UNIQUE (edital_id, ordem)
    );
    CREATE INDEX IX_cronograma_publico_item_edital ON dbo.cronograma_publico_item (edital_id);
END
GO

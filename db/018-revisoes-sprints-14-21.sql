/*
 * Spec 006 — colunas da discursiva e modalidade S05 em dbo.etapa; tipo link_acesso;
 * configuração padrão e por edital das publicações (estrutura; valores padrão: P-08).
 *
 * Rodar em CADA banco: IDECAN_v2 e IDIB_v2 (sqlcmd -I -f 65001). É idempotente.
 * Requer db/001 (dom_tipo_publicacao, etapa) e db/015 (cronograma dono das datas).
 * GO entre ADD COLUMN e CHECK — o SQL Server não enxerga a coluna nova na mesma batch.
 */

SET XACT_ABORT ON;
GO

/* ------------------------------------------------------------------ */
/* dbo.etapa — colunas                                                 */
/* ------------------------------------------------------------------ */
IF COL_LENGTH('dbo.etapa', 'nota_zero_elimina') IS NULL
    ALTER TABLE dbo.etapa ADD nota_zero_elimina BIT NULL;
GO

IF COL_LENGTH('dbo.etapa', 'nota_minima_percentual') IS NULL
    ALTER TABLE dbo.etapa ADD nota_minima_percentual DECIMAL(5, 2) NULL;
GO

IF COL_LENGTH('dbo.etapa', 'modalidade') IS NULL
    ALTER TABLE dbo.etapa ADD modalidade NVARCHAR(20) NULL;
GO

IF OBJECT_ID('dbo.CHK_etapa_nota_minima', 'C') IS NULL
    ALTER TABLE dbo.etapa ADD CONSTRAINT CHK_etapa_nota_minima
        CHECK (nota_minima_percentual IS NULL
            OR (nota_minima_percentual > 0 AND nota_minima_percentual <= 100));
GO

IF OBJECT_ID('dbo.CHK_etapa_modalidade', 'C') IS NULL
    ALTER TABLE dbo.etapa ADD CONSTRAINT CHK_etapa_modalidade
        CHECK (modalidade IS NULL OR modalidade IN (N'ONLINE', N'PRESENCIAL', N'REMOTA'));
GO

/* ------------------------------------------------------------------ */
/* Link / instruções (modalidade Remota do S05)                        */
/* ------------------------------------------------------------------ */
INSERT INTO dbo.dom_tipo_publicacao (valor, descricao, abre_recurso)
SELECT N'link_acesso', N'Link / instruções de acesso', 0
WHERE NOT EXISTS (SELECT 1 FROM dbo.dom_tipo_publicacao WHERE valor = N'link_acesso');
GO

INSERT INTO dbo.dom_tipo_etapa_publicacao (tipo_etapa_id, tipo_publicacao_id)
SELECT te.id, tp.id
FROM dbo.dom_tipo_etapa te
JOIN dbo.dom_tipo_publicacao tp ON tp.valor = N'local_prova'
WHERE te.valor IN (N'heteroidentificacao', N'biopsicossocial')
  AND NOT EXISTS (
        SELECT 1 FROM dbo.dom_tipo_etapa_publicacao x
        WHERE x.tipo_etapa_id = te.id AND x.tipo_publicacao_id = tp.id);
GO

/* ------------------------------------------------------------------ */
/* Configuração de publicação (padrão + por edital)                    */
/* ------------------------------------------------------------------ */
IF OBJECT_ID('dbo.config_publicacao_padrao', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.config_publicacao_padrao (
        tipo_publicacao_id     INT NOT NULL
            CONSTRAINT PK_config_publicacao_padrao PRIMARY KEY
            CONSTRAINT FK_config_publicacao_padrao_tipo REFERENCES dbo.dom_tipo_publicacao (id),
        referencia_codigo      NVARCHAR(40)  NULL,
        dias_apos_referencia   INT           NULL,
        dias_no_ar             INT           NOT NULL,
        formato                NVARCHAR(20)  NOT NULL,
        CONSTRAINT CHK_config_publicacao_padrao_dias CHECK (dias_no_ar > 0),
        CONSTRAINT CHK_config_publicacao_padrao_formato
            CHECK (formato IN (N'LISTA_PDF', N'TABELA_LINK'))
    );
END;
GO

IF OBJECT_ID('dbo.config_publicacao_edital', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.config_publicacao_edital (
        id                     INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_config_publicacao_edital PRIMARY KEY,
        edital_id              INT           NOT NULL,
        tipo_publicacao_id     INT           NOT NULL
            CONSTRAINT FK_config_publicacao_edital_tipo REFERENCES dbo.dom_tipo_publicacao (id),
        referencia_codigo      NVARCHAR(40)  NULL,
        dias_apos_referencia   INT           NULL,
        dias_no_ar             INT           NOT NULL,
        formato                NVARCHAR(20)  NOT NULL,
        updated_at             DATETIMEOFFSET NOT NULL,
        CONSTRAINT UQ_config_publicacao_edital UNIQUE (edital_id, tipo_publicacao_id),
        CONSTRAINT CHK_config_publicacao_edital_dias CHECK (dias_no_ar > 0),
        CONSTRAINT CHK_config_publicacao_edital_formato
            CHECK (formato IN (N'LISTA_PDF', N'TABELA_LINK'))
    );
    CREATE INDEX IX_config_publicacao_edital_edital ON dbo.config_publicacao_edital (edital_id);
END;
GO

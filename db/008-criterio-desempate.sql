/*
 * Critérios de desempate do edital, na ordem em que se aplicam (1º, 2º, 3º...).
 *
 * Rodar o script inteiro em CADA banco: IDECAN_v2 e IDIB_v2 (sqlcmd -f 65001: arquivo em UTF-8).
 * É idempotente. O modulo-fase acessa a tabela por SQL nativo.
 *
 * tipo            IDADE | NOTA | DISCIPLINA | SORTEIO | OUTRO
 * regra           MAIOR | MENOR | SIM_NAO
 * fase_aplicacao  PROVA_OBJETIVA | RESULTADO_FINAL | AMBAS
 * referencia      disciplina, fase ou base legal ligada ao critério (obrigatória no tipo DISCIPLINA)
 * fundamentacao   item do edital ou lei (ex.: 14.3, Estatuto da Pessoa Idosa)
 * observacao      detalhes (ex.: data-base da idade)
 *
 * A ordem não tem UNIQUE: a reordenação reescreve várias linhas e a aplicação garante 1..n.
 */

SET XACT_ABORT ON;
BEGIN TRANSACTION;

IF OBJECT_ID('dbo.criterio_desempate', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.criterio_desempate (
        id             INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_criterio_desempate PRIMARY KEY,
        edital_id      INT               NOT NULL CONSTRAINT FK_criterio_desempate_edital REFERENCES dbo.Edital (ediId),
        ordem          SMALLINT          NOT NULL CONSTRAINT CK_criterio_desempate_ordem CHECK (ordem > 0),
        criterio       NVARCHAR(300)     NOT NULL,
        tipo           VARCHAR(20)       NOT NULL CONSTRAINT CK_criterio_desempate_tipo
                           CHECK (tipo IN ('IDADE', 'NOTA', 'DISCIPLINA', 'SORTEIO', 'OUTRO')),
        regra          VARCHAR(10)       NOT NULL CONSTRAINT CK_criterio_desempate_regra
                           CHECK (regra IN ('MAIOR', 'MENOR', 'SIM_NAO')),
        referencia     NVARCHAR(200)     NULL,
        fase_aplicacao VARCHAR(20)       NOT NULL CONSTRAINT CK_criterio_desempate_fase_aplicacao
                           CHECK (fase_aplicacao IN ('PROVA_OBJETIVA', 'RESULTADO_FINAL', 'AMBAS')),
        fundamentacao  NVARCHAR(300)     NULL,
        observacao     NVARCHAR(500)     NULL,
        created_at     DATETIMEOFFSET    NOT NULL,
        updated_at     DATETIMEOFFSET    NOT NULL
    );
    CREATE INDEX IX_criterio_desempate_edital ON dbo.criterio_desempate (edital_id, ordem);
END;

COMMIT TRANSACTION;

SELECT name FROM sys.columns WHERE object_id = OBJECT_ID('dbo.criterio_desempate') ORDER BY column_id;

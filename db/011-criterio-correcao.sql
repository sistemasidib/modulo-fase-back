/*
 * Critérios de correção da prova discursiva: o que a banca avalia no texto, agrupado em módulos
 * (ex.: Formal, Textual, Técnico), com a pontuação máxima de cada critério. A prova objetiva não
 * usa esta tabela: lá a nota vem das disciplinas e do valor de cada questão.
 *
 * Rodar o script inteiro em CADA banco: IDECAN_v2 e IDIB_v2 (sqlcmd -f 65001: arquivo em UTF-8).
 * É idempotente. O modulo-fase acessa a tabela por SQL nativo.
 *
 * etapa_id           a prova discursiva (dbo.etapa), como as disciplinas da prova
 * ordem              posição na etapa (1..n); os módulos aparecem na ordem do primeiro critério de cada um
 * modulo             grupo do critério (ex.: Formal); o total do módulo é a soma dos critérios dele
 * criterio           o que é avaliado (ex.: Concordância verbal e nominal)
 * desconto_por_erro  pontos tirados a cada erro (ex.: 0,25); nulo = a banca atribui a nota direto
 * pontuacao_maxima   nota máxima do critério; a nota da prova é a soma de todos os critérios
 *
 * A ordem não tem UNIQUE: a reordenação reescreve várias linhas e a aplicação garante 1..n.
 */

SET XACT_ABORT ON;
BEGIN TRANSACTION;

IF OBJECT_ID('dbo.criterio_correcao', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.criterio_correcao (
        id                INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_criterio_correcao PRIMARY KEY,
        etapa_id          INT               NOT NULL CONSTRAINT FK_criterio_correcao_etapa REFERENCES dbo.etapa (id),
        ordem             SMALLINT          NOT NULL CONSTRAINT CK_criterio_correcao_ordem CHECK (ordem > 0),
        modulo            NVARCHAR(100)     NOT NULL,
        criterio          NVARCHAR(500)     NOT NULL,
        desconto_por_erro DECIMAL(6,2)      NULL CONSTRAINT CK_criterio_correcao_desconto CHECK (desconto_por_erro > 0),
        pontuacao_maxima  DECIMAL(6,2)      NOT NULL CONSTRAINT CK_criterio_correcao_pontuacao CHECK (pontuacao_maxima > 0),
        created_at        DATETIMEOFFSET    NOT NULL,
        updated_at        DATETIMEOFFSET    NOT NULL
    );
    CREATE INDEX IX_criterio_correcao_etapa ON dbo.criterio_correcao (etapa_id, ordem);
END;

COMMIT TRANSACTION;

SELECT name FROM sys.columns WHERE object_id = OBJECT_ID('dbo.criterio_correcao') ORDER BY column_id;

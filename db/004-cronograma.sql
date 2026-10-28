/*
 * Especificação 003 — Cronograma unificado: concurso e cronograma.
 *
 * Rodar o script inteiro em CADA banco: IDECAN_v2 e IDIB_v2.
 * É idempotente: pode rodar de novo sem duplicar tabelas nem linhas.
 *
 * Tabelas existentes NÃO são alteradas. O vínculo edital → concurso fica em concurso_edital,
 * e não numa coluna nova em dbo.Edital (PK legada ediId preservada).
 *
 * O esqueleto (54 etapas) fica no código (EsqueletoIdecanV1). Aqui ficam só os números do
 * modelo (RN17), que mudam sem deploy: UPDATE dbo.cronograma_parametro_padrao.
 * O modulo-fase acessa estas tabelas por SQL nativo (sem entidades no sys-core).
 */

SET XACT_ABORT ON;
BEGIN TRANSACTION;

/* ------------------------------------------------------------------ */
/* Concurso (RN01): agrupa editais; um edital em no máximo um concurso */
/* ------------------------------------------------------------------ */
IF OBJECT_ID('dbo.concurso', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.concurso (
        id         INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_concurso PRIMARY KEY,
        nome       NVARCHAR(200)     NOT NULL CONSTRAINT UQ_concurso_nome UNIQUE,
        created_at DATETIMEOFFSET    NOT NULL,
        updated_at DATETIMEOFFSET    NOT NULL
    );
END;

IF OBJECT_ID('dbo.concurso_edital', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.concurso_edital (
        id          INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_concurso_edital PRIMARY KEY,
        concurso_id INT NOT NULL CONSTRAINT FK_concurso_edital_concurso REFERENCES dbo.concurso (id),
        edital_id   INT NOT NULL CONSTRAINT FK_concurso_edital_edital REFERENCES dbo.Edital (ediId),
        CONSTRAINT UQ_concurso_edital_edital UNIQUE (edital_id)
    );
    CREATE INDEX IX_concurso_edital_concurso ON dbo.concurso_edital (concurso_id);
END;

/* ------------------------------------------------------------------ */
/* Números do modelo (RN13–RN19, RN24)                                 */
/* ------------------------------------------------------------------ */
IF OBJECT_ID('dbo.cronograma_parametro_padrao', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.cronograma_parametro_padrao (
        chave     NVARCHAR(50)  NOT NULL CONSTRAINT PK_cronograma_parametro_padrao PRIMARY KEY,
        valor     INT           NOT NULL CONSTRAINT CK_cronograma_parametro_padrao_valor CHECK (valor >= 0),
        descricao NVARCHAR(255) NULL
    );
END;

INSERT INTO dbo.cronograma_parametro_padrao (chave, valor, descricao)
SELECT v.chave, v.valor, v.descricao
FROM (VALUES
    (N'inicio_inscricoes',   1,  N'Início das inscrições (dias depois do edital)'),
    (N'duracao_inscricoes',  30, N'Duração das inscrições (padrão operacional)'),
    (N'isencao',             2,  N'Pedido de isenção da taxa (duração; padrão 2 dias)'),
    (N'pagamento',           1,  N'Último dia de pagamento (dias depois do fim das inscrições)'),
    (N'intervalo_prova',     43, N'Fim das inscrições → prova objetiva (levada ao domingo seguinte)'),
    (N'gabarito_apos_prova', 7,  N'Gabarito preliminar (dias depois da prova; norma 1–2 semanas)'),
    (N'recurso_gabarito',    5,  N'Recurso contra o gabarito (duração; faixa 2–7)'),
    (N'recurso_demais',      5,  N'Demais recursos (duração; faixa 2–7)'),
    (N'rm01_minimo',         45, N'RM-01: mínimo entre o fim das inscrições e a prova')
) AS v (chave, valor, descricao)
WHERE NOT EXISTS (SELECT 1 FROM dbo.cronograma_parametro_padrao p WHERE p.chave = v.chave);

/* ------------------------------------------------------------------ */
/* Cronograma do concurso (RN03): um por concurso                      */
/* ------------------------------------------------------------------ */
IF OBJECT_ID('dbo.cronograma', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.cronograma (
        id            INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_cronograma PRIMARY KEY,
        concurso_id   INT               NOT NULL CONSTRAINT FK_cronograma_concurso REFERENCES dbo.concurso (id),
        esqueleto     NVARCHAR(100)     NOT NULL,
        data_edital   DATE              NULL,
        modulos       NVARCHAR(100)     NOT NULL CONSTRAINT DF_cronograma_modulos DEFAULT (N''),
        justificativa NVARCHAR(2000)    NULL,
        versao        INT               NOT NULL CONSTRAINT DF_cronograma_versao DEFAULT (0),
        created_at    DATETIMEOFFSET    NOT NULL,
        updated_at    DATETIMEOFFSET    NOT NULL,
        CONSTRAINT UQ_cronograma_concurso UNIQUE (concurso_id)
    );
END;

/* Cópia dos parâmetros na criação (RN17) + exceção liberada (RN19). */
IF OBJECT_ID('dbo.cronograma_parametro', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.cronograma_parametro (
        cronograma_id INT          NOT NULL CONSTRAINT FK_cronograma_parametro_cronograma REFERENCES dbo.cronograma (id),
        chave         NVARCHAR(50) NOT NULL,
        valor         INT          NOT NULL,
        excecao       BIT          NOT NULL CONSTRAINT DF_cronograma_parametro_excecao DEFAULT (0),
        CONSTRAINT PK_cronograma_parametro PRIMARY KEY (cronograma_id, chave)
    );
END;

/* Uma linha por etapa do esqueleto: ajustes (NULL = modelo), removida, status manual e
   cópia das datas do último cálculo, para leitura. */
IF OBJECT_ID('dbo.cronograma_etapa', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.cronograma_etapa (
        id             INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_cronograma_etapa PRIMARY KEY,
        cronograma_id  INT          NOT NULL CONSTRAINT FK_cronograma_etapa_cronograma REFERENCES dbo.cronograma (id),
        codigo         NVARCHAR(10) NOT NULL,
        dias           INT          NULL,
        duracao        INT          NULL,
        removida       BIT          NOT NULL CONSTRAINT DF_cronograma_etapa_removida DEFAULT (0),
        status_manual  NVARCHAR(30) NOT NULL CONSTRAINT DF_cronograma_etapa_status DEFAULT (N'A_FAZER'),
        inicio_rel     INT          NOT NULL,
        fim_rel        INT          NOT NULL,
        inicio         DATE         NULL,
        fim            DATE         NULL,
        CONSTRAINT UQ_cronograma_etapa_codigo UNIQUE (cronograma_id, codigo)
    );
END;

IF OBJECT_ID('dbo.cronograma_particularidade', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.cronograma_particularidade (
        id            INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_cronograma_particularidade PRIMARY KEY,
        cronograma_id INT            NOT NULL CONSTRAINT FK_cronograma_particularidade_cronograma REFERENCES dbo.cronograma (id),
        texto         NVARCHAR(500)  NOT NULL,
        assunto       NVARCHAR(50)   NOT NULL,
        etapa_codigo  NVARCHAR(10)   NULL,
        autor         NVARCHAR(150)  NOT NULL,
        criado_em     DATETIMEOFFSET NOT NULL
    );
    CREATE INDEX IX_cronograma_particularidade_cronograma ON dbo.cronograma_particularidade (cronograma_id);
END;

/* Histórico só de inclusão (RN27): o serviço não tem UPDATE nem DELETE nestas tabelas. */
IF OBJECT_ID('dbo.cronograma_acao', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.cronograma_acao (
        id            BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT PK_cronograma_acao PRIMARY KEY,
        cronograma_id INT            NOT NULL CONSTRAINT FK_cronograma_acao_cronograma REFERENCES dbo.cronograma (id),
        autor         NVARCHAR(150)  NOT NULL,
        criado_em     DATETIMEOFFSET NOT NULL,
        acao          NVARCHAR(500)  NOT NULL,
        detalhe       NVARCHAR(500)  NULL,
        origem        NVARCHAR(50)   NOT NULL,
        justificativa NVARCHAR(2000) NULL
    );
    CREATE INDEX IX_cronograma_acao_cronograma ON dbo.cronograma_acao (cronograma_id, id);
END;

IF OBJECT_ID('dbo.cronograma_acao_item', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.cronograma_acao_item (
        id           BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT PK_cronograma_acao_item PRIMARY KEY,
        acao_id      BIGINT        NOT NULL CONSTRAINT FK_cronograma_acao_item_acao REFERENCES dbo.cronograma_acao (id),
        etapa_codigo NVARCHAR(10)  NULL,
        campo        NVARCHAR(100) NOT NULL,
        antes        NVARCHAR(100) NULL,
        depois       NVARCHAR(100) NULL
    );
    CREATE INDEX IX_cronograma_acao_item_acao ON dbo.cronograma_acao_item (acao_id);
END;

COMMIT TRANSACTION;

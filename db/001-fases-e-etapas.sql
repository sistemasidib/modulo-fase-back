/*
 * Especificação 001 — Fases e Etapas: tabelas novas e valores de domínio.
 *
 * Rodar o script inteiro em CADA banco: IDECAN_v2 e IDIB_v2 (resposta 15: as duas instituições).
 * É idempotente: pode rodar de novo sem duplicar tabelas nem linhas.
 *
 * Tabelas existentes NÃO são alteradas (resposta 17: editais antigos ficam como estão).
 * A ligação fase → grupo fica em fase_grupo_cargo, e não numa coluna nova em dbo.fases.
 *
 * Entidades correspondentes no sys-core (idecan e idib):
 *   faseDominio.DomTipoEtapa, DomTipoPublicacao, DomTipoEtapaPublicacao
 *   dbo.GrupoCargo, GrupoCargoCargo, FaseGrupoCargo, Publicacao
 */

SET XACT_ABORT ON;
BEGIN TRANSACTION;

/* ------------------------------------------------------------------ */
/* Domínio: tipos de etapa (respostas 5 e 7)                          */
/* ------------------------------------------------------------------ */
IF OBJECT_ID('dbo.dom_tipo_etapa', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.dom_tipo_etapa (
        id        INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_dom_tipo_etapa PRIMARY KEY,
        valor     NVARCHAR(100)     NOT NULL CONSTRAINT UQ_dom_tipo_etapa_valor UNIQUE,
        descricao NVARCHAR(255)     NULL
    );
END;

INSERT INTO dbo.dom_tipo_etapa (valor, descricao)
SELECT v.valor, v.descricao
FROM (VALUES
    (N'prova_objetiva',               N'Prova objetiva'),
    (N'prova_discursiva',             N'Prova discursiva'),
    (N'taf',                          N'Teste de aptidão física (TAF)'),
    (N'the',                          N'Teste de habilidade específica (THE)'),
    (N'heteroidentificacao',          N'Heteroidentificação'),
    (N'biopsicossocial',              N'Avaliação biopsicossocial'),
    (N'analise_atendimento_especial', N'Análise de solicitações: atendimento especial'),
    (N'analise_pcd',                  N'Análise de solicitações: PcD'),
    (N'analise_hipossuficiencia',     N'Análise de solicitações: hipossuficiência'),
    (N'analise_isencao_pagamento',    N'Análise de solicitações: isenção de pagamento'),
    (N'analise_gabarito',             N'Análise de solicitações: gabarito'),
    (N'analise_impugnacao',           N'Análise de solicitações: impugnação'),
    (N'analise_prova_titulos',        N'Análise de solicitações: prova de títulos'),
    (N'analise_locais_prova',         N'Análise de solicitações: locais de prova (ensalamento)')
) AS v (valor, descricao)
WHERE NOT EXISTS (SELECT 1 FROM dbo.dom_tipo_etapa d WHERE d.valor = v.valor);

/* ------------------------------------------------------------------ */
/* Domínio: tipos de publicação (respostas 1 e 2)                     */
/* abre_recurso = 1: publicar este tipo abre o período de recurso.    */
/* ------------------------------------------------------------------ */
IF OBJECT_ID('dbo.dom_tipo_publicacao', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.dom_tipo_publicacao (
        id           INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_dom_tipo_publicacao PRIMARY KEY,
        valor        NVARCHAR(100)     NOT NULL CONSTRAINT UQ_dom_tipo_publicacao_valor UNIQUE,
        descricao    NVARCHAR(255)     NULL,
        abre_recurso BIT               NOT NULL CONSTRAINT DF_dom_tipo_publicacao_abre_recurso DEFAULT (0)
    );
END;

INSERT INTO dbo.dom_tipo_publicacao (valor, descricao, abre_recurso)
SELECT v.valor, v.descricao, v.abre_recurso
FROM (VALUES
    (N'local_prova',          N'Local de prova',       0),
    (N'gabarito_preliminar',  N'Gabarito preliminar',  1),
    (N'gabarito_definitivo',  N'Gabarito definitivo',  0),
    (N'resultado_preliminar', N'Resultado preliminar', 1),
    (N'resultado_oficial',    N'Resultado oficial',    0)
) AS v (valor, descricao, abre_recurso)
WHERE NOT EXISTS (SELECT 1 FROM dbo.dom_tipo_publicacao d WHERE d.valor = v.valor);

/* ------------------------------------------------------------------ */
/* Domínio: publicações criadas junto com cada tipo de etapa          */
/* Toda etapa: resultado preliminar e oficial (resposta 2).           */
/* Prova objetiva: + local de prova e gabaritos (especificação, CA08).*/
/* Os demais tipos ganham publicações extras incluindo linhas aqui    */
/* ou pela tela (POST /publicacoes), sem mudar código.                */
/* ------------------------------------------------------------------ */
IF OBJECT_ID('dbo.dom_tipo_etapa_publicacao', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.dom_tipo_etapa_publicacao (
        id                 INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_dom_tipo_etapa_publicacao PRIMARY KEY,
        tipo_etapa_id      INT NOT NULL
            CONSTRAINT FK_dom_tipo_etapa_publicacao_tipo_etapa REFERENCES dbo.dom_tipo_etapa (id),
        tipo_publicacao_id INT NOT NULL
            CONSTRAINT FK_dom_tipo_etapa_publicacao_tipo_publicacao REFERENCES dbo.dom_tipo_publicacao (id),
        CONSTRAINT UQ_dom_tipo_etapa_publicacao UNIQUE (tipo_etapa_id, tipo_publicacao_id)
    );
END;

INSERT INTO dbo.dom_tipo_etapa_publicacao (tipo_etapa_id, tipo_publicacao_id)
SELECT te.id, tp.id
FROM dbo.dom_tipo_etapa te
JOIN dbo.dom_tipo_publicacao tp
  ON tp.valor IN (N'resultado_preliminar', N'resultado_oficial')
  OR (te.valor = N'prova_objetiva' AND tp.valor IN (N'local_prova', N'gabarito_preliminar', N'gabarito_definitivo'))
WHERE te.valor IN (
        N'prova_objetiva', N'prova_discursiva', N'taf', N'the', N'heteroidentificacao', N'biopsicossocial',
        N'analise_atendimento_especial', N'analise_pcd', N'analise_hipossuficiencia', N'analise_isencao_pagamento',
        N'analise_gabarito', N'analise_impugnacao', N'analise_prova_titulos', N'analise_locais_prova')
  AND NOT EXISTS (
        SELECT 1 FROM dbo.dom_tipo_etapa_publicacao x
        WHERE x.tipo_etapa_id = te.id AND x.tipo_publicacao_id = tp.id);

/* ------------------------------------------------------------------ */
/* Grupos de cargos (RN02, respostas 9 e 10)                          */
/* ------------------------------------------------------------------ */
IF OBJECT_ID('dbo.grupo_cargo', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.grupo_cargo (
        id         INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_grupo_cargo PRIMARY KEY,
        edital_id  INT               NOT NULL CONSTRAINT FK_grupo_cargo_edital REFERENCES dbo.Edital (ediId),
        nome       NVARCHAR(150)     NOT NULL,
        created_at DATETIMEOFFSET    NOT NULL,
        updated_at DATETIMEOFFSET    NOT NULL,
        CONSTRAINT UQ_grupo_cargo_edital_nome UNIQUE (edital_id, nome)
    );
END;

/* Um cargo está em no máximo um grupo (CA04): UK em cargo_id. */
IF OBJECT_ID('dbo.grupo_cargo_cargo', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.grupo_cargo_cargo (
        id             INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_grupo_cargo_cargo PRIMARY KEY,
        grupo_cargo_id INT NOT NULL CONSTRAINT FK_grupo_cargo_cargo_grupo REFERENCES dbo.grupo_cargo (id),
        cargo_id       INT NOT NULL CONSTRAINT FK_grupo_cargo_cargo_cargo REFERENCES dbo.Cargo (carId),
        CONSTRAINT UQ_grupo_cargo_cargo_cargo UNIQUE (cargo_id)
    );
    CREATE INDEX IX_grupo_cargo_cargo_grupo ON dbo.grupo_cargo_cargo (grupo_cargo_id);
END;

/* Fase gerada para um grupo. Fases sem linha aqui são do modelo antigo. */
IF OBJECT_ID('dbo.fase_grupo_cargo', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.fase_grupo_cargo (
        id             INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_fase_grupo_cargo PRIMARY KEY,
        fases_id       INT NOT NULL CONSTRAINT FK_fase_grupo_cargo_fase REFERENCES dbo.fases (id),
        grupo_cargo_id INT NOT NULL CONSTRAINT FK_fase_grupo_cargo_grupo REFERENCES dbo.grupo_cargo (id),
        CONSTRAINT UQ_fase_grupo_cargo_fase UNIQUE (fases_id)
    );
    CREATE INDEX IX_fase_grupo_cargo_grupo ON dbo.fase_grupo_cargo (grupo_cargo_id);
END;

/* ------------------------------------------------------------------ */
/* Espaços de publicação da fase (etapa_id nulo) ou da etapa          */
/* (RN05, respostas 1, 2, 3, 4 e 6: todas as datas são manuais).      */
/* ------------------------------------------------------------------ */
IF OBJECT_ID('dbo.publicacao', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.publicacao (
        id                  INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_publicacao PRIMARY KEY,
        fases_id            INT NOT NULL CONSTRAINT FK_publicacao_fase REFERENCES dbo.fases (id),
        etapa_id            INT NULL     CONSTRAINT FK_publicacao_etapa REFERENCES dbo.etapa (id),
        tipo_publicacao_id  INT NOT NULL CONSTRAINT FK_publicacao_tipo REFERENCES dbo.dom_tipo_publicacao (id),
        data_prevista       DATETIMEOFFSET NULL,
        visualizacao_inicio DATETIMEOFFSET NULL,
        visualizacao_fim    DATETIMEOFFSET NULL,
        recurso_inicio      DATETIMEOFFSET NULL,
        recurso_fim         DATETIMEOFFSET NULL,
        publicada_em        DATETIMEOFFSET NULL,
        created_at          DATETIMEOFFSET NOT NULL,
        updated_at          DATETIMEOFFSET NOT NULL,
        CONSTRAINT CK_publicacao_visualizacao CHECK (visualizacao_fim IS NULL OR visualizacao_fim >= visualizacao_inicio),
        CONSTRAINT CK_publicacao_recurso      CHECK (recurso_fim IS NULL OR recurso_fim >= recurso_inicio)
    );
    CREATE INDEX IX_publicacao_fase  ON dbo.publicacao (fases_id);
    CREATE INDEX IX_publicacao_etapa ON dbo.publicacao (etapa_id);
END;

COMMIT TRANSACTION;

/* Conferência */
SELECT 'dom_tipo_etapa' AS tabela, COUNT(*) AS linhas FROM dbo.dom_tipo_etapa
UNION ALL SELECT 'dom_tipo_publicacao', COUNT(*) FROM dbo.dom_tipo_publicacao
UNION ALL SELECT 'dom_tipo_etapa_publicacao', COUNT(*) FROM dbo.dom_tipo_etapa_publicacao;

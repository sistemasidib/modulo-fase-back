/*
 * Critérios de desempate: estrutura da API (label, tipo, direcao, condicao, dataReferencia, fonte, baseLegal).
 *
 * Rodar em CADA banco: IDECAN_v2 e IDIB_v2 (sqlcmd -f 65001). Idempotente.
 *
 * tipo            idade | nota | booleano
 * direcao         asc | desc
 * condicao_*      operador / valor / unidade (opcional)
 * data_referencia ex.: ultimo_dia_inscricao
 * fonte           ex.: prova_objetiva, conhecimentos_especificos
 * base_legal      ex.: idoso, jurado
 *
 * Colunas novas entram via ALTER; referências a elas usam EXEC (batch separado no SQL Server).
 */

SET XACT_ABORT ON;
BEGIN TRANSACTION;

IF OBJECT_ID('dbo.criterio_desempate', 'U') IS NOT NULL
BEGIN
    IF COL_LENGTH('dbo.criterio_desempate', 'label') IS NULL
        ALTER TABLE dbo.criterio_desempate ADD label NVARCHAR(300) NULL;
    IF COL_LENGTH('dbo.criterio_desempate', 'direcao') IS NULL
        ALTER TABLE dbo.criterio_desempate ADD direcao VARCHAR(4) NULL;
    IF COL_LENGTH('dbo.criterio_desempate', 'condicao_operador') IS NULL
        ALTER TABLE dbo.criterio_desempate ADD condicao_operador VARCHAR(10) NULL;
    IF COL_LENGTH('dbo.criterio_desempate', 'condicao_valor') IS NULL
        ALTER TABLE dbo.criterio_desempate ADD condicao_valor NVARCHAR(50) NULL;
    IF COL_LENGTH('dbo.criterio_desempate', 'condicao_unidade') IS NULL
        ALTER TABLE dbo.criterio_desempate ADD condicao_unidade NVARCHAR(40) NULL;
    IF COL_LENGTH('dbo.criterio_desempate', 'data_referencia') IS NULL
        ALTER TABLE dbo.criterio_desempate ADD data_referencia NVARCHAR(80) NULL;
    IF COL_LENGTH('dbo.criterio_desempate', 'fonte') IS NULL
        ALTER TABLE dbo.criterio_desempate ADD fonte NVARCHAR(120) NULL;
    IF COL_LENGTH('dbo.criterio_desempate', 'base_legal') IS NULL
        ALTER TABLE dbo.criterio_desempate ADD base_legal NVARCHAR(80) NULL;

    /* Remove checks antigos antes de alterar valores de tipo/regra. */
    IF EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = 'CK_criterio_desempate_tipo'
               AND parent_object_id = OBJECT_ID('dbo.criterio_desempate'))
        ALTER TABLE dbo.criterio_desempate DROP CONSTRAINT CK_criterio_desempate_tipo;
    IF EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = 'CK_criterio_desempate_regra'
               AND parent_object_id = OBJECT_ID('dbo.criterio_desempate'))
        ALTER TABLE dbo.criterio_desempate DROP CONSTRAINT CK_criterio_desempate_regra;
    IF EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = 'CK_criterio_desempate_fase_aplicacao'
               AND parent_object_id = OBJECT_ID('dbo.criterio_desempate'))
        ALTER TABLE dbo.criterio_desempate DROP CONSTRAINT CK_criterio_desempate_fase_aplicacao;
    IF EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = 'CK_criterio_desempate_direcao'
               AND parent_object_id = OBJECT_ID('dbo.criterio_desempate'))
        ALTER TABLE dbo.criterio_desempate DROP CONSTRAINT CK_criterio_desempate_direcao;

    /* Migra colunas antigas (criterio/tipo/regra/referencia/…) se ainda existirem. */
    IF COL_LENGTH('dbo.criterio_desempate', 'criterio') IS NOT NULL
    BEGIN
        EXEC('
            UPDATE dbo.criterio_desempate
            SET label = COALESCE(label, criterio),
                direcao = COALESCE(direcao, CASE regra WHEN ''MENOR'' THEN ''asc'' ELSE ''desc'' END),
                fonte = COALESCE(fonte, CASE
                    WHEN tipo IN (''NOTA'', ''DISCIPLINA'') AND NULLIF(LTRIM(RTRIM(referencia)), '''') IS NOT NULL
                        THEN LOWER(REPLACE(REPLACE(LTRIM(RTRIM(referencia)), '' '', ''_''), ''ã'', ''a''))
                    ELSE NULL END),
                base_legal = COALESCE(base_legal, CASE
                    WHEN tipo = ''IDADE'' THEN ''idoso''
                    WHEN fundamentacao LIKE ''%jurado%'' THEN ''jurado''
                    ELSE NULL END),
                data_referencia = COALESCE(data_referencia, CASE
                    WHEN observacao LIKE ''%último dia%'' OR observacao LIKE ''%ultimo dia%''
                        THEN ''ultimo_dia_inscricao''
                    ELSE NULL END),
                condicao_operador = COALESCE(condicao_operador, CASE
                    WHEN tipo = ''IDADE'' THEN ''>=''
                    ELSE NULL END),
                condicao_valor = COALESCE(condicao_valor, CASE
                    WHEN tipo = ''IDADE'' THEN ''60''
                    ELSE NULL END),
                condicao_unidade = COALESCE(condicao_unidade, CASE
                    WHEN tipo = ''IDADE'' THEN ''anos''
                    ELSE NULL END)
            WHERE label IS NULL OR direcao IS NULL;
        ');

        EXEC('
            UPDATE dbo.criterio_desempate
            SET tipo = CASE UPPER(tipo)
                WHEN ''IDADE'' THEN ''idade''
                WHEN ''NOTA'' THEN ''nota''
                WHEN ''DISCIPLINA'' THEN ''nota''
                WHEN ''SORTEIO'' THEN ''booleano''
                WHEN ''OUTRO'' THEN ''booleano''
                ELSE LOWER(tipo) END
            WHERE tipo COLLATE Latin1_General_CS_AS <> LOWER(tipo)
               OR tipo IN (''IDADE'', ''NOTA'', ''DISCIPLINA'', ''SORTEIO'', ''OUTRO'');
        ');
    END;

    /* Garante NOT NULL nas novas colunas obrigatórias. */
    EXEC('UPDATE dbo.criterio_desempate SET label = N''Critério'' WHERE label IS NULL OR LTRIM(RTRIM(label)) = ''''');
    EXEC('UPDATE dbo.criterio_desempate SET direcao = ''desc'' WHERE direcao IS NULL');
    EXEC('UPDATE dbo.criterio_desempate SET fonte = ''prova_objetiva'' WHERE LOWER(tipo) = ''nota'' AND (fonte IS NULL OR LTRIM(RTRIM(fonte)) = '''')');

    EXEC('ALTER TABLE dbo.criterio_desempate ALTER COLUMN label NVARCHAR(300) NOT NULL');
    EXEC('ALTER TABLE dbo.criterio_desempate ALTER COLUMN direcao VARCHAR(4) NOT NULL');
    EXEC('ALTER TABLE dbo.criterio_desempate ALTER COLUMN tipo VARCHAR(20) NOT NULL');

    EXEC('
        ALTER TABLE dbo.criterio_desempate
            ADD CONSTRAINT CK_criterio_desempate_tipo
                CHECK (tipo IN (''idade'', ''nota'', ''booleano''));
    ');
    EXEC('
        ALTER TABLE dbo.criterio_desempate
            ADD CONSTRAINT CK_criterio_desempate_direcao
                CHECK (direcao IN (''asc'', ''desc''));
    ');

    IF COL_LENGTH('dbo.criterio_desempate', 'criterio') IS NOT NULL
        EXEC('ALTER TABLE dbo.criterio_desempate DROP COLUMN criterio');
    IF COL_LENGTH('dbo.criterio_desempate', 'regra') IS NOT NULL
        EXEC('ALTER TABLE dbo.criterio_desempate DROP COLUMN regra');
    IF COL_LENGTH('dbo.criterio_desempate', 'referencia') IS NOT NULL
        EXEC('ALTER TABLE dbo.criterio_desempate DROP COLUMN referencia');
    IF COL_LENGTH('dbo.criterio_desempate', 'fase_aplicacao') IS NOT NULL
        EXEC('ALTER TABLE dbo.criterio_desempate DROP COLUMN fase_aplicacao');
    IF COL_LENGTH('dbo.criterio_desempate', 'fundamentacao') IS NOT NULL
        EXEC('ALTER TABLE dbo.criterio_desempate DROP COLUMN fundamentacao');
    IF COL_LENGTH('dbo.criterio_desempate', 'observacao') IS NOT NULL
        EXEC('ALTER TABLE dbo.criterio_desempate DROP COLUMN observacao');
END;

COMMIT TRANSACTION;

SELECT name FROM sys.columns WHERE object_id = OBJECT_ID('dbo.criterio_desempate') ORDER BY column_id;

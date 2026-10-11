/*
 * Disciplinas e critérios de desempate amarrados à fase de cada grupo.
 *
 * Rodar o script inteiro em CADA banco: IDECAN_v2 e IDIB_v2 (sqlcmd -f 65001: arquivo em UTF-8).
 * É idempotente.
 *
 * DisciplinasEditaisQuestoes.etapa_id  etapa (prova) a que a faixa de questões pertence.
 *     Nulo = faixa antiga, cadastrada antes desta coluna: vale como prova objetiva do edital.
 *     O modulo-dados classifica os recursos só pelas faixas sem etapa ou de etapa prova_objetiva.
 *
 * criterio_desempate.fase_id  fase (de um grupo) a que o critério pertence; substitui edital_id.
 *     Cada critério já cadastrado é copiado, na mesma ordem, para as fases do seu edital que estão num grupo
 *     e têm prova objetiva ou discursiva; critério de edital sem essas fases é descartado.
 *
 * As instruções que usam fase_id vão em EXEC: a coluna ainda não existe quando o lote é compilado.
 */

SET XACT_ABORT ON;
BEGIN TRANSACTION;

IF COL_LENGTH('dbo.DisciplinasEditaisQuestoes', 'etapa_id') IS NULL
BEGIN
    ALTER TABLE dbo.DisciplinasEditaisQuestoes
        ADD etapa_id INT NULL CONSTRAINT FK_DisciplinasEditaisQuestoes_etapa REFERENCES dbo.etapa (id);
    EXEC('CREATE INDEX IX_DisciplinasEditaisQuestoes_etapa ON dbo.DisciplinasEditaisQuestoes (etapa_id)');
END;

IF COL_LENGTH('dbo.criterio_desempate', 'fase_id') IS NULL
BEGIN
    ALTER TABLE dbo.criterio_desempate ADD fase_id INT NULL;

    EXEC('
        INSERT INTO dbo.criterio_desempate
            (edital_id, fase_id, ordem, criterio, tipo, regra, referencia, fase_aplicacao,
             fundamentacao, observacao, created_at, updated_at)
        SELECT c.edital_id, f.id, c.ordem, c.criterio, c.tipo, c.regra, c.referencia, c.fase_aplicacao,
               c.fundamentacao, c.observacao, c.created_at, c.updated_at
        FROM dbo.criterio_desempate c
        INNER JOIN dbo.fases f ON f.edital_id = c.edital_id
        WHERE c.fase_id IS NULL
          AND EXISTS (SELECT 1 FROM dbo.fase_grupo_cargos g WHERE g.fases_id = f.id)
          AND EXISTS (SELECT 1 FROM dbo.etapa e
                      INNER JOIN dbo.dom_tipo_etapa te ON te.id = e.tipo_id
                      WHERE e.fases_id = f.id AND te.valor IN (N''prova_objetiva'', N''prova_discursiva''));
        DELETE FROM dbo.criterio_desempate WHERE fase_id IS NULL;');

    IF EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_criterio_desempate_edital'
               AND object_id = OBJECT_ID('dbo.criterio_desempate'))
        DROP INDEX IX_criterio_desempate_edital ON dbo.criterio_desempate;
    IF OBJECT_ID('dbo.FK_criterio_desempate_edital', 'F') IS NOT NULL
        ALTER TABLE dbo.criterio_desempate DROP CONSTRAINT FK_criterio_desempate_edital;
    IF COL_LENGTH('dbo.criterio_desempate', 'edital_id') IS NOT NULL
        ALTER TABLE dbo.criterio_desempate DROP COLUMN edital_id;

    EXEC('ALTER TABLE dbo.criterio_desempate ALTER COLUMN fase_id INT NOT NULL');
    EXEC('ALTER TABLE dbo.criterio_desempate
              ADD CONSTRAINT FK_criterio_desempate_fase FOREIGN KEY (fase_id) REFERENCES dbo.fases (id)');
    EXEC('CREATE INDEX IX_criterio_desempate_fase ON dbo.criterio_desempate (fase_id, ordem)');
END;

COMMIT TRANSACTION;

SELECT 'DisciplinasEditaisQuestoes' AS tabela, name AS coluna FROM sys.columns
WHERE object_id = OBJECT_ID('dbo.DisciplinasEditaisQuestoes') AND name = 'etapa_id'
UNION ALL
SELECT 'criterio_desempate', name FROM sys.columns
WHERE object_id = OBJECT_ID('dbo.criterio_desempate') AND name IN ('fase_id', 'edital_id');

EXEC('SELECT fase_id, COUNT(*) AS criterios FROM dbo.criterio_desempate GROUP BY fase_id ORDER BY fase_id');

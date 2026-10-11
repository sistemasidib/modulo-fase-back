/*
 * Valor (peso) de cada questão da disciplina e ordem de desempate na prova objetiva.
 *
 * Reaproveita as tabelas que já existem: Disciplinas (nome), DisciplinasEditais (disciplina no
 * edital) e DisciplinasEditaisQuestoes (faixa de questões, por cargo). Só acrescenta duas colunas
 * anuláveis na faixa, como disPeso/disDesempate da tabela legada Disciplina:
 *   peso       nº de questões = questaoFim - questaoInicio + 1; total de pontos = nº de questões × peso
 *   desempate  1 = primeiro critério ("maior nota em ..."); nulo = não desempata
 *
 * Rodar o script inteiro em CADA banco: IDECAN_v2 e IDIB_v2.
 * É idempotente. Requer a coluna cargo_id (modulo-dados: DisciplinasEditaisQuestoes_cargo_id.sql).
 */

IF COL_LENGTH('dbo.DisciplinasEditaisQuestoes', 'peso') IS NULL
    ALTER TABLE dbo.DisciplinasEditaisQuestoes ADD peso DECIMAL(6, 2) NULL;
GO

IF OBJECT_ID('dbo.CK_DisciplinasEditaisQuestoes_peso', 'C') IS NULL
    ALTER TABLE dbo.DisciplinasEditaisQuestoes
        ADD CONSTRAINT CK_DisciplinasEditaisQuestoes_peso CHECK (peso IS NULL OR peso > 0);
GO

IF COL_LENGTH('dbo.DisciplinasEditaisQuestoes', 'desempate') IS NULL
    ALTER TABLE dbo.DisciplinasEditaisQuestoes ADD desempate TINYINT NULL;
GO

IF OBJECT_ID('dbo.CK_DisciplinasEditaisQuestoes_desempate', 'C') IS NULL
    ALTER TABLE dbo.DisciplinasEditaisQuestoes
        ADD CONSTRAINT CK_DisciplinasEditaisQuestoes_desempate CHECK (desempate IS NULL OR desempate > 0);
GO

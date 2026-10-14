/*
 * Especificação 005 — encerramento do modelo anterior (plano 005, §4).
 *
 * Rodar DEPOIS da db/015, em CADA banco: IDECAN_v2 e IDIB_v2 (sqlcmd -I -f 65001). É idempotente.
 *
 * O que faz:
 *   1. Edital que está num concurso com cronograma e ainda não segue nenhum passa a seguir o do concurso.
 *   2. Edital com fases do modelo antigo (sem grupo) e nenhum grupo ganha o grupo "Todos os cargos"
 *      com todos os cargos dele, e essas fases passam a ter esse grupo.
 *   3. Relatórios (não mudam nada):
 *        a. editais que seguem cronograma: chamar POST /v1/{instituicao}/editais/{id}/cronograma/sincronizar
 *           em cada um. A ponte cria as fases de execução que faltam, adota a fase antiga quando há só uma
 *           daquele tipo no edital e aponta as publicações para o cronograma;
 *        b. editais com mais de uma fase do mesmo tipo de execução (modelo de uma fase por grupo): a ponte
 *           não escolhe sozinha; juntar à mão (ou manter como legado);
 *        c. editais com fases e sem cronograma: legado que ainda digita datas. Depois da data em
 *           fase.legado.encerramento (application.properties), as datas digitadas são recusadas.
 */

SET XACT_ABORT ON;
BEGIN TRANSACTION;

/* 1. Editais de concurso com cronograma passam a segui-lo (antes da db/017, que tira concurso_id). */
IF COL_LENGTH('dbo.cronograma', 'concurso_id') IS NOT NULL AND OBJECT_ID('dbo.concurso_edital', 'U') IS NOT NULL
    EXEC(N'
        INSERT INTO dbo.cronograma_edital (cronograma_id, edital_id, versao, created_at, updated_at)
        SELECT c.id, ce.edital_id, 0, SYSDATETIMEOFFSET(), SYSDATETIMEOFFSET()
        FROM dbo.cronograma c
        JOIN dbo.concurso_edital ce ON ce.concurso_id = c.concurso_id
        WHERE NOT EXISTS (SELECT 1 FROM dbo.cronograma_edital x WHERE x.edital_id = ce.edital_id);');

/* 2. Grupo "Todos os cargos" para editais com fase sem grupo e nenhum grupo. */
DECLARE @editais TABLE (edital_id INT PRIMARY KEY);
INSERT INTO @editais (edital_id)
SELECT DISTINCT f.edital_id
FROM dbo.fases f
WHERE NOT EXISTS (SELECT 1 FROM dbo.fase_grupo_cargos g WHERE g.fases_id = f.id)
  AND NOT EXISTS (SELECT 1 FROM dbo.grupo_cargos gc WHERE gc.edital_id = f.edital_id);

INSERT INTO dbo.grupo_cargos (edital_id, nome, created_at, updated_at)
SELECT e.edital_id, N'Todos os cargos', SYSDATETIMEOFFSET(), SYSDATETIMEOFFSET()
FROM @editais e;

INSERT INTO dbo.cargo_grupo (grupo_cargos_id, cargo_id)
SELECT gc.id, c.carId
FROM @editais e
JOIN dbo.grupo_cargos gc ON gc.edital_id = e.edital_id AND gc.nome = N'Todos os cargos'
JOIN dbo.Cargo c ON c.ediId = e.edital_id
WHERE NOT EXISTS (SELECT 1 FROM dbo.cargo_grupo x WHERE x.cargo_id = c.carId);

INSERT INTO dbo.fase_grupo_cargos (fases_id, grupo_cargos_id)
SELECT f.id, gc.id
FROM @editais e
JOIN dbo.grupo_cargos gc ON gc.edital_id = e.edital_id AND gc.nome = N'Todos os cargos'
JOIN dbo.fases f ON f.edital_id = e.edital_id
WHERE NOT EXISTS (SELECT 1 FROM dbo.fase_grupo_cargos g WHERE g.fases_id = f.id);

SELECT COUNT(*) AS grupos_todos_os_cargos_criados FROM @editais;

COMMIT TRANSACTION;

/* 3a. Editais para sincronizar (POST /v1/{instituicao}/editais/{edital_id}/cronograma/sincronizar). */
SELECT x.edital_id, x.cronograma_id
FROM dbo.cronograma_edital x
ORDER BY x.edital_id;

/* 3b. Editais com mais de uma fase do mesmo tipo de execução: a ponte não adota nenhuma delas. */
SELECT f.edital_id, te.valor AS tipo_etapa, COUNT(DISTINCT f.id) AS fases
FROM dbo.fases f
JOIN dbo.etapa e ON e.fases_id = f.id
JOIN dbo.dom_tipo_etapa te ON te.id = e.tipo_id
WHERE te.valor IN (N'prova_objetiva', N'prova_discursiva', N'taf')
  AND NOT EXISTS (SELECT 1 FROM dbo.fase_cronograma fc WHERE fc.fases_id = f.id)
GROUP BY f.edital_id, te.valor
HAVING COUNT(DISTINCT f.id) > 1
ORDER BY f.edital_id, te.valor;

/* 3c. Legado restante: editais com fases e sem cronograma. */
SELECT DISTINCT f.edital_id
FROM dbo.fases f
WHERE NOT EXISTS (SELECT 1 FROM dbo.cronograma_edital x WHERE x.edital_id = f.edital_id)
ORDER BY f.edital_id;

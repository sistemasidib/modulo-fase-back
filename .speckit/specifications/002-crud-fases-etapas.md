# Especificação 002 — CRUD de Fases e Etapas

**Fase do produto:** 1 · **Status:** Rascunho · **Autor:** Yuri Farias · **Data:** 2026-09-23
**Fonte:** pedido de Yuri Farias (23/09/2026): expor o cadastro de `Fase` e `Etapa` dos models do
`sys-core` com controllers, use cases e DTOs.

## 1. Contexto e problema
As tabelas `dbo.fases` e `dbo.etapa` já existem nos bancos IDECAN e IDIB (entidades do
`sys-core`), mas nenhum serviço permite criar, consultar, editar ou excluir esses registros.
A especificação 001 (geração automática por grupo de cargos) depende de pendências de negócio.
Enquanto elas não são respondidas, o operador precisa manter fases e etapas manualmente.

## 2. Objetivo
Oferecer um cadastro (criar, listar, buscar, editar, excluir) de fases por edital e de
etapas por fase, nos dois bancos, respeitando as restrições que já existem no modelo.

## 3. Fora do escopo
- Grupos de cargos, geração automática, espaços de publicação, datas relativas D±N e
  cláusula de barreira: são da especificação 001 e dependem das pendências dela.
- Status de fase/etapa com regra de transição (`DomStatusFase`/`DomStatusEtapa`): o campo
  `statusId` da etapa é gravado como veio, sem validar transição.
- Cadastro das tabelas de domínio (`dom_tipo_etapa`, `dom_status_*`).
- Autenticação e permissões: seguem a pendência "Permissões" da 001.
- Mover uma fase para outro edital ou uma etapa para outra fase.

## 4. Glossário
| Termo | Definição |
|-------|-----------|
| Instituição | `idecan` ou `idib`. Cada uma tem seu banco. Toda operação diz em qual instituição acontece. |
| Fase | Registro de `dbo.fases`, ligado a um edital, com ordem única dentro do edital. |
| Etapa | Registro de `dbo.etapa`, ligado a uma fase. Pode nascer sem datas. |
| Etapa de elegibilidade | Etapa que é pré-requisito de outra (`elegibilidade_etapa_id`). |

## 5. Regras de negócio
- **RN01 – Instituição explícita.** Toda operação indica a instituição. Dados de uma
  instituição nunca são lidos nem gravados na outra.
- **RN02 – Fase pertence a um edital existente.** Criar ou listar fases de um edital que
  não existe é recusado.
- **RN03 – Ordem única por edital.** Duas fases do mesmo edital não podem ter a mesma ordem.
  A ordem começa em 1.
- **RN04 – Dados mínimos da fase.** Nome (até 150 caracteres), ordem, data de abertura e data
  de fechamento são obrigatórios. O fechamento não pode ser anterior à abertura.
- **RN05 – Etapa pertence a uma fase existente.** Criar ou listar etapas de uma fase que não
  existe é recusado.
- **RN06 – Dados mínimos da etapa.** Nome (até 150 caracteres) e tipo são obrigatórios. As
  datas são opcionais. Quando as duas existem, o fechamento não pode ser anterior à abertura.
  As flags `reprovavel`, `obrigatoriaParaFase`, `todosCandidatos` e `aceitaDocumentos` valem
  "não" quando omitidas.
- **RN07 – Pré-requisito válido.** A etapa de elegibilidade, quando informada, precisa existir
  e não pode ser a própria etapa.
- **RN08 – Vínculo fixo.** Editar não muda o edital da fase nem a fase da etapa.
- **RN09 – Exclusão protegida.** Não é possível excluir uma fase que tenha etapas ou candidatos,
  nem uma etapa que tenha candidatos, documentos ou recursos vinculados.

## 6. Histórias de usuário
- **HU01.** Como operador do edital, quero cadastrar e ordenar as fases de um edital para
  montar a estrutura do concurso enquanto a geração automática não existe.
- **HU02.** Como operador do edital, quero cadastrar as etapas de cada fase, com tipo e datas,
  para que os módulos de inscrição, ensalamento e resultado as encontrem.
- **HU03.** Como operador do edital, quero ser impedido de excluir fases e etapas em uso para
  não perder dados de candidatos.

## 7. Critérios de aceite
| ID | Regra | Critério (Dado / Quando / Então) |
|----|-------|----------------------------------|
| CA01 | RN01 | Dado um edital 10 no IDECAN e outro edital 10 no IDIB, quando crio a fase de ordem 1 nos dois, então as duas são aceitas e cada listagem mostra só a sua. |
| CA02 | RN02 | Dado um edital inexistente, quando crio ou listo fases dele, então recebo "não encontrado" (404). |
| CA03 | RN03 | Dado um edital com fase de ordem 1, quando crio outra fase de ordem 1, então recebo conflito (409). |
| CA04 | RN03 | Dadas as fases de ordem 1 e 2, quando edito a 2 para ordem 1, então recebo conflito (409). Editar a fase mantendo a própria ordem é aceito. |
| CA05 | RN04 | Dado um pedido sem nome, sem datas, com ordem 0 ou com fechamento antes da abertura, quando crio a fase, então recebo erro de validação (400) com a mensagem do problema. |
| CA06 | RN03 | Dado um edital com fases de ordem 2 e 1, quando listo as fases, então vêm em ordem crescente. |
| CA07 | RN05 | Dada uma fase inexistente, quando crio ou listo etapas dela, então recebo 404. |
| CA08 | RN06 | Dado um pedido só com nome e tipo, quando crio a etapa, então ela é criada sem datas e com as quatro flags em "não". |
| CA09 | RN06 | Dado um pedido sem nome ou sem tipo, quando crio a etapa, então recebo 400. |
| CA10 | RN07 | Dado um pré-requisito inexistente, quando crio ou edito a etapa, então recebo 404. Dado o pré-requisito igual à própria etapa, recebo 400. |
| CA11 | RN08 | Dada uma fase do edital 10, quando a edito, então ela continua no edital 10. Dada uma etapa da fase 5, quando a edito, então continua na fase 5. |
| CA12 | RN09 | Dada uma fase com etapas, quando a excluo, então recebo conflito (409) e nada é apagado. |
| CA13 | RN09 | Dada uma etapa com candidatos vinculados, quando a excluo, então recebo conflito (409) e nada é apagado. |
| CA14 | — | Dado um id inexistente, quando busco, edito ou excluo a fase/etapa, então recebo 404. |

## 8. Pendências
- [ ] `[A DEFINIR]` **Ordem das etapas**: `dbo.etapa` não tem coluna de ordem; a listagem usa o
  id. É suficiente? Responsável: Yuri.
- [ ] `[A DEFINIR]` **Pré-requisito na mesma fase**: a etapa de elegibilidade precisa ser da
  mesma fase ou do mesmo edital? Hoje só se exige que exista. Responsável: Cauet.
- [ ] `[A DEFINIR]` **Dados legados inválidos**: se houver fase/etapa antiga que viole RN04/RN06
  (ex.: fechamento antes da abertura), a leitura falha com 400. Corrigir os dados ou relaxar a
  leitura? Responsável: Yuri.
- [ ] `[A DEFINIR]` **Permissões**: mesma pendência da especificação 001. Responsável: Cauet.

## 9. Notas técnicas para o plano
| # | Situação atual | Impacto |
|---|----------------|---------|
| N1 | Entidades `Fase`/`Etapa` existem em `idecan` e `idib` com os mesmos campos, em pacotes diferentes. | Um adapter por instituição, com o mesmo código. |
| N2 | `Etapa.tipoId` e `statusId` são `Integer` (Dom* não mapeado). | A existência do tipo é garantida pela FK do banco, se houver; sem FK, o valor é gravado como veio. |
| N3 | A especificação 001 (N1) prevê mudar a UK da fase para `(grupo_id, ordem)`. | RN03 deste CRUD muda quando a 001 for implementada. |

## 10. Dependências e responsáveis
| Responsável | Item |
|-------------|------|
| Yuri Farias | Implementação e testes |
| Gustavo Nunes | Validação em homologação (IDECAN e IDIB) |

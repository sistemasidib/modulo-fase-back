# Tarefas 001 — Fases e Etapas do Edital

**Plano:** `../plans/001-fases-e-etapas.md` · **Data:** 2026-09-23

Legenda: `[ ]` pendente · `[x]` concluída · `[P]` pode rodar em paralelo com as vizinhas.

## Preparação
- [x] T001 — Script `db/001-fases-e-etapas.sql` (tabelas + valores de domínio) · atende: respostas 2, 5, 7, 15 · pronto quando: roda duas vezes sem erro
- [ ] T002 — **Yuri:** rodar o script em homologação IDECAN_v2 e IDIB_v2 · pronto quando: consulta de conferência mostra 14 / 5 / 31 linhas
- [x] T003 — Entidades no sys-core (`features/fase_etapas`), IDECAN e IDIB · pronto quando: `mvnw install` do sys-core ok
- [ ] T004 — **Yuri:** commitar e publicar o sys-core depois do T002

## Núcleo
- [x] T010 — Domínio `Publicacao` (publicar abre recurso, datas manuais) · atende: respostas 1, 3, 4, 5, 6 · pronto quando: `PublicacaoTest` verde
- [x] T011 — Espaços de publicação na criação de fase e etapa · atende: RN05, resposta 2 · pronto quando: CA08, CA09 verdes
- [x] T012 [P] — Grupos de cargos e "agrupar cargo" a qualquer momento · atende: RN02, respostas 9, 10 · pronto quando: `GrupoCargoUseCasesTest` verde
- [x] T013 — Geração por múltipla escolha de tipos · atende: RN03, RN04, resposta 8 · pronto quando: CA05, CA06 verdes
- [x] T014 — Exclusão de fase/etapa só com espaços vazios (provisório, pergunta 11)

## Integração e acabamento
- [x] T020 — Controllers `GrupoCargoController`, `PublicacaoController`, `TipoController`; `GET /fases?grupoCargoId=`
- [ ] T021 — Testar os endpoints no Swagger contra homologação, nas duas instituições

## Bloqueadas (aguardam validação)
- [ ] T030 — Remoção de etapa com publicação (pergunta 11)
- [ ] T031 — D±N e recálculo, ou retirada de RN07/RN12 (perguntas 13, 14) + emenda de P4
- [ ] T032 — Duração padrão do recurso por tipo (pergunta 5)
- [ ] T033 — Onde guardar o documento publicado
- [ ] T034 — `etapa_id` → `fase_id` nas solicitações (técnica)

## Validação
- [ ] T040 — Criar `../validation/001-fases-e-etapas.md` com evidências

# Tarefas 002 — CRUD de Fases e Etapas

**Plano:** `../plans/002-crud-fases-etapas.md` · **Data:** 2026-09-23

Legenda: `[ ]` pendente · `[x]` concluída · `[P]` pode rodar em paralelo com as vizinhas.

## Preparação
- [x] T001 — Projeto Quarkus (`pom.xml`, Maven wrapper, `application.properties` com PUs `idecan`/`idib` do sys-core) · atende: RN01 · pronto quando: `mvnw package` gera `target/quarkus-app`
- [ ] T002 — Aprovar a spec 002 e resolver os desvios P3/P9 do plano (emenda ou mover controllers) · pronto quando: status "Aprovada" e constituição coerente com o código

## Núcleo
- [x] T010 — Modelos `Fase`, `Etapa`, `Instituicao` com invariantes · atende: RN04, RN06, RN07 · pronto quando: `FaseTest`, `EtapaTest` verdes
- [x] T011 — Portas `FaseRepository`, `EtapaRepository` · atende: RN01
- [x] T012 [P] — Use cases de fase (criar, listar, buscar, atualizar, excluir) · atende: RN02, RN03, RN08 · pronto quando: `FaseUseCasesTest` verde
- [x] T013 [P] — Use cases de etapa (criar, listar, buscar, atualizar, excluir) · atende: RN05, RN07, RN08 · pronto quando: `EtapaUseCasesTest` verde
- [x] T014 — Adapters Panache IDECAN e IDIB + tradução de FK/UK em 409 · atende: RN01, RN09 · pronto quando: build do Quarkus sem erro de injeção

## Integração e acabamento
- [x] T020 — DTOs, `FaseController`, `EtapaController`, `ErrosHttp` · atende: CA02, CA03, CA05, CA14
- [ ] T021 — Consultar em homologação se há fases/etapas legadas que violam RN04/RN06 · atende: pendência "Dados legados" · pronto quando: consulta registrada e decisão tomada
- [ ] T022 — Rodar o serviço contra homologação e exercitar os 10 endpoints pelo Swagger (`/q/swagger-ui`) · pronto quando: roteiro da validação executado

## Validação
- [ ] T030 — Preencher `../validation/002-crud-fases-etapas.md` com evidências de CA01–CA14

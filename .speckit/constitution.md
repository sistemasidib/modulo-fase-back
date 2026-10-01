# Constituição do Projeto — Módulo Fase (Controle de Demanda)

> Princípios que **não se negociam** por feature. Toda especificação, plano, tarefa e
> validação deve estar de acordo com este documento. Se algo aqui precisar mudar,
> altere a constituição primeiro (com registro no histórico ao final), e só depois o
> restante.

**Versão:** 1.2.0 · **Ratificada em:** 2026-09-23 · **Fonte de negócio:** `docs/Controle de Demanda - Fase 1.pdf`

---

## 1. Propósito

O Controle de Demanda existe para controlar publicações, prazos públicos e obrigações
internas de cada setor em um concurso, evitando erros operacionais como **publicar fora
de ordem** ou **com dados incompletos**. O produto é entregue em 4 fases sequenciais:

| Fase | Foco | Comportamento de restrições |
|------|------|-----------------------------|
| 1 | Estrutura de fases e etapas | Nenhuma automação, sem alertas/bloqueios |
| 2 | Cronograma e demandas por setor | Restrições geram **alerta** |
| 3 | Integração e automação | Alertas viram **bloqueio** |
| 4 | Projeção e IA | Projeções com backtest e nível de confiança |

## 2. Princípios

### P1 — A regra de negócio é a fonte da verdade
Todo comportamento implementado deve ser rastreável a uma regra de negócio (`RNxx`) ou a
um critério de aceite documentado. Código sem regra correspondente não entra; regra sem
código correspondente é uma pendência explícita.

### P2 — Disciplina de escopo por fase
Uma feature pertence a **uma** fase. Não se antecipa escopo de fases futuras
(ex.: nada de alertas ou bloqueios na Fase 1). Cada fase só começa depois que a anterior
foi validada, com 1 a 2 semanas de testes e ajustes entre elas.

### P3 — Especificação antes de código
Nenhuma implementação começa sem uma especificação aprovada em `specifications/` e um
plano em `plans/`. Ambiguidades são marcadas como `[A DEFINIR]` e resolvidas antes de
virarem tarefa — nunca "chutadas" no código.

### P4 — Datas são relativas, determinísticas e em dias corridos
- A data da etapa (Dia D) é a âncora; tudo o que depende dela guarda um deslocamento `D±N`.
- Deslocamentos são sempre em **dias corridos**.
- Datas dependentes são **derivadas**, nunca digitadas: mudar a âncora recalcula tudo, sem ação manual.
- Deslocamentos têm padrão, mas são parametrizáveis **por edital**, sem afetar outros editais.
- O cálculo de datas é puro e coberto por testes com os exemplos oficiais
  (prova 20/10 → locais 11/10, gabarito 21/10; prova 22/10 → locais 13/10, gabarito 23/10).

### P5 — Isolamento entre grupos e editais
Alterações em um grupo de cargos não afetam outros grupos; alterações em um edital não
afetam outros editais. Um cargo pertence a exatamente um grupo.

### P6 — Testes provam os critérios de aceite
Cada critério de aceite vira pelo menos um teste automatizado ou, quando impossível, um
roteiro de validação manual em `validation/`. Regras de negócio críticas (datas,
agrupamento, geração automática) são escritas com teste primeiro.

### P7 — Simplicidade e clean code
Resolver o problema da fase atual da forma mais simples que respeite os princípios.
- Nomes honestos, funções curtas, um motivo claro para mudar.
- **Sem introspecções sem sentido:** não inventar camadas, wrappers, facades ou
  “arquitetura genérica” que não carreguem regra de negócio ou adaptação real.
- Abstrações “para o futuro” só entram quando a fase que precisa delas chegar.

### P8 — Idioma
Documentação e termos de domínio em **português** (edital, cargo, grupo, fase, etapa,
publicação, recurso, cláusula de barreira). Identificadores de código seguem a convenção
Java (pacotes em inglês estrutural; linguagem ubíqua do domínio em português quando
expressa entidades/regras).

### P9 — Arquitetura DDD em microsserviço Quarkus
O módulo é um **microsserviço Quarkus (Java)** organizado por **Domain-Driven Design**
com exatamente três níveis. Nada além disso sem emenda à constituição.

| Nível | Pacote típico | Responsabilidade | Pode depender de |
|-------|---------------|------------------|------------------|
| **domain** | `...domain` | Entidades, value objects, agregados, invariantes, portas (interfaces) | Nada externo (sem Quarkus, JPA, REST, HTTP) |
| **application** | `...application` | Casos de uso / application services, orquestração, DTOs de entrada/saída | `domain` |
| **infrastructure** | `...infrastructure` | Persistência (Panache/JPA), REST, mensageria, clientes, adapters das portas | `application`, `domain` |

Regras de dependência:
1. Dependências apontam **para dentro**: `infrastructure` → `application` → `domain`.
2. Regras de negócio e cálculo de datas vivem no **domain** (ou application só como
   orquestração, nunca duplicando regra).
3. Controllers/resources REST e entidades de persistência **não** carregam regra de negócio.
4. Um bounded context por microsserviço; integração com `idecan`/`idib` via adapters
   em `infrastructure`, sem vazar detalhes de schema para o domínio.

## 3. Restrições técnicas

| Item | Decisão |
|------|---------|
| Forma de entrega | Microsserviço |
| Runtime / framework | **Quarkus** (Java) |
| Arquitetura | DDD + clean code; camadas `domain` / `application` / `infrastructure` |
| Persistência | Hibernate ORM com Panache (`PanacheEntityBase`), apenas em `infrastructure` |
| Datasources / PUs | Dois isolados: `idecan` e `idib` (schema `dbo`), com entidades espelhadas de mesmo nome em pacotes distintos (`...infrastructure.persistence.idecan` / `...idib`) |
| Convenções de modelo | Tabelas novas do módulo de fases usam PK `id` e FKs em snake_case (ex.: `fases_id`). Tabelas de domínio estendem `DominioBase` (`id`, `valor`, `descricao`) e usam o prefixo `Dom*`. PKs legadas (`ediId`, `carId`) não são renomeadas |
| Testes | Domain: unitários puros. Application: unitários com portas mockadas. Infrastructure: `@QuarkusTest` / integração. `[A DEFINIR]` ferramentas exatas |
| Regras de alerta/bloqueio (Fase 2+) | Novo microsserviço dedicado (mesma constituição de camadas) |

> Referência do modelo atual: `docs/MODELS_IDECAN_IDIB.md`. Os planos devem citar quais
> entidades existentes reutilizam, alteram ou criam, e em qual datasource.

## 4. Governança

- **Emendas:** qualquer mudança nesta constituição exige PR próprio, incremento de
  versão (semver: MAJOR remove/redefine princípio, MINOR adiciona, PATCH esclarece) e
  entrada no histórico abaixo.
- **Conflito:** se uma especificação contradiz a constituição, a constituição vence até
  ser emendada.
- **Revisão:** planos devem conter uma seção "Checagem de constituição" confirmando
  aderência a P1–P9.

## 5. Histórico

| Versão | Data | Mudança |
|--------|------|---------|
| 1.0.0 | 2026-09-22 | Versão inicial |
| 1.1.0 | 2026-09-22 | §3 preenchida com a stack e as convenções de `docs/MODELS_IDECAN_IDIB.md` |
| 1.2.0 | 2026-09-23 | Microsserviço Quarkus; DDD com camadas domain/application/infrastructure; P7 reforçado (clean code, sem introspecções sem sentido); novo P9 |

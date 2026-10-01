# SpecKit — Desenvolvimento guiado por especificação

Este diretório organiza o trabalho do módulo em uma sequência fixa de artefatos. Cada
etapa só começa quando a anterior está aprovada.

```
constitution.md ──► specifications/ ──► plans/ ──► tasks/ ──► código ──► validation/
   (princípios)        (O QUÊ/POR QUÊ)    (COMO)    (PASSOS)              (PROVA)
```

## Estrutura

| Caminho | Conteúdo | Pergunta que responde |
|---------|----------|-----------------------|
| `constitution.md` | Princípios inegociáveis do projeto | Quais regras valem sempre? |
| `specifications/` | Requisitos da feature, regras de negócio, critérios de aceite | O que construir e por quê? |
| `plans/` | Arquitetura, modelo de dados, decisões técnicas | Como construir? |
| `tasks/` | Lista ordenada de tarefas pequenas e verificáveis | Em que ordem, e o que é "pronto"? |
| `validation/` | Evidência de que cada critério de aceite foi atendido | Funcionou mesmo? |

Cada pasta tem um `_template.md`. Copie-o para criar um novo artefato.

## Convenção de nomes

Todos os artefatos de uma feature compartilham o mesmo identificador `NNN-slug`:

```
specifications/001-fases-e-etapas.md
plans/001-fases-e-etapas.md
tasks/001-fases-e-etapas.md
validation/001-fases-e-etapas.md
```

## Fluxo

1. **Constituição** — leia `constitution.md`. Ela vale para toda feature; mude-a só por emenda.
2. **Especificar** — copie `specifications/_template.md`. Descreva o problema, as regras
   (`RNxx`) e os critérios de aceite **sem falar de tecnologia**. Marque dúvidas como
   `[A DEFINIR]`. Status: `Rascunho` → `Aprovada`.
3. **Clarificar** — resolva todos os `[A DEFINIR]` com o responsável de negócio antes de
   seguir. Especificação com pendência não gera plano.
4. **Planejar** — copie `plans/_template.md`. Defina modelo de dados, componentes,
   contratos e riscos. Preencha a *Checagem de constituição*.
5. **Quebrar em tarefas** — copie `tasks/_template.md`. Tarefas pequenas (≤ 1 dia),
   ordenadas por dependência, cada uma ligada a `RNxx`/critério de aceite. Marque com
   `[P]` as que podem rodar em paralelo.
6. **Implementar** — execute as tarefas em ordem, marcando `[x]` conforme conclui.
   Testes dos critérios de aceite antes ou junto com o código.
7. **Validar** — copie `validation/_template.md`. Para cada critério de aceite, registre
   como foi verificado (teste automatizado ou roteiro manual) e o resultado. Feature
   só está pronta com 100% dos critérios aprovados.

## Regras de ouro

- Rastreabilidade ponta a ponta: `RNxx` → critério de aceite → tarefa → teste → validação.
- Mudou o requisito? Atualize a **especificação primeiro**, depois plano, tarefas e validação.
- Nada de escopo de fase futura (ver constituição, P2).

## Referências

- Regras de negócio da Fase 1: `docs/Controle de Demanda - Fase 1.pdf`
- Modelo de dados atual (entidades JPA IDECAN/IDIB): `docs/MODELS_IDECAN_IDIB.md`

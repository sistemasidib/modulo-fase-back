# Plano 001 — Fases e Etapas do Edital

**Especificação:** `../specifications/001-fases-e-etapas.md` (decisões em §8.1) · **Status:** Rascunho · **Data:** 2026-09-23

Cobre só as pendências já respondidas. D±N e recálculo (RN07, RN12) ficam fora até as perguntas 13 e 14.

## 1. Resumo da abordagem
Estende o CRUD da spec 002. O usuário monta grupos de cargos, escolhe as fases e os tipos de
etapa de cada fase (múltipla escolha), e o sistema gera tudo com os espaços de publicação.
Todas as datas de publicação são manuais. Publicar exige o período de visualização e, para
gabarito/resultado preliminar, o período de recurso; o recurso fica aberto dentro dele.
Nenhuma tabela existente é alterada.

## 2. Checagem de constituição
| Princípio | Aderente? | Observação |
|-----------|-----------|------------|
| P1 | ☑ | Cada regra cita a pergunta respondida (§8.1 da spec). |
| P2 | ☑ | Sem alertas, bloqueios ou cronograma. Publicar abre recurso é regra da Fase 1 (resposta 1). |
| P3 | ☒ | Spec ainda em Rascunho; implementado só o decidido. Aprovar a spec após as perguntas abertas. |
| P4 | ☒ | **Conflito:** a resposta 3/4/6/12 torna as datas manuais, e P4 exige datas derivadas D±N. Emendar P4 se as perguntas 13/14 confirmarem. |
| P5 | ☑ | Grupos isolados; cargo em um grupo só (UK). |
| P6 | ☑ | Testes de domínio e de use case (57 no total). Banco real: roteiro manual. |
| P7 | ☑ | Espaços padrão parametrizados em tabela, sem código por tipo. |
| P8 | ☑ | |
| P9 | ☒ | Controllers em `application.controllers` (mesmo desvio do plano 002). |

## 3. Modelo de dados
Script: `db/001-fases-e-etapas.sql` (IDECAN_v2 e IDIB_v2). Entidades no sys-core, pacotes `idecan` e `idib`.

| Tabela | Entidade sys-core | Papel |
|--------|-------------------|-------|
| `dom_tipo_etapa` (+14 valores) | `faseDominio.DomTipoEtapa` (IDIB novo) | Tipos de etapa (respostas 5 e 7) |
| `dom_tipo_publicacao` (+5 valores) | `faseDominio.DomTipoPublicacao` | Local de prova, gabaritos, resultados; `abre_recurso` |
| `dom_tipo_etapa_publicacao` | `faseDominio.DomTipoEtapaPublicacao` | Espaços criados por tipo de etapa |
| `grupo_cargo` | `dbo.GrupoCargo` | Grupo do edital (UK edital + nome) |
| `grupo_cargo_cargo` | `dbo.GrupoCargoCargo` | Cargo no grupo (UK `cargo_id`) |
| `fase_grupo_cargo` | `dbo.FaseGrupoCargo` | Fase gerada para o grupo (sem alterar `dbo.fases`) |
| `publicacao` | `dbo.Publicacao` | Espaço da fase (`etapa_id` nulo) ou da etapa; datas manuais, `publicada_em` |

A UK `(edital_id, ordem)` de `dbo.fases` continua: a ordem é contínua no edital (grupo A: 1–2, grupo B: 3–4).

## 4. Componentes
| Camada | Novos componentes |
|--------|-------------------|
| domain | `GrupoCargo`, `Cargo`, `TipoEtapa`, `TipoPublicacao`, `Publicacao` (publicar, recurso aberto, preenchida); portas `GrupoCargoRepository`, `PublicacaoRepository`, `TipoRepository`; `FaseRepository` + `listarPorGrupo`, `maiorOrdem` |
| application | Use cases de grupo (criar, listar, renomear, excluir, adicionar/remover cargo, cargos sem grupo), `GerarFasesDoGrupo`, `ListarFasesDoGrupo`, publicações (listar, criar, atualizar, publicar, excluir), `ListarTipos`, `EspacosDePublicacao`; `CriarFase`/`CriarEtapa` criam espaços; `ExcluirFase`/`ExcluirEtapa` removem espaços vazios |
| infrastructure | `Idecan*`/`Idib*` Grupos, Publicacoes, Tipos + adapters de roteamento |

## 5. Contratos (novos)
`{i}` = `idecan` | `idib`.

| Método | Caminho | Uso |
|--------|---------|-----|
| GET | `/v1/{i}/tipos/etapa` · `/v1/{i}/tipos/publicacao` | Opções de múltipla escolha |
| GET / POST | `/v1/{i}/grupos-cargo?editalId=` | Listar / criar grupo `{nome}` |
| PUT / DELETE | `/v1/{i}/grupos-cargo/{id}` | Renomear / excluir (409 se tiver fases) |
| GET | `/v1/{i}/grupos-cargo/cargos-sem-grupo?editalId=` | Botão "agrupar cargo" |
| PUT / DELETE | `/v1/{i}/grupos-cargo/{id}/cargos/{cargoId}` | Colocar / tirar cargo (409 se estiver em outro grupo) |
| POST | `/v1/{i}/grupos-cargo/{id}/gerar-fases` | `{fases:[{nome, dataAbertura, dataFechamento, etapas:[{tipoId, nome?}]}]}` |
| GET | `/v1/{i}/fases?grupoCargoId=` | Fases do grupo |
| GET / POST | `/v1/{i}/publicacoes?faseId=` | Publicações da fase e das etapas / espaço extra `{tipoPublicacaoId, etapaId?}` |
| PUT | `/v1/{i}/publicacoes/{id}` | Datas `{dataPrevista, visualizacaoInicio/Fim, recursoInicio/Fim}` |
| POST | `/v1/{i}/publicacoes/{id}/publicar` | Mesmo corpo; publica e abre o recurso |
| DELETE | `/v1/{i}/publicacoes/{id}` | Só se não publicada |

## 6. Decisões técnicas
| Decisão | Alternativas | Motivo |
|---------|--------------|--------|
| Vínculo fase→grupo em tabela própria | Coluna `grupo_cargo_id` em `dbo.fases` | Não mexer em tabela usada por outros módulos; editais antigos intactos (resposta 17). |
| Ordem da fase contínua no edital | Trocar a UK para `(edital, grupo, ordem)` | Evita ALTER na UK existente. |
| Subtipos de análise como tipos de etapa | Tabela de subtipos | Uma tabela só; cada subtipo pode ter espaços próprios. |
| Espaços padrão em `dom_tipo_etapa_publicacao` | `switch` no código | Cauet ajusta os espaços por tipo sem deploy (resposta 6). |
| Exclusão recusa espaço preenchido | Apagar tudo; pedir confirmação | Mais seguro enquanto a pergunta 11 está aberta. |

## 7. Estratégia de testes
- `PublicacaoTest`: publicar, janela do recurso (exemplo 22/10 00:00 a 30/10 23:59), validações.
- `GrupoCargoUseCasesTest`: agrupamento, CA04, geração, CA06, ordem contínua.
- `PublicacaoUseCasesTest`, `EtapaUseCasesTest`, `FaseUseCasesTest`: CA08, CA09, espaços da fase, publicar abre recurso, exclusão protegida.
- Manual (homologação, IDECAN e IDIB): rodar o script, gerar um grupo e publicar um gabarito.

## 8. Riscos
- `dom_tipo_etapa` já existir no IDECAN com outros valores → o script só insere valores ausentes; revisar duplicidades de significado.
- O sys-core com as entidades novas precisa ser publicado **depois** de rodar o script: entidades sem tabela quebram só quem as consulta, mas é melhor não arriscar.

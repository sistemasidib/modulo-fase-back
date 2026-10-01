# Plano 002 — CRUD de Fases e Etapas

**Especificação:** `../specifications/002-crud-fases-etapas.md` · **Status:** Rascunho · **Data:** 2026-09-23

## 1. Resumo da abordagem
Microsserviço Quarkus 3.24.5 (Java 21) que usa as entidades `Fase`/`Etapa` do
`bio-registro-core` 9.9.9.9 (sys-core) nos dois persistence units (`idecan`, `idib`).
O domínio tem modelos próprios (records) com as invariantes. Os use cases orquestram as
checagens de existência e unicidade. Os adapters Panache traduzem entre domínio e entidade.

## 2. Checagem de constituição
| Princípio | Aderente? | Observação |
|-----------|-----------|------------|
| P1 Regra de negócio é a fonte da verdade | ☑ | Cada checagem cita RN01–RN09 da spec 002. |
| P2 Disciplina de escopo por fase | ☑ | Nada de alerta, bloqueio, grupo ou D±N. |
| P3 Especificação antes de código | ☒ | **Desvio:** código escrito com a spec em Rascunho, a pedido do Yuri (23/09). Aprovar a spec 002 e ajustar o código se algo mudar. |
| P4 Datas relativas em dias corridos | n/a | Sem datas relativas neste CRUD. |
| P5 Isolamento entre grupos e editais | ☑ | Isolamento por instituição (RN01) e por edital (RN03). |
| P6 Testes provam os critérios de aceite | ◐ | Domínio e use cases testados sem banco. CA12/CA13 (FK) dependem de banco: roteiro manual. |
| P7 Simplicidade e clean code | ☑ | Um use case por operação, sem interfaces sem segunda implementação. |
| P8 Idioma | ☑ | Domínio em português. |
| P9 DDD Quarkus | ☒ | **Desvio:** os controllers REST ficam em `application.controllers` (padrão dos outros módulos, ex.: `modulo-ensalamento-back`, e pedido do Yuri). A constituição põe REST em `infrastructure`. Resolver com emenda (mover REST para `application` é MINOR) ou mover os controllers. |

## 3. Modelo de dados
Nenhuma tabela nova nem alteração de schema.

| Domínio (`domain.model`) | Entidade sys-core | Tabela | Datasource |
|--------------------------|-------------------|--------|------------|
| `Fase` (id, editalId, nome, descricao, ordem, dataAbertura, dataFechamento) | `entities.idecan.dbo.Fase` / `entities.idib.dbo.Fase` | `dbo.fases` | `idecan` / `idib` |
| `Etapa` (id, faseId, nome, tipoId, descricao, datas, statusId, flags, elegibilidade, responsavelId, publicadaEm) | `entities.idecan.dbo.Etapa` / `entities.idib.dbo.Etapa` | `dbo.etapa` | `idecan` / `idib` |

`created_at`/`updated_at` são preenchidos pelo adapter; não aparecem no domínio.

## 4. Componentes e responsabilidades
Pacote base `br.bioregistro.fase`.

| Camada | Componente | Papel |
|--------|------------|-------|
| domain | `model.Fase`, `model.Etapa`, `model.Instituicao` | Invariantes RN04, RN06, RN07 (auto-referência) |
| domain | `port.FaseRepository`, `port.EtapaRepository` | Portas de persistência |
| domain | `exception.*` | `RegraDeNegocioException` (400), `NaoEncontradoException` (404), `ConflitoException` (409) |
| application | `use_cases.CriarFase`, `ListarFases`, `BuscarFase`, `AtualizarFase`, `ExcluirFase` | RN02, RN03, RN08 |
| application | `use_cases.CriarEtapa`, `ListarEtapas`, `BuscarEtapa`, `AtualizarEtapa`, `ExcluirEtapa` | RN05, RN07, RN08 |
| application | `dtos.FaseRequest/Response`, `EtapaRequest/Response`, `ErroResponse` | Contrato HTTP |
| application | `controllers.FaseController`, `EtapaController`, `ErrosHttp` | Endpoints e tradução de exceções para HTTP |
| infrastructure | `persistence.FaseRepositoryAdapter`, `EtapaRepositoryAdapter` | Implementam as portas e escolhem o banco (RN01) |
| infrastructure | `persistence.idecan.*`, `persistence.idib.*` | Panache sobre as entidades do sys-core |
| infrastructure | `persistence.RestricaoDoBanco` | Converte violação de FK/UK em 409 (RN09) |

## 5. Contratos
`{instituicao}` = `idecan` | `idib`. Erros: `{"message": "..."}`.

| Método | Caminho | Corpo | Sucesso | Erros |
|--------|---------|-------|---------|-------|
| GET | `/v1/{instituicao}/fases?editalId=` | — | 200 lista por ordem | 400, 404 |
| POST | `/v1/{instituicao}/fases?editalId=` | `FaseRequest` | 201 `FaseResponse` | 400, 404, 409 |
| GET | `/v1/{instituicao}/fases/{id}` | — | 200 | 404 |
| PUT | `/v1/{instituicao}/fases/{id}` | `FaseRequest` | 200 | 400, 404, 409 |
| DELETE | `/v1/{instituicao}/fases/{id}` | — | 204 | 404, 409 |
| GET | `/v1/{instituicao}/etapas?faseId=` | — | 200 | 400, 404 |
| POST | `/v1/{instituicao}/etapas?faseId=` | `EtapaRequest` | 201 `EtapaResponse` | 400, 404, 409 |
| GET | `/v1/{instituicao}/etapas/{id}` | — | 200 | 404 |
| PUT | `/v1/{instituicao}/etapas/{id}` | `EtapaRequest` | 200 | 400, 404, 409 |
| DELETE | `/v1/{instituicao}/etapas/{id}` | — | 204 | 404, 409 |

Instituição inválida no caminho: 404 (o JAX-RS trata falha de conversão de path param como 404).

## 6. Decisões técnicas
| Decisão | Alternativas consideradas | Motivo |
|---------|---------------------------|--------|
| Instituição no caminho | Header; um serviço por instituição | Explícito na URL e fácil de testar (RN01). |
| Pai (`editalId`/`faseId`) como query param no POST | Pai no corpo; rota aninhada | O corpo serve igual para criar e editar; o pai não pode ser trocado na edição (RN08). |
| Um adapter por instituição com código espelhado | Reflexão/genéricos sobre as duas entidades | As entidades do sys-core são classes distintas sem interface comum; código espelhado é mais simples de ler. |
| Exclusão protegida pelas FKs do banco | Contar vínculos em cada tabela satélite | O banco já garante; evita esquecer uma tabela nova. |
| Checagem de ordem no use case e também UK no banco | Só a UK | Mensagem clara no caso comum; a UK cobre concorrência. |
| Transação nos use cases de escrita | No adapter | Checagem + gravação na mesma transação. |
| Credenciais só por variável de ambiente | Default no `application.properties` | Não versionar senha. |

## 7. Estratégia de testes
| Tipo | Arquivo | Cobre |
|------|---------|-------|
| Unitário domínio | `domain/model/FaseTest`, `EtapaTest` | CA05, CA08, CA09, CA10 (auto-referência) |
| Unitário use case (repositório em memória) | `application/use_cases/FaseUseCasesTest`, `EtapaUseCasesTest` | CA01–CA04, CA06–CA08, CA10, CA11, CA14 |
| Manual em homologação | `validation/002-crud-fases-etapas.md` | CA12, CA13 e a ida e volta real no IDECAN e no IDIB |

## 8. Riscos e mitigação
- Dados legados que violam as invariantes quebram a leitura → pendência "Dados legados" da spec; levantar com consulta antes de liberar.
- Entidade IDIB `Fase`/`Etapa` pode divergir da IDECAN no sys-core → adapters separados isolam o impacto.
- Spec 001 muda a UK da fase para `(grupo_id, ordem)` → RN03 e `ordemEmUso` serão revistos na 001.

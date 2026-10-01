# Validação 002 — CRUD de Fases e Etapas

**Especificação:** `../specifications/002-crud-fases-etapas.md` · **Validado por:** _(pendente)_ · **Data:** _(pendente)_
**Ambiente:** local (testes automatizados) + homologação IDECAN e IDIB (roteiros manuais)

## Resultado por critério de aceite
| CA | Regra | Método | Evidência | Resultado |
|----|-------|--------|-----------|-----------|
| CA01 | RN01 | auto | `FaseUseCasesTest.mesmaOrdemEmInstituicoesDiferentesNaoConflita` | ✅ (local, 23/09) |
| CA02 | RN02 | auto | `FaseUseCasesTest.naoCriaFaseEmEditalInexistente` | ✅ (local, 23/09) |
| CA03 | RN03 | auto | `FaseUseCasesTest.naoRepeteOrdemNoMesmoEdital` | ✅ (local, 23/09) |
| CA04 | RN03 | auto | `FaseUseCasesTest.atualizarParaOrdemDeOutraFaseConflita`, `atualizaMantendoEditalEPermitindoAPropriaOrdem` | ✅ (local, 23/09) |
| CA05 | RN04 | auto | `FaseTest` | ✅ (local, 23/09) |
| CA06 | RN03 | auto | `FaseUseCasesTest.listaEmOrdemCrescente` | ✅ (local, 23/09) |
| CA07 | RN05 | auto | `EtapaUseCasesTest.naoCriaEtapaEmFaseInexistente` | ✅ (local, 23/09) |
| CA08 | RN06 | auto | `EtapaUseCasesTest.criaEtapaSemDatasComFlagsFalsasPorPadrao` | ✅ (local, 23/09) |
| CA09 | RN06 | auto | `EtapaTest.exigeFaseNomeETipo` | ✅ (local, 23/09) |
| CA10 | RN07 | auto | `EtapaUseCasesTest.preRequisitoPrecisaExistir`, `EtapaTest.etapaNaoEPreRequisitoDeSiMesma` | ✅ (local, 23/09) |
| CA11 | RN08 | auto | `FaseUseCasesTest.atualizaMantendoEditalEPermitindoAPropriaOrdem`, `EtapaUseCasesTest.atualizaMantendoAFase` | ✅ (local, 23/09) |
| CA12 | RN09 | manual | Roteiro abaixo | ⏳ |
| CA13 | RN09 | manual | Roteiro abaixo | ⏳ |
| CA14 | — | auto | `FaseUseCasesTest.operacoesEmFaseInexistenteDao404`, `EtapaUseCasesTest.listaBuscaEExcluiJuntoComEspacosVazios` | ✅ (local, 23/09) |

Os testes automatizados usam repositórios em memória. A ida e volta com o banco real
(roteiro "Fluxo completo") ainda precisa ser feita em homologação, nas duas instituições.

## Roteiros manuais

### Fluxo completo (repetir com `idecan` e `idib`)
1. `POST /v1/{inst}/fases?editalId=<edital de teste>` com nome, ordem 1 e datas.
2. `POST /v1/{inst}/etapas?faseId=<id da fase>` com nome e `tipoId` válido.
3. `GET`, `PUT` e `GET` de novo na fase e na etapa.
- **Esperado:** 201, 200, dados alterados persistidos, `createdAt`/`updatedAt` preenchidos no banco.
- **Obtido:**

### CA12 — Excluir fase com etapas
1. Usar a fase do fluxo acima, que tem uma etapa.
2. `DELETE /v1/{inst}/fases/{id}`.
- **Esperado:** 409 com mensagem; fase continua no banco.
- **Obtido:**

### CA13 — Excluir etapa com candidato
1. Escolher uma etapa com registro em `candidato_etapa` no edital de teste.
2. `DELETE /v1/{inst}/etapas/{id}`.
- **Esperado:** 409 com mensagem; etapa continua no banco.
- **Obtido:**

## Problemas encontrados
- _(nenhum até agora)_

## Decisão
- [ ] Aprovada — todos os critérios atendidos
- [ ] Reprovada — ver problemas acima

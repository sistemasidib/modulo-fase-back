# Pendências da Especificação 001 — Fases e Etapas do Edital

**Para:** Cauet (negócio) e Yuri Farias (técnico) · **Data:** 2026-09-23
**Origem:** §5.1 e §8 de `001-fases-e-etapas.md`

São **19 pendências**: 18 perguntas de negócio (abaixo) e 1 técnica, que fica com o Yuri
(no fim). A especificação só vira plano quando todas tiverem resposta. As 22 marcações
`[A DEFINIR]` do arquivo incluem 3 linhas da tabela §5.1 que repetem as perguntas 3, 4 e 5.

Como responder: marque uma opção ou escreva na linha **Resposta**. As opções são sugestões
para agilizar a conversa; qualquer outra resposta vale.

## Situação em 23/09/2026

| Situação | Perguntas |
|----------|-----------|
| ✅ Respondida e implementada | 1, 2, 3, 4, 5 (tipos e forma de contar), 6, 7, 8, 9, 10, 12, 15, 16, 17, 18 |
| ⏳ Falta validar | 11, 13, 14; 5 (duração padrão: tabela sem valores); confirmar o nome "THE"; pendência técnica das solicitações |

Detalhe das decisões e do que ficou provisório: §8 de `001-fases-e-etapas.md`.
Tabelas a criar: `db/001-fases-e-etapas.sql`.

---

## A. Contradições no documento-fonte (Cauet)

**1. Publicar o gabarito abre o recurso: evento ou data?**
O documento diz que publicar o gabarito abre o recurso, mas também diz que a Fase 1 não tem
nenhuma automação.
- [ ] a) Na Fase 1 o recurso só tem a **data de início** igual à data do gabarito preliminar. A abertura automática fica para a Fase 3.
- [x] b) Já na Fase 1, **publicar** o gabarito abre o recurso na hora.
- Resposta: Sim e pede para que usuario insira data de inicio e de fim do recurso assim o tempo de vizualizacao do documento.

**2. Os espaços de publicação e de recurso são criados com a fase ou com a etapa?**
O objetivo fala em "fase"; as regras e os critérios de aceite falam em "etapa".
- [ ] a) Etapa (cada etapa tem seus próprios espaços).
- [ ] b) Fase.
- [x] c) Fase e etapa do concurso devem conseguir manipular resultados prelimninares e resultado oficias

## B. Prazos e modelos padrão (Cauet)

**3. Gabarito definitivo: quantos dias depois da prova (D+N)?**
- Resposta: A escolha da pessoa que ira postar 

**4. Resultado: quantos dias depois da prova (D+N)?**
- Resposta: A escolha da pessoa que ira postar 

**5. Duração padrão do recurso, por tipo de etapa**
| Tipo de etapa | Duração (dias corridos) |
|---------------|-------------------------|
| Prova objetiva | | Prova Discursiva | THE| Heteroidentificacao|Biopsicosocial| Analise de solicitacoes (Atendimento Especial, Pcd, Hipossuficiencia, Isencao de Pagamento, Gabarito, Impugnacao, Prova de Titulos, Locais de Prova (Ensalamento))
| TAF | |
| Demais | |

O dia de início conta? Ex.: gabarito em 21/10 e 2 dias de recurso, o recurso vai até:
- [x] a) 22/10/2026 as 00:00 ate 30/10/2026 23:59 (Exemplo) 
- [ ] b) 23/10 (conta a partir do dia seguinte)

**6. Quais espaços de publicação o TAF e as demais modalidades têm, e com quais prazos?**
(Prova objetiva já está definida: local de prova D-9, gabarito preliminar D+1, gabarito definitivo, resultado.)
| Tipo de etapa | Publicação | Prazo (D±N) |
|---------------|-----------|-------------|
| TAF | | | (os prazos devem ser definos conforme o concurso implica em insercao manual dos dados )
| TAF | | |
| _(outro tipo)_ | | |

**7. Lista completa de tipos de etapa na Fase 1**
  (Revise o item 5) - Analise maneira que faca mas sentido de ser aplicada 
  Caso seja necesssarias novas tabelas criar todo escopo e estrura do back para que eu possa criar a tabela no banco.  
- [ ] Prova objetiva  - [ ] Prova discursiva  - [ ] TAF  - [ ] Avaliação psicológica
- [ ] Exame médico  - [ ] Prova de títulos  - [ ] Investigação social  - [ ] Prova prática
- Outros:

**8. Quais fases e etapas o sistema gera ao confirmar um grupo? (Cauet + Yuri)**
- O modelo deve ser de acordo com o edital, pode haver opcoes base estilo multipla escolha que o usuario define no preechimento dos dados base do concurso

## C. Agrupamento de cargos (Cauet)

**9. Pode gerar fases com cargos que ainda não estão em nenhum grupo?**
- [ ] a) Não: todo cargo precisa estar em um grupo antes de gerar.
- [ ] b) Sim: cargos sem grupo ficam sem fases até serem agrupados.
- [x] c)Resposta: O sistema tem que ser dinamico cargo no caso de um nova cargo criado deve haver um o botao de agrupar cargo. 

**10. E se um cargo mudar de grupo, ou um grupo for excluído, depois de gerar as fases? (Cauet + Yuri)**
- [ ] a) Proibido depois da geração.
- [ ] b) Permitido; o cargo passa a seguir as fases do novo grupo.
- [ ] c) Permitido só enquanto o edital não tiver inscritos.
- [x] d)Resposta: O sistema tem que ser dinamico cargo no caso de um nova cargo criado deve haver um o botao de agrupar cargo. 

## D. Ajustes e datas (Cauet)

**11. Remover uma etapa que já tem publicação ou data preenchida**
- [ ] a) Remove direto.
- [ ] b) Pede confirmação e remove.
- [ ] c) Impede a remoção.
- Resposta:

**12. O usuário pode digitar uma data fixa em uma publicação (em vez de D±N)?**
Ja Respondida nos primeiros itens 
- [ ] a) Não; só pode mudar o número de dias (D±N).
- [ ] b) Sim, e a data fixa **não muda** quando a data da prova mudar.
- [ ] c) Sim, mas a data fixa **é recalculada** quando a data da prova mudar.
- Resposta:

**13. Onde se ajusta o prazo D±N?**
- [ ] a) Por edital (vale para todas as etapas do mesmo tipo no edital).
- [ ] b) Por grupo de cargos.
- [ ] c) Por etapa.
- Resposta:

**14. Na Fase 1, só publicações e recurso têm prazo D±N (demandas ficam para a Fase 2)?**
- [ ] a) Sim.
- [ ] b) Não. Explicar:

## E. Abrangência e transição (Cauet + Yuri)

**15. A Fase 1 vale para quais instituições?**
- [x] a) IDECAN e IDIB.
- [ ] b) Só IDECAN, IDIB depois.
- [ ] c) Só IDIB, IDECAN depois.

**16. O espaço de recurso da etapa é o mesmo recurso que o candidato já envia hoje?**
- [ ] a) Não: é só a janela de datas (início e fim) do recurso.
- [x] b) Sim: é o mesmo recurso que o candidato abre hoje, agora ligado à etapa.
- Resposta: Fase e Etapa apenas Organize os dados pre-existentes deixando o fluxo mais organizacao e parametrizado

**17. Editais que já existem, com fases e candidatos**
- [ ] a) Ganham automaticamente um grupo com todos os cargos.
- [x] b) Ficam como estão; só editais novos usam o modelo novo.
- [ ] c) Migração manual, edital por edital.
- Resposta: Pois isso deve ser predefido no no cadastro do concurso, nao e obrigatorio

**18. Quem pode agrupar cargos, gerar e ajustar etapas e mudar prazos?**
- [x] a) Quem já pode editar o cadastro do edital.
- [ ] b) Uma permissão nova. Qual perfil:

---

## Pendência só técnica (Yuri, não precisa do Cauet)
- **Análise de solicitações (RN08):** no modelo atual, as solicitações já se ligam só à
  inscrição, não a fase/etapa. Localizar onde está o acoplamento que precisa ser desfeito
  (regra de serviço, tela ou status).
Hoje existe uma coluna etapa_id em todas solicitacoe, caso haja uma mudanca pode ser mudificada para fase_id

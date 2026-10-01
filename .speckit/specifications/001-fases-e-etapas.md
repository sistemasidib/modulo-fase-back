# Especificação 001 — Fases e Etapas do Edital

**Fase do produto:** 1 · **Status:** Rascunho (parcialmente decidida em 23/09/2026, ver §8) · **Autor:** Yuri Farias · **Data:** 2026-09-22
**Fonte:** `docs/Controle de Demanda - Fase 1.pdf` (Cauet, 18/09/2026), elaborado a partir da reunião "Modelagem de Fases e Cronogramas" de 17/09/2026 (Cauet, Yuri Farias, Gustavo Nunes)

## 1. Contexto e problema

O objetivo do Controle de Demanda é controlar publicações, prazos públicos e obrigações
internas de cada setor, evitando erros operacionais como publicar fora de ordem ou com
dados incompletos. O produto é entregue em 4 fases sequenciais, começando pela estrutura
de fases e etapas do concurso.

### 1.1 Visão geral das fases (fonte, p. 1–2)

Cada fase só começa depois que a anterior foi validada. Entre elas há 1 a 2 semanas de
testes e ajustes. As restrições entram como alerta na Fase 2 e só passam a bloquear na Fase 3.

| Fase | Objetivo | Regras de negócio | Fora do escopo |
|------|----------|-------------------|----------------|
| **1 – Estrutura de fases** *(esta especificação)* | Ao ser criada, a fase já tem os espaços de publicação e de recurso | Os cargos são agrupados, e o sistema gera automaticamente as fases e etapas de cada grupo. Cada etapa traz slots de publicação e recurso, com campo de data. As datas são relativas (D±N) à data da etapa, preenchida no cadastro do edital. Publicar o gabarito abre o recurso. O modelo vale para prova objetiva, TAF e demais modalidades. A análise de solicitações é desvinculada. | Cronograma, demandas, alertas e bloqueios |
| 2 – Cronograma e demandas | Criar o cronograma e as demandas de cada setor | O cronograma cobre eventos públicos e obrigações internas (ensalamento, baixas bancárias, atendimentos especiais). Item de cronograma é diferente de atividade: a atividade tem setor, dependências, demanda aplicada e checklist. As restrições de publicação de solicitações e de locais de prova geram **alerta**. As regras moram em um novo microsserviço. | Bloqueio e integração automática com a Fase 1 |
| 3 – Integração e automação | Juntar a Fase 1 com a Fase 2 | Publicações e etapas concluídas atualizam o painel de demandas. A ação de um setor dispara a demanda do próximo. Os alertas viram **bloqueio**: por exemplo, o gabarito não sai antes do fim da prova, e o local de prova não sai com ensalamento pendente. | Projeção e IA |
| 4 – Projeção e IA | Antecipar atrasos e volume de demandas | Projeção sobre o histórico, com backtest e nível de confiança. Mostra o impacto no cronograma e sugere medidas preventivas. Depende de um período de operação e de base acumulada. | — |

Esta especificação é a base das fases seguintes: cronograma, demandas, alertas,
bloqueios e projeção dependem da estrutura de fases, etapas e espaços de publicação
criada aqui.

## 2. Objetivo

No cadastro do edital, permitir que o usuário agrupe os cargos e que o sistema gere
automaticamente, para cada grupo, as fases e etapas. Cada etapa deve nascer com seus
espaços de publicação e de recurso e com datas relativas (D±N) à data da etapa, que são
recalculadas automaticamente quando ela muda.

## 3. Fora do escopo

Fonte: "Cronograma, demandas, alertas e bloqueios". A Fase 1 entrega a estrutura
"sem nenhuma automação". Em detalhe:

- Cronograma e demandas por setor, incluindo obrigações internas como ensalamento,
  baixas bancárias e atendimentos especiais (Fase 2).
- Alertas de qualquer tipo, inclusive os de publicação de solicitações e de locais de prova (Fase 2).
- Bloqueios de publicação, por exemplo gabarito antes do fim da prova ou local de prova
  com ensalamento pendente (Fase 3).
- Atualização automática do painel de demandas por publicações ou etapas concluídas (Fase 3).
- Projeções e IA (Fase 4).
- *Interpretação, a confirmar:* abertura do recurso disparada pelo **evento** de
  publicar o gabarito. Nesta leitura, na Fase 1 o vínculo é só de **data** (RN11). Ver
  a pendência "Publicar o gabarito abre o recurso".
- Ajuste da área do candidato antiga para exibir a nova configuração. É uma entrega
  paralela, sob responsabilidade de Alisson (ver §10).

## 4. Glossário

| Termo | Definição |
|-------|-----------|
| Edital | Documento/cadastro que define um concurso. Contém cargos, grupos, fases e etapas. |
| Cargo | Vaga ofertada no edital. |
| Grupo de cargos | Conjunto de cargos do edital, montado livremente pelo usuário, que compartilha a mesma estrutura de fases e etapas. |
| Fase | Agrupador de etapas dentro de um grupo. Pode aplicar cláusula de barreira. |
| Etapa | Evento avaliativo de uma fase (ex.: prova objetiva, TAF). Tem tipo, data-âncora e espaços de publicação. |
| Tipo de etapa | Modalidade da etapa (prova objetiva, TAF, demais). Define os espaços de publicação criados e a duração padrão do recurso. |
| Dia D / data-âncora | Data da etapa, preenchida no cadastro do edital. Referência para todas as datas relativas da etapa. |
| Deslocamento (D±N) | Distância, em **dias corridos**, entre uma data dependente e o Dia D. |
| Espaço (slot) de publicação | Registro criado junto com a etapa, pronto para receber data e conteúdo de uma publicação (ex.: local de prova). |
| Período de recurso | Janela em que o candidato pode recorrer. Começa com o gabarito preliminar ("Modelo de datas relativas"). Evento × data: ver pendência. |
| Cláusula de barreira | Opção da fase que limita quantos candidatos avançam. *(Definição usual do termo; a fonte não define. Na Fase 1 é só uma opção liga/desliga, sem regra de corte.)* |
| Análise de solicitações | Processo de análise de pedidos de candidatos (ex.: atendimento especial), independente de fases e etapas. |

## 5. Regras de negócio

RN01–RN10 transcritas **literalmente** do documento-fonte (p. 2–3). Os
**Complementos** trazem detalhes de outras partes do mesmo documento, com a origem
indicada. RN11 e RN12 não estão numeradas na fonte e foram extraídas dos critérios de aceite.

> As fases e etapas são criadas dentro do cadastro do edital, por grupo de cargos, e o
> sistema as gera automaticamente.
> O cadastro do edital segue essa ordem: cargos, agrupamento, geração, ajuste e datas.

- **RN01 – Posição no cadastro.** A etapa "Fases e Etapas" fica no cadastro do edital,
  logo depois de "Cargos".
- **RN02 – Agrupamento antes das fases.** Antes de criar as fases, o usuário monta
  livremente os grupos de cargos do edital. Por exemplo, um grupo de nível médio e outro
  de nível superior, quando têm número de etapas diferente. Cada cargo pertence a
  exatamente um grupo.
  - *Complemento (critério de aceite "Agrupamento de cargos"):* não é possível gerar
    fases sem ao menos um grupo; o sistema não impõe critério fixo; o sistema impede
    incluir em um grupo um cargo que já está em outro.
- **RN03 – Etapas por grupo.** Cada grupo tem seu próprio conjunto de fases e etapas.
  Grupos do mesmo edital podem ter quantidade de etapas e datas diferentes.
- **RN04 – Geração automática.** O sistema gera automaticamente as fases e etapas de cada
  grupo, sem digitação manual.
  - *Complemento (critério de aceite "Geração automática"):* a geração ocorre ao confirmar um grupo.
- **RN05 – Espaços de publicação.** Cada etapa gerada já cria seus espaços de publicação
  e o espaço do período de recurso.
  - *Complemento (visão geral da Fase 1 e critérios de aceite "Espaços de publicação"):*
    o modelo vale para prova objetiva, TAF e demais modalidades. Para prova objetiva, os
    espaços são: local de prova, gabarito preliminar, gabarito definitivo e resultado.
- **RN06 – Ajuste.** Depois de geradas, as etapas de cada grupo podem ser adicionadas,
  removidas ou renomeadas, sem afetar os outros grupos.
- **RN07 – Datas.** A data de cada etapa é preenchida no edital. As publicações e
  demandas da etapa usam um deslocamento D±N em relação a ela, contado em dias corridos.
  Se a data da etapa mudar, todas as datas dependentes são recalculadas automaticamente.
  - *Complemento ("Modelo de datas relativas" e critério de aceite "Data-âncora"):* a
    etapa nasce com o campo de data vazio.
- **RN08 – Solicitações.** A análise de solicitações não depende de fase nem de etapa.
- **RN09 – Cláusula de barreira.** Cada fase tem a opção de aplicar cláusula de barreira.
- **RN10 – Duração do recurso.** Cada etapa tem um campo para a duração do período de
  recurso. O valor vem pré-configurado ao selecionar o tipo de etapa (prova objetiva ou
  outras), e o usuário pode sobrescrevê-lo.
- **RN11 – Recurso vinculado ao gabarito.** *(Derivada do critério de aceite "Recurso
  vinculado" e da visão geral: "Publicar o gabarito abre o recurso".)* O período de
  recurso começa na data de publicação do gabarito preliminar.
- **RN12 – Deslocamentos por edital.** *(Derivada de "Modelo de datas relativas" e do
  critério de aceite "Datas relativas".)* Os deslocamentos são padrão, mas podem ser
  ajustados por edital. Alterar um edital não afeta os outros.

### 5.1 Modelo de datas relativas (fonte, p. 2)

> A data da etapa (Dia D) é a âncora, e toda publicação ou demanda da etapa guarda um
> deslocamento em relação a ela, contado em dias corridos. A etapa nasce com o campo de
> data vazio, e a data é preenchida no cadastro do edital. Os deslocamentos são padrão,
> mas podem ser ajustados por edital. Se a data da etapa mudar, todas as datas
> dependentes são recalculadas automaticamente.

| Publicação / demanda | Deslocamento | Exemplo (prova em 20/10) |
|----------------------|--------------|--------------------------|
| Publicação dos locais de prova | D-9 | 11/10 |
| Aplicação da prova (âncora) | D | 20/10 |
| Publicação do gabarito preliminar | D+1 | 21/10 |
| Período de recurso | Inicia com o gabarito preliminar. A duração vem pré-configurada pelo tipo de etapa e é editável | `[A DEFINIR]` |
| Gabarito definitivo | `[A DEFINIR]` | `[A DEFINIR]` |
| Resultado | `[A DEFINIR]` | `[A DEFINIR]` |

As datas são ilustrativas. Os deslocamentos "a definir" estão nas pendências (§8).

### 5.2 Pontos de atenção da fonte (p. 4), com cobertura

| Ponto de atenção | Coberto por |
|------------------|-------------|
| A condição de publicação de solicitações não bloqueia. Ela gera um alerta sinalizando que nem todas as solicitações que deveriam ter sido respondidas foram respondidas. | §3: alerta é escopo da Fase 2 |
| Cada fase precisa oferecer a opção de cláusula de barreira. | RN09, CA07 |
| A duração do período de recurso vem pré-configurada conforme o tipo de etapa (prova objetiva ou outras), mas o usuário pode sobrescrevê-la. | RN10, CA15 |
| Todos os deslocamentos contam em dias corridos. | RN07, CA11, CA18 |
| Mudar a data de uma etapa recalcula automaticamente todas as datas dependentes. | RN07, CA13 |
| Os deslocamentos padrão do gabarito definitivo e do resultado ainda precisam ser definidos. | §8, pendências |

## 6. Histórias de usuário

*Redigidas a partir das regras; não constam da fonte.*

- **HU01.** Como operador do edital, quero agrupar os cargos por estrutura de avaliação
  para que cargos com etapas diferentes (ex.: nível médio e nível superior) tenham fases próprias.
- **HU02.** Como operador do edital, quero que o sistema gere as fases e etapas de cada
  grupo para não precisar digitá-las.
- **HU03.** Como operador do edital, quero que cada etapa já traga os espaços de
  publicação e de recurso para não esquecer nenhuma publicação obrigatória.
- **HU04.** Como operador do edital, quero informar só a data da prova e ter as demais
  datas calculadas para evitar erros de cálculo e retrabalho quando a prova mudar de data.
- **HU05.** Como operador do edital, quero ajustar as etapas e os deslocamentos de um
  edital específico sem afetar os outros grupos ou editais.

## 7. Critérios de aceite

### 7.1 Escopo da Fase 1 (fonte, p. 3–4, transcrito literalmente)

> A Fase 1 entrega a estrutura de fases e etapas, com slots de publicação e datas
> relativas, sem nenhuma automação. Cronograma, demandas por setor, alertas e bloqueios
> ficam para as fases seguintes.

| Épico | Tarefa | Critério de aceite | Regra | Cenários |
|-------|--------|--------------------|-------|----------|
| Cadastro do edital | Criar a etapa "Fases e Etapas" no cadastro do edital, logo após "Cargos" | No cadastro do edital, "Fases e Etapas" aparece imediatamente depois de "Cargos" | RN01 | CA01 |
| Agrupamento de cargos | O usuário monta livremente os grupos de cargos antes da criação das fases | Não é possível gerar fases sem ao menos um grupo. O usuário escolhe quais cargos entram em cada grupo, sem critério fixo imposto pelo sistema. O sistema impede incluir em um grupo um cargo que já está em outro | RN02 | CA02, CA03, CA04 |
| Geração automática | O sistema gera automaticamente as fases e etapas de cada grupo | Ao confirmar um grupo, o sistema cria as fases e etapas sem digitação manual | RN04 | CA05 |
| Etapas por grupo | Cada grupo mantém seu próprio conjunto de etapas | Dois grupos do mesmo edital podem ter quantidades de etapas e datas diferentes | RN03 | CA06 |
| Cláusula de barreira | Opção de cláusula de barreira em cada fase | Cada fase permite ativar ou não a cláusula de barreira | RN09 | CA07 |
| Espaços de publicação | Cada etapa nasce com espaços de publicação e de período de recurso | Ao criar uma etapa de prova objetiva, o sistema já cria o espaço de publicação de cada item (local de prova, gabarito preliminar, gabarito definitivo e resultado) e o espaço do período de recurso, prontos para receber data e conteúdo | RN05 | CA08 |
| Espaços de publicação | O mesmo modelo vale para TAF e demais modalidades | Ao criar uma etapa de TAF, o sistema já cria os espaços de publicação correspondentes | RN05 | CA09 |
| Data-âncora | Campo de data da etapa, preenchido no cadastro do edital | A etapa aparece sem data até ser preenchida no edital e exibe a data depois disso | RN07 | CA10 |
| Datas relativas | Cada publicação e demanda guarda um deslocamento (D±N) em dias corridos | Com a prova em 20/10, o sistema calcula locais de prova em 11/10 (D-9) e gabarito preliminar em 21/10 (D+1) | RN07 | CA11 |
| Datas relativas | Deslocamentos parametrizáveis por edital | Alterar o D-9 para D-7 em um edital não afeta os outros | RN12 | CA12 |
| Recálculo automático | Alterar a data da etapa recalcula as datas dependentes | Ao mudar a prova de 20/10 para 22/10, locais de prova passa para 13/10 e gabarito preliminar para 23/10, sem ação manual | RN07 | CA13 |
| Recurso vinculado | O período de recurso fica atrelado à publicação do gabarito | O recurso começa na data de publicação do gabarito | RN11 | CA14 |
| Duração do recurso | Campo de duração do período de recurso | Ao selecionar prova objetiva ou outro tipo de etapa, a duração vem preenchida e o usuário pode sobrescrevê-la | RN10 | CA15 |
| Ajuste | Etapas editáveis por grupo | É possível adicionar, remover ou renomear etapas de um grupo sem afetar os outros grupos | RN06 | CA16 |
| Desvinculação | Análise de solicitações fora de fases e etapas | A análise de solicitações funciona sem depender de fase ou etapa | RN08 | CA17 |

### 7.2 Cenários de teste (Dado / Quando / Então)

Cada critério da fonte foi desdobrado em cenários testáveis. Onde o cenário interpreta a
fonte, isso está indicado na coluna "Nota".

| ID | Regra | Critério (Dado / Quando / Então) | Nota |
|----|-------|----------------------------------|------|
| CA01 | RN01 | Dado o cadastro do edital, quando o usuário vê a sequência de etapas do cadastro, então "Fases e Etapas" aparece imediatamente depois de "Cargos". | |
| CA02 | RN02 | Dado um edital sem nenhum grupo de cargos, quando o usuário tenta gerar as fases, então o sistema impede a geração. | |
| CA03 | RN02 | Dado um edital com cargos cadastrados, quando o usuário monta um grupo, então pode escolher quaisquer cargos para ele, sem critério imposto pelo sistema. | |
| CA04 | RN02 | Dado um cargo que já pertence ao grupo A, quando o usuário tenta incluí-lo no grupo B, então o sistema impede a inclusão. | |
| CA05 | RN04 | Dado um grupo de cargos montado, quando o usuário confirma o grupo, então o sistema cria as fases e etapas do grupo sem digitação manual. | Quais fases e etapas são geradas: pendência "Modelo de geração". |
| CA06 | RN03 | Dado um edital com os grupos A e B, quando o usuário configura as etapas e datas de cada um, então A e B podem ter quantidades de etapas e datas diferentes. | |
| CA07 | RN09 | Dada uma fase gerada, quando o usuário edita a fase, então pode ativar ou desativar a cláusula de barreira. | |
| CA08 | RN05 | Dada a criação de uma etapa de prova objetiva, quando a etapa é criada, então já existem os espaços de publicação de local de prova, gabarito preliminar, gabarito definitivo e resultado, além do espaço do período de recurso, prontos para receber data e conteúdo. | |
| CA09 | RN05 | Dada a criação de uma etapa de TAF, quando a etapa é criada, então já existem os espaços de publicação correspondentes ao TAF. | Lista de espaços do TAF: pendência. |
| CA10 | RN07 | Dada uma etapa recém-gerada, quando ainda não há data informada no edital, então a etapa aparece sem data. Quando a data é informada, a etapa passa a exibi-la. | |
| CA11 | RN07 | Dada uma prova objetiva em 20/10 com os deslocamentos padrão, quando as datas são calculadas, então locais de prova = 11/10 (D-9) e gabarito preliminar = 21/10 (D+1). | |
| CA12 | RN12 | Dados dois editais com deslocamento padrão D-9 para locais de prova, quando o usuário muda para D-7 em um deles, então o outro continua com D-9. | |
| CA13 | RN07 | Dada uma prova em 20/10, quando o usuário muda a data para 22/10, então locais de prova passa para 13/10 e gabarito preliminar para 23/10, sem ação manual. | |
| CA14 | RN11 | Dada uma etapa com gabarito preliminar em 21/10, quando as datas são calculadas, então o período de recurso começa em 21/10. | *Interpretação:* a fonte diz "gabarito", sem qualificar. Assumido **preliminar**, com base no "Modelo de datas relativas" ("Inicia com o gabarito preliminar"). Evento × data: pendência "Publicar o gabarito abre o recurso". |
| CA15 | RN10 | Dada a criação de uma etapa, quando o usuário seleciona o tipo (prova objetiva ou outro), então a duração do recurso vem preenchida com o padrão do tipo, e o usuário pode sobrescrevê-la. | Valores padrão: pendência. |
| CA16 | RN06 | Dado um edital com os grupos A e B, quando o usuário adiciona, remove ou renomeia etapas do grupo A, então as etapas do grupo B não mudam. | |
| CA17 | RN08 | Dado um edital, com ou sem fases e etapas, quando o usuário realiza a análise de solicitações, então ela funciona sem depender de fase ou etapa. | |
| CA18 | RN07 | Dado um deslocamento que cruza a virada de mês (ex.: prova em 05/11, D-9), quando as datas são calculadas, então o resultado é 27/10, contando dias corridos (sem desconsiderar fins de semana ou feriados). | **Não está na fonte.** Cenário adicional para testar "dias corridos" (RN07). |

## 8. Pendências

Respostas registradas em `001-fases-e-etapas-pendencias.md` (23/09/2026). As decisões abaixo
**prevalecem** sobre o texto das seções 5 e 7 onde houver conflito.

### 8.1 Decididas (implementadas)

| # | Pergunta | Decisão | Efeito na especificação |
|---|----------|---------|-------------------------|
| 1 | Recurso: evento ou data? | **Evento.** Publicar o gabarito abre o recurso. Ao publicar, o usuário informa início e fim do recurso e o período de visualização do documento. | RN11 passa a ser evento. CA14 vira: "ao publicar o gabarito preliminar com recurso de 22/10 00:00 a 30/10 23:59, o recurso fica aberto nesse período". |
| 2 | Espaços na fase ou na etapa? | **Nas duas.** Fase e etapa manipulam resultado preliminar e resultado oficial. | RN05: a fase também nasce com espaços de resultado preliminar e oficial. "Resultado" vira "resultado preliminar" + "resultado oficial". |
| 3, 4, 6, 12 | Deslocamentos do gabarito definitivo, do resultado, do TAF e demais; data fixa? | **Datas manuais**, escolhidas por quem publica, conforme o concurso. | RN07 e RN12 (D±N, recálculo) ficam **suspensas** até as perguntas 13 e 14. CA11, CA12, CA13 e CA18 suspensos. |
| 5 (dia de início) | Como contar o período? | **Datas e horas explícitas** (ex.: 22/10/2026 00:00 a 30/10/2026 23:59). | RN10: em vez de duração pré-configurada, o período de recurso é informado ao publicar. CA15 suspenso. |
| 5, 7 | Tipos de etapa | Prova objetiva, prova discursiva, TAF, THE, heteroidentificação, biopsicossocial e análise de solicitações (atendimento especial, PcD, hipossuficiência, isenção de pagamento, gabarito, impugnação, prova de títulos, locais de prova/ensalamento). | Tabela `dom_tipo_etapa`. Cada subtipo de análise é um tipo de etapa. Espaços por tipo parametrizados em `dom_tipo_etapa_publicacao`. |
| 8 | O que é gerado? | O usuário escolhe, em múltipla escolha, as fases e os tipos de etapa de cada fase no cadastro do concurso. | RN04: geração a partir da escolha do usuário, sem modelo fixo. |
| 9, 10 | Cargo sem grupo; mudança de grupo | **Dinâmico.** Botão "agrupar cargo" a qualquer momento, inclusive para cargos novos. | Pode gerar com cargos soltos. Cargo muda de grupo saindo do anterior (CA04 mantido: não entra em dois grupos ao mesmo tempo). |
| 15 | Instituições | IDECAN e IDIB. | Mesmas tabelas nos dois bancos. |
| 16 | Espaço de recurso × recurso existente | É o recurso que o candidato já envia. Fase e etapa só organizam e parametrizam os dados existentes. | Nenhuma entidade nova de recurso; a janela fica na publicação. |
| 17 | Editais existentes | Ficam como estão; o modelo novo é opcional no cadastro do concurso. | Sem migração. Tabelas antigas não mudam. |
| 18 | Permissões | As de quem já edita o cadastro do edital. | Sem permissão nova. |

### 8.2 Ainda abertas

- [ ] `[A DEFINIR]` **(11) Remover etapa com publicação ou data preenchida.** *Provisório no
  código:* a remoção é recusada enquanto houver espaço preenchido ou publicado; espaços vazios
  são removidos junto. Responsável: Cauet.
- [ ] `[A DEFINIR]` **(13) Onde se ajusta o D±N** e **(14) só publicações e recurso têm D±N?**
  Com as datas manuais (3, 4, 6, 12), confirmar se o D±N sai da Fase 1 de vez. Se sair, remover
  RN07/RN12 e CA11–CA13/CA18. Responsável: Cauet.
- [ ] `[A DEFINIR]` **(5) Duração padrão do recurso por tipo**: a tabela veio sem valores. Com o
  período informado ao publicar, ainda precisa de padrão? Responsável: Cauet.
- [ ] `[A DEFINIR]` **"THE"**: confirmar o nome. Cadastrado como "Teste de habilidade específica (THE)". Responsável: Cauet.
- [ ] `[A DEFINIR]` **Espaços padrão dos demais tipos** (discursiva, TAF, THE...): hoje só
  resultado preliminar e oficial; a prova objetiva tem também local de prova e gabaritos. Ajustável
  em `dom_tipo_etapa_publicacao`, sem código. Responsável: Cauet.
- [ ] `[A DEFINIR]` **Conteúdo da publicação**: o espaço guarda datas; onde fica o documento
  (arquivo, link)? Responsável: Cauet / Yuri.
- [ ] `[A DEFINIR]` **Solicitações (técnica)**: todas as solicitações têm `etapa_id`; pode virar
  `fase_id` se houver mudança. Não executado. Responsável: Yuri.

## 9. Notas técnicas para o plano

Levantadas a partir de `docs/MODELS_IDECAN_IDIB.md`. Não fazem parte do requisito, mas
o plano precisa tratá-las.

| # | Situação atual | Impacto na especificação |
|---|----------------|--------------------------|
| N1 | `Fase` pertence ao `Edital` (`edital_id`), com UK `(edital_id, ordem)`. | **Conflita com RN03**: dois grupos do mesmo edital teriam fase de ordem 1. A fase precisa se ligar ao grupo, e a UK virar `(grupo_id, ordem)` ou equivalente. |
| N2 | Não existe entidade de grupo de cargos. `Cargo` só se liga a `Edital`. | Criar grupo + vínculo cargo↔grupo, garantindo que cada cargo esteja em no máximo um grupo por edital (RN02, CA04). |
| N3 | Em IDECAN, `Etapa.tipo` → `DomTipoEtapa` está **comentado**. Em IDIB está ativo, mas aponta para a entidade do pacote `idecan` (relação entre pacotes/datasources). | RN05 e RN10 dependem do tipo da etapa. Ativar o mapeamento no IDECAN e revisar a referência entre datasources no IDIB. |
| N4 | `Etapa` não tem data-âncora, duração de recurso nem espaços de publicação. | Novos campos/entidades: data da etapa (anulável, CA10), duração do recurso, espaço de publicação com deslocamento. |
| N5 | Não há onde guardar deslocamentos padrão por tipo de etapa nem sobrescrita por edital. | Tabela de padrões (provavelmente ligada a `DomTipoEtapa`) + sobrescrita por edital (RN12, CA12). |
| N6 | `Fase` não tem flag de cláusula de barreira. `Fase.status` / `DomStatusFase` e `Etapa.status` estão comentados. | Adicionar a flag (RN09). Os status seguem fora do escopo, salvo decisão em contrário. |
| N7 | Já existem `CandidatoFase`, `CandidatoEtapa`, `EtapaDocumento` e `EtapaRecurso` ligados a `Fase`/`Etapa`. | Regerar ou remover etapas (RN06) pode deixar esses registros órfãos. Definir a regra junto com a pendência "Remoção de etapa". |

## 10. Dependências e responsáveis

| Responsável | Item |
|-------------|------|
| Cauet | Enviar o material do fluxo de acompanhamento e definir os deslocamentos do gabarito definitivo e do resultado |
| Cauet | Mock simplificado da Fase 1 |
| Yuri Farias | Refatorar a lógica das fases e implementar esta especificação |
| Yuri Farias | Desvincular a análise de solicitações das fases e etapas (RN08) |
| Gustavo Nunes | Configuração de teste no edital para validar a Fase 1 (base de `validation/001-fases-e-etapas.md`) |
| Alisson | Ajustar a área do candidato antiga para exibir a nova configuração |

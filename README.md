# modulo-fase-back

Microsserviço do **Controle de Demanda — Fase 1**: estrutura de fases, etapas, grupos de
cargos e publicações de cada edital, para as instituições **IDECAN** e **IDIB**.

Nesta fase não há automação, alertas nem bloqueios — apenas o cadastro e a organização
da estrutura. Regras de negócio em `docs/Controle de Demanda - Fase 1.pdf`.

## Stack

- Java 21 · Quarkus 3.24 (REST + Jackson, OpenAPI/Swagger UI, Health)
- Hibernate ORM com Panache, via entidades do `bio-registro-core`
- SQL Server, com um datasource por instituição (`idecan` e `idib`)
- JUnit 5

## Arquitetura

DDD com três camadas; as dependências apontam para dentro
(`infrastructure` → `application` → `domain`).

```
src/main/java/br/bioregistro/fase/
├── domain/           # modelo, regras e invariantes; portas (interfaces de repositório)
│   ├── model/
│   ├── port/
│   └── exception/
├── application/      # casos de uso, DTOs e controllers REST
│   ├── use_cases/
│   ├── dtos/
│   └── controllers/
└── infrastructure/   # adapters das portas sobre Panache, um pacote por banco
    └── persistence/
        ├── idecan/
        └── idib/
```

O domínio não conhece Quarkus, JPA nem HTTP. A escolha do banco é feita pela instituição
informada na URL.

## Pré-requisitos

- JDK 21
- Acesso ao repositório Maven privado do `bio-registro-core` (AWS CodeArtifact,
  declarado no `pom.xml`), com credenciais configuradas no `~/.m2/settings.xml`
- SQL Server com os bancos IDECAN e IDIB

## Banco de dados

Rode `db/001-fases-e-etapas.sql` **em cada banco** (IDECAN e IDIB). O script cria as
tabelas novas e os valores de domínio, não altera tabelas existentes e é idempotente.

## Configuração

As credenciais vêm só de variáveis de ambiente — nunca as coloque no
`application.properties`.

| Variável | Descrição |
|----------|-----------|
| `DB_URL_IDECAN` | JDBC URL do banco IDECAN |
| `DB_USER_IDECAN` | Usuário do banco IDECAN |
| `DB_PASS_IDECAN` | Senha do banco IDECAN |
| `DB_URL_IDIB` | JDBC URL do banco IDIB |
| `DB_USER_IDIB` | Usuário do banco IDIB |
| `DB_PASS_IDIB` | Senha do banco IDIB |
| `HTTP_PORT` | Porta HTTP (padrão `8000`) |

Para desenvolvimento local, um arquivo `.env` na raiz é lido pelo Quarkus e já está no
`.gitignore`.

## Como rodar

```powershell
# modo dev (hot reload)
./mvnw quarkus:dev

# testes
./mvnw test

# build e execução do pacote
./mvnw package
java -jar target/quarkus-app/quarkus-run.jar
```

Com a aplicação no ar:

- Swagger UI: http://localhost:8000/q/swagger-ui
- Health: http://localhost:8000/q/health

## API

Todas as rotas começam com `/v1/{instituicao}`, onde `{instituicao}` é `idecan` ou `idib`.

| Recurso | Rotas |
|---------|-------|
| Fases | `GET /fases?editalId=&grupoCargoId=` · `POST /fases?editalId=` · `GET/PUT/DELETE /fases/{id}` |
| Etapas | `GET /etapas?faseId=` · `POST /etapas?faseId=` · `GET/PUT/DELETE /etapas/{id}` |
| Grupos de cargo | `GET /grupos-cargo?editalId=` · `POST /grupos-cargo?editalId=` · `PUT/DELETE /grupos-cargo/{id}` |
| Cargos do grupo | `GET /grupos-cargo/cargos-sem-grupo?editalId=` · `PUT/DELETE /grupos-cargo/{id}/cargos/{cargoId}` |
| Geração de fases | `POST /grupos-cargo/{id}/gerar-fases` |
| Publicações | `GET /publicacoes?faseId=` · `POST /publicacoes?faseId=` · `PUT/DELETE /publicacoes/{id}` · `POST /publicacoes/{id}/publicar` |
| Tipos | `GET /tipos/etapa` · `GET /tipos/publicacao` |

Os contratos completos (corpos de requisição e resposta) estão no Swagger UI.

## Documentação

- `docs/Controle de Demanda - Fase 1.pdf` — regras de negócio e escopo da fase
- `docs/MODELS_IDECAN_IDIB.md` — modelo de dados existente nos dois bancos
- `docs/relatorio-fase1.pdf` — relatório da Fase 1

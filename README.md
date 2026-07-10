# Assistente de RH — Hvogel (Spring AI + RAG + Claude)

Assistente virtual de RH com **RAG** (Retrieval-Augmented Generation), ancorado no manual de políticas da **Hvogel Tecnologia Ltda.**

- Chat com **Claude** (Anthropic) em streaming SSE
- Embeddings com **Ollama** (`bge-m3`)
- Vector store: **PostgreSQL + pgvector**
- Memória de conversa: **Redis**
- UI web com identidade visual Hvogel

Repositório: [hamdenvogel/spring-ai-hvogel-assistant](https://github.com/hamdenvogel/spring-ai-hvogel-assistant)

---

## Origem e créditos

Este projeto foi **baseado** no artigo e no código hands-on da DevSuperior:

- Artigo: [Spring AI em ação: assistente de RH com RAG e Claude](https://devsuperior.com.br/blog/spring-ai-em-acao-assistente-de-rh-com-rag-e-claude)
- Repositório citado no artigo (esqueleto / código completo): [github.com/devsuperior/blog](https://github.com/devsuperior/blog) — pasta do projeto `hr-assistant` no blog

A base original usa o caso **Aurora Car Dealer** (`br.com.devsuperior`). Esta evolução adapta o mesmo padrão Spring AI + RAG + Claude para a **Hvogel** (`br.com.hvogel`) e acrescenta as implementações listadas abaixo.

---

## O que foi alterado / acrescentado neste projeto

### Rebrand e domínio Hvogel
- Pacote e `groupId` renomeados de `devsuperior` → `hvogel`
- Manual próprio: `hvogel_politicas_rh.pdf` (gerado a partir do markdown + logo da marca)
- Prompts e contatos de RH/ética atualizados para Hvogel
- Vector store dedicado: tabela `hvogel_vector_store`

### Ingestão e RAG
- Ingestão automática na subida (`StartupIngestionRunner`) quando o vector store está vazio
- Parâmetros RAG ajustados (`top-k`, `similarity-threshold`) para o manual Hvogel
- Endpoint de ingest manual mantido (`POST /ingest`)

### UI profissional (`http://localhost:8080/`)
- Layout com identidade visual Hvogel (logo, cores, footer com versão)
- Bolhas de chat, sugestões rápidas e indicador de digitação animado
- Estados claros: “Consultando o manual…” → “Redigindo resposta…”
- Destaque visual quando o RAG não encontra informação (fallback)
- Copiar resposta; feedback 👍/👎 (`POST /chat/feedback`)
- Exportar conversa em TXT e PDF
- `GET /api/info` para versão/desenvolvedor no footer

### Qualidade, testes e Sonar
- Cobertura JUnit ampla (controllers, services, ingestion, advisor, DTOs, config)
- JaCoCo + plugin SonarQube (`sonar.projectKey=hr-assistant`)
- Issues Sonar corrigidas (ex.: método `register` no feedback, null-safety no advisor)

### Segurança e preparação para GitHub
- Sem credenciais versionadas (`.env`, `application-local.*` no `.gitignore`)
- Segredos só via variáveis de ambiente (sem senha/API key hardcoded)
- Arquivos `.example`: `.env.example`, `application-local.yml.example`, `application-local.properties.example`

### Documentação
- `documento-tecnico-hr-assistant.md` — arquitetura
- `melhorias-ui-hr-assistant.md` — UI e contrato de feedback
- `roadmap-melhorias-hr-assistant.md` — evolução futura
- `comandos-cmd-cmder-hr-assistant.md` / `sonarqube.md` — operação e análise

---

## Pré-requisitos

- JDK 25+
- Docker e Docker Compose
- Chave da API Anthropic (`ANTHROPIC_API_KEY`)

---

## Configuração (sem credenciais no Git)

1. Copie o exemplo de ambiente e **preencha** chave/senha:

```powershell
copy .env.example .env
```

2. (Opcional) Overrides locais — escolha um:

```powershell
copy src\main\resources\application-local.yml.example src\main\resources\application-local.yml
# ou
copy src\main\resources\application-local.properties.example src\main\resources\application-local.properties
```

Arquivos **não** versionados (`.gitignore`):
- `.env`
- `application-local.yml`
- `application-local.properties`

Senhas e `ANTHROPIC_API_KEY` **não** ficam hardcoded em `docker-compose.yml` / `application.yml` — só via variáveis de ambiente.

---

## Execução local

```powershell
docker compose up -d
# carrega ANTHROPIC_API_KEY do .env ou exporte no shell:
$env:ANTHROPIC_API_KEY = "sua-chave-aqui"
$env:SPRING_PROFILES_ACTIVE = "anthropic"
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=anthropic"
```

Abra: http://localhost:8080/

No primeiro start (vector store vazio), o PDF `hvogel_politicas_rh.pdf` é indexado automaticamente.

Ingest manual:

```powershell
curl.exe -X POST -F "file=@src/main/resources/docs/hvogel_politicas_rh.pdf" http://localhost:8080/ingest
```

---

## Testes e cobertura

```powershell
.\mvnw.cmd test
```

Relatório JaCoCo: `target/site/jacoco/index.html`

---

## SonarQube (opcional)

```powershell
$env:SONAR_TOKEN = "seu-token"
.\mvnw.cmd clean verify sonar:sonar
```

Dashboard: http://localhost:9000/dashboard?id=hr-assistant  
Detalhes: [`sonarqube.md`](sonarqube.md)

---

## Documentação do projeto

| Arquivo | Conteúdo |
|---|---|
| [`documento-tecnico-hr-assistant.md`](documento-tecnico-hr-assistant.md) | Arquitetura e funcionamento |
| [`melhorias-ui-hr-assistant.md`](melhorias-ui-hr-assistant.md) | UI, feedback e exportação |
| [`roadmap-melhorias-hr-assistant.md`](roadmap-melhorias-hr-assistant.md) | Roadmap de evolução |
| [`comandos-cmd-cmder-hr-assistant.md`](comandos-cmd-cmder-hr-assistant.md) | Comandos operacionais |
| [`sonarqube.md`](sonarqube.md) | Análise Sonar |

---

## Stack

- Java 25 / Spring Boot 4.1 / Spring AI 2.0
- Anthropic Claude (chat) + Ollama bge-m3 (embeddings)
- PostgreSQL + pgvector, Redis Stack
- JaCoCo + SonarQube

---

## Segurança

- **Não** commite `.env`, `application-local.yml` ou tokens
- Credenciais de DB/Redis/Ollama vêm de variáveis de ambiente (veja `.env.example`)
- A chave Anthropic é lida apenas de `ANTHROPIC_API_KEY`

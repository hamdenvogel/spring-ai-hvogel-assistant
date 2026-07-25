# HV Assistant — Hvogel (Spring AI + RAG + Claude)

![CI](https://github.com/hamdenvogel/spring-ai-hvogel-assistant/actions/workflows/ci.yml/badge.svg)
![Release](https://img.shields.io/github/v/release/hamdenvogel/spring-ai-hvogel-assistant?label=release)

Assistente virtual de RH com **RAG** (Retrieval-Augmented Generation), ancorado no manual de políticas da **Hvogel Tecnologia Ltda.**

- Chat com **Claude** (Anthropic) em streaming SSE
- Embeddings com **OpenAI** (`text-embedding-3-small`, 1536 dims) — sem Ollama
- Vector store: **PostgreSQL + pgvector** (Neon em produção)
- Memória de conversa: **Redis Stack** (Redis Cloud em produção)
- UI web com identidade visual Hvogel
- Deploy: **Google Cloud Run** (`Dockerfile` + profile `cloud`)

Repositório: [hamdenvogel/spring-ai-hvogel-assistant](https://github.com/hamdenvogel/spring-ai-hvogel-assistant)

Demo: https://hv-assistant-649100031966.us-central1.run.app

### Imagem Docker (GHCR)

```bash
docker pull ghcr.io/hamdenvogel/spring-ai-hvogel-assistant:v0.1.0
```

Package: [ghcr.io/hamdenvogel/spring-ai-hvogel-assistant](https://github.com/hamdenvogel/spring-ai-hvogel-assistant/pkgs/container/spring-ai-hvogel-assistant)

### CI/CD — o que cada gatilho faz

> **PR só valida; `main` integra; tag `v*` publica e faz deploy.**

| Gatilho | Ação |
|---------|------|
| Pull Request / push na `main` | CI (testes, package, Docker, scans) — **sem** deploy |
| Tag `v*` (ex.: `v0.1.0`) | GitHub Release + imagem no GHCR + deploy no Cloud Run |

Detalhes do fluxo e como versionar: [CONTRIBUTING.md](CONTRIBUTING.md).

---

## Origem e créditos

Este projeto foi **baseado** no artigo e no código hands-on da DevSuperior:

- Artigo: [Spring AI em ação: assistente de RH com RAG e Claude](https://devsuperior.com.br/blog/spring-ai-em-acao-assistente-de-rh-com-rag-e-claude)
- Repositório citado no artigo (esqueleto / código completo): [github.com/devsuperior/blog](https://github.com/devsuperior/blog) — pasta do projeto `hv-assistant` no blog

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
- Embeddings via OpenAI (sem Ollama)

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
- JaCoCo + plugin SonarQube (`sonar.projectKey=hv-assistant`)
- Issues Sonar corrigidas (ex.: método `register` no feedback, null-safety no advisor)

### Segurança e preparação para GitHub
- Sem credenciais versionadas (`.env`, `.env.cloudrun.yaml`, `application-local.*` no `.gitignore`)
- Segredos só via variáveis de ambiente (sem senha/API key hardcoded)
- Arquivos `.example`: `.env.example`, `.env.cloudrun.yaml.example`, `application-local.yml.example`, `application-local.properties.example`

### Cloud (produção demo)
- `Dockerfile` multi-stage (Java 25 / Corretto Alpine)
- Profile `cloud` (Redis URL para Actuator health)
- Cloud Run + Neon (pgvector) + Redis Cloud + Anthropic + OpenAI

---

## Pré-requisitos

- JDK 25+
- Docker e Docker Compose (só Postgres + Redis)
- Chave da API Anthropic (`ANTHROPIC_API_KEY`) — chat
- Chave da API OpenAI (`OPENAI_API_KEY`) — embeddings

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
- `.env.cloudrun.yaml`
- `application-local.yml`
- `application-local.properties`

Senhas, `ANTHROPIC_API_KEY` e `OPENAI_API_KEY` **não** ficam hardcoded — só via variáveis de ambiente.

---

## Execução local

```powershell
# Se veio de Ollama/índice antigo (1024 dims), limpe volumes antes:
# docker compose down -v

docker compose up -d
$env:ANTHROPIC_API_KEY = "sua-chave-anthropic"
$env:OPENAI_API_KEY = "sua-chave-openai"
$env:SPRING_DATASOURCE_PASSWORD = "sua-senha-postgres"   # igual ao .env / compose
$env:SPRING_PROFILES_ACTIVE = "anthropic"
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=anthropic"
```

Abra: http://localhost:8080/

No primeiro start (vector store vazio), o PDF `hvogel_politicas_rh.pdf` é indexado automaticamente via OpenAI.

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

Dashboard: http://localhost:9000/dashboard?id=hv-assistant

---

## Stack

- Java 25 / Spring Boot 4.1 / Spring AI 2.0
- Anthropic Claude (chat) + OpenAI `text-embedding-3-small` (embeddings)
- PostgreSQL + pgvector, Redis Stack (Docker local; sem Ollama)
- Docker / Cloud Run
- JaCoCo + SonarQube

---

## Segurança

- **Não** commite `.env`, `application-local.yml` ou tokens
- Credenciais de DB/Redis/APIs vêm de variáveis de ambiente (veja `.env.example`)
- Chaves: `ANTHROPIC_API_KEY` (chat) e `OPENAI_API_KEY` (embeddings)

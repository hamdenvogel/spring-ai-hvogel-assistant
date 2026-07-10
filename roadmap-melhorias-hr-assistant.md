# Roadmap de melhorias — hr-assistant

Documento de referência com sugestões para aperfeiçoar o assistente de RH da Hvogel.
Complementa o `documento-tecnico-hr-assistant.md`, que descreve o estado **atual** do sistema.

---

## 1. Contexto

O hr-assistant já possui uma base sólida:

| Capacidade | Status |
|---|---|
| RAG com pgvector + embeddings Ollama (bge-m3) | Implementado |
| Chat com Claude (streaming SSE) | Implementado |
| Memória de conversa no Redis | Implementado |
| Ingest automático na subida da aplicação | Implementado |
| Prompts com guardrails (escopo, fallback, ética) | Implementado |
| UI web com identidade visual Hvogel | Implementado |
| Advisor de log de prompt (debug) | Implementado |

Este documento lista melhorias sugeridas, organizadas por **prioridade** e **categoria**, para evoluir o projeto de "funciona" para "funciona bem, de forma confiável e mensurável".

---

## 2. Priorização resumida

| Prioridade | Melhoria | Motivo |
|---|---|---|
| **Alta** | Metadados nos chunks + re-ingest limpa | Melhora precisão e evita duplicidade |
| **Alta** | Citações de fonte na UI | Aumenta confiança do colaborador |
| **Alta** | Dataset de avaliação RAG (20–30 perguntas) | Mede qualidade de forma objetiva |
| **Média** | Autenticação no `/ingest` + rate limiting | Prepara para uso real |
| **Média** | Endpoint de histórico de chat | Completa a API |
| **Média** | Health checks compostos (DB, Redis, Ollama) | Melhora operação e troubleshooting |
| **Baixa** | Feedback 👍/👎 por resposta | Base para melhoria contínua |
| **Baixa** | Múltiplos documentos / busca híbrida | Evolução de produto |

---

## 3. Qualidade do RAG

### 3.1 Situação atual

O `IngestionService` lê o PDF com Tika, divide com `TokenTextSplitter` padrão e grava no vector store **sem metadados ricos**:

```java
TikaDocumentReader reader = new TikaDocumentReader(pdf);
List<Document> documents = reader.read();
List<Document> chunks = TokenTextSplitter.builder().build().apply(documents);
vectorStore.add(chunks);
```

### 3.2 Metadados nos chunks

**Objetivo:** cada chunk carregar informação que ajude na recuperação, citação e manutenção.

**Campos sugeridos:**

| Metadado | Exemplo | Uso |
|---|---|---|
| `source` | `hvogel_politicas_rh.pdf` | Identificar origem |
| `version` | `1.1` | Controle de versão do manual |
| `section` | `4.2 Férias` | Citação na resposta |
| `page` | `12` | Referência cruzada |
| `company` | `Hvogel` | Filtro em ambientes multi-tenant |

**Benefícios:**
- Respostas com citação precisa ("conforme seção 4.2 – Férias")
- Filtros no retrieval (`filterExpression` por documento ou versão)
- Rastreabilidade em auditoria

### 3.3 Chunking configurável

**Problema:** splitter genérico pode cortar seções densas (férias, licenças, benefícios) no meio de parágrafos importantes.

**Sugestão:**
- Expor no `application.yml`:
  - `app.ingest.chunk-size` (tokens)
  - `app.ingest.chunk-overlap` (tokens)
- Valores iniciais sugeridos: 512 tokens, overlap 64–128
- Alternativa avançada: split por headings do markdown antes de gerar o PDF

### 3.4 Re-ingestão limpa

**Problema:** ao atualizar o PDF, novos chunks são adicionados sem remover os antigos → respostas inconsistentes.

**Sugestão:**
1. Antes de indexar, deletar chunks com `metadata.source = hvogel_politicas_rh.pdf`
2. Ingerir a nova versão
3. Registrar no log: versão anterior → nova versão, quantidade de chunks

**Onde aplicar:**
- `IngestionService.ingest()`
- `StartupIngestionRunner` (opcional: re-ingest se versão mudou)

### 3.5 Citações de fonte na interface web

**Objetivo:** abaixo de cada resposta do assistente, exibir algo como:

> Baseado em: **Seção 4.2 – Férias** (manual v1.1)

**Implementação possível:**
- Custom advisor que captura os `Document` recuperados e envia metadados via header SSE ou evento JSON separado
- Ou pedir ao modelo incluir a seção (já previsto no prompt) e destacar na UI com CSS

### 3.6 Busca híbrida (evolução futura)

Combinar busca vetorial com busca por palavra-chave para termos específicos de RH:
- "CLT", "180 dias", "VR", "home office", "licença-maternidade"

Útil quando o embedding não captura bem siglas ou números exatos.

---

## 4. Confiança e anti-alucinação

### 4.1 Situação atual

O `context-prompt.st` já define fallback quando o contexto não contém a informação:

```
"Não encontrei essa informação nas políticas oficiais da Hvogel.
Procure o RH: rh@hvogel.com.br ou (61) 3321-7700, ramal 200."
```

### 4.2 Short-circuit por score de similaridade

**Sugestão:** se nenhum chunk passar do `similarity-threshold`, responder o fallback **sem chamar o Claude**.

**Benefícios:**
- Menor custo de tokens
- Menor risco de alucinação
- Resposta mais rápida

**Implementação:** custom advisor ou lógica no `ChatService` antes do `QuestionAnswerAdvisor`.

### 4.3 Classificador de escopo

**Objetivo:** detectar perguntas claramente fora de RH (programação, opinião, notícias) antes do RAG.

**Opções:**
- Regras simples (palavras-chave)
- Chamada leve ao modelo ("esta pergunta é sobre RH interno? sim/não")
- Prompt no system message (já parcialmente coberto)

### 4.4 Dataset de avaliação RAG

**Objetivo:** medir qualidade de forma repetível e objetiva.

**Estrutura sugerida** (`src/test/resources/rag-evaluation.json`):

```json
[
  {
    "question": "Quantos dias de férias tenho direito?",
    "expectedSection": "Férias",
    "mustContain": ["30 dias"],
    "mustNotContain": ["não encontrei"]
  },
  {
    "question": "Qual a licença-maternidade?",
    "expectedSection": "Licenças",
    "mustContain": ["120 dias", "180 dias"]
  }
]
```

**Métricas:**
- Taxa de acerto (resposta contém termos esperados)
- Taxa de fallback (quando deveria responder vs. quando deveria fallback)
- Latência média (retrieval + geração)

**Execução:** teste de integração ou script Maven profile `rag-eval`.

---

## 5. Segurança e uso em ambiente real

### 5.1 Situação atual

Endpoints abertos, sem autenticação:
- `POST /chat/stream` — qualquer um pode conversar
- `POST /ingest` — qualquer um pode indexar PDFs

Aceitável para demo local; **inadequado para intranet/produção**.

### 5.2 Proteger o endpoint `/ingest`

| Abordagem | Complexidade | Uso |
|---|---|---|
| API Key no header (`X-Ingest-Key`) | Baixa | Demo / staging |
| Spring Security + role `ADMIN` | Média | Intranet |
| OAuth2 / Azure AD | Alta | Empresa |

### 5.3 Rate limiting

**Objetivo:** evitar abuso de tokens da Anthropic e sobrecarga do Ollama.

**Sugestões:**
- Bucket por `X-Conversation-Id` ou IP
- Limite sugerido: 20 perguntas/minuto por sessão
- Biblioteca: Bucket4j ou Spring Cloud Gateway (se houver API Gateway)

### 5.4 Validação de entrada

| Campo | Regra sugerida |
|---|---|
| `message` | Obrigatório, 1–2000 caracteres, trim |
| `X-Conversation-Id` | Obrigatório, UUID ou string alfanumérica (max 64) |
| Arquivo no `/ingest` | Apenas PDF, max 10 MB |

**Implementação:** `@Valid` no DTO + `@RequestHeader` com validação ou filter.

### 5.5 Dados sensíveis

Reforçar no prompt e em validação:
- Não solicitar CPF, salário individual, dados médicos
- Em temas de assédio/denúncia, sempre encaminhar ao Canal de Ética (já no prompt)

---

## 6. Experiência do usuário (UI)

### 6.1 Situação atual

Interface em `src/main/resources/static/index.html` com:
- Logo Hvogel, bolhas de chat, sugestões rápidas
- Indicador animado de digitação (três pontos)
- Streaming SSE token a token

### 6.2 Histórico de conversa

**Endpoint sugerido:**

```
GET /chat/history?conversationId={id}
```

Retorna lista de mensagens da memória Redis para reconstruir a conversa ao recarregar a página.

> Nota: o `CURL-EXEMPLOS.md` menciona este endpoint, mas ele ainda **não está implementado** no código.

### 6.3 Feedback por resposta

Botões 👍 / 👎 abaixo de cada resposta do assistente.

**Persistência:** tabela simples ou log estruturado com:
- `conversationId`, pergunta, resposta, rating, timestamp

**Uso:** identificar perguntas mal respondidas e ajustar RAG/prompts.

### 6.4 Ações na bolha de resposta

- Copiar resposta
- Exportar conversa (TXT ou PDF)
- Link direto para contato do RH quando fallback

### 6.5 Estados visuais mais claros

| Estado | Indicador sugerido |
|---|---|
| Aguardando resposta | Três pontos animados (já implementado) |
| Buscando no manual | "Consultando políticas..." |
| Sem contexto encontrado | Mensagem de fallback destacada em amarelo |
| Erro de rede/servidor | Mensagem de erro com botão "Tentar novamente" |

---

## 7. Observabilidade

### 7.1 Health checks compostos

Expandir `/actuator/health` para incluir:

| Componente | Verificação |
|---|---|
| PostgreSQL (pgvector) | `SELECT 1` |
| Redis | `PING` |
| Ollama | `GET /api/tags` (modelo bge-m3 presente) |

**Implementação:** Spring Boot Actuator custom `HealthIndicator` beans.

### 7.2 Métricas customizadas

| Métrica | Tipo | Descrição |
|---|---|---|
| `rag.retrieval.duration` | Timer | Tempo de busca no vector store |
| `rag.chunks.retrieved` | Gauge | Quantidade de chunks por pergunta |
| `rag.fallback.count` | Counter | Respostas sem contexto suficiente |
| `chat.tokens.estimated` | Counter | Tokens consumidos (estimativa) |
| `ingest.chunks.total` | Gauge | Total de chunks indexados |

**Exposição:** `/actuator/metrics` ou Prometheus.

### 7.3 Logs estruturados

Campos sugeridos em cada interação de chat:

```json
{
  "conversationId": "abc-123",
  "question": "Como funciona o home office?",
  "chunksRetrieved": 4,
  "topScore": 0.72,
  "fallback": false,
  "durationMs": 2340
}
```

O `PromptLoggingAdvisor` (nível DEBUG) já ajuda; logs estruturados em INFO facilitam análise em produção.

---

## 8. Testes

### 8.1 Situação atual

| Teste | Arquivo |
|---|---|
| Context load da aplicação | `HrAssistantApplicationTests.java` |
| Redis Chat Memory Repository | `RedisChatMemoryRepositoryTest.java` |

Cobertura mínima; sem testes de RAG, ingest ou chat.

### 8.2 Testes sugeridos

| Tipo | O que validar |
|---|---|
| **Integração (Testcontainers)** | Postgres + Redis + fluxo ingest → chat |
| **Contrato SSE** | Formato `data:` do `/chat/stream` |
| **Ingest** | PDF de teste gera N chunks com metadados |
| **RAG evaluation** | Dataset fixo de perguntas/respostas esperadas |
| **StartupIngestionRunner** | Não re-ingere se vector store já tem dados |

### 8.3 Profile de teste

Manter `app.ingest.auto-on-startup=false` nos testes (já configurado) e usar `@DynamicPropertySource` para containers efêmeros.

---

## 9. Operacional e DevOps

### 9.1 Dependências na subida

**Problema:** race condition — Spring sobe antes do Ollama ter o bge-m3 pronto.

**Sugestões:**
- `depends_on` com `condition: service_healthy` no docker-compose da app (se containerizada)
- Retry no `StartupIngestionRunner` se embedding falhar
- Script de wait-for-it antes do `spring-boot:run`

### 9.2 Variáveis de ambiente

Centralizar em `.env` (não commitar):

| Variável | Descrição |
|---|---|
| `ANTHROPIC_API_KEY` | Chave da API Claude |
| `SPRING_DATASOURCE_URL` | JDBC do Postgres |
| `SPRING_AI_OLLAMA_BASE_URL` | URL do Ollama (porta 11435 no Docker) |
| `APP_INGEST_AUTO_ON_STARTUP` | true/false |

Referência: `.env.example` no repositório.

### 9.3 CI/CD

Pipeline mínimo sugerido:

```yaml
steps:
  - docker compose up -d db redis ollama
  - mvnw test
  - mvnw verify -Prag-eval   # opcional: avaliação RAG
```

### 9.4 Limpeza e manutenção

Documentado em `comandos-cmd-cmder-hr-assistant.md` (seção 12):
- `docker compose down -v` — apaga volumes (reindexação necessária)
- `docker system prune` — limpeza geral

---

## 10. Funcionalidades avançadas (evolução de produto)

### 10.1 Múltiplos documentos

Indexar além do manual principal:
- Código de conduta
- FAQ de RH
- Formulários e procedimentos

Cada documento com `metadata.source` distinto; retrieval pode filtrar ou buscar em todos.

### 10.2 Integração com canais de RH

Quando a IA não souber responder (fallback):
- Abrir e-mail pré-preenchido para `rh@hvogel.com.br`
- Webhook para sistema de tickets (Jira Service Management, GLPI, etc.)

### 10.3 Multilíngue

Detectar idioma da pergunta e responder em PT ou EN (útil para colaboradores internacionais).

### 10.4 Personalização por perfil

Futuro: respostas diferentes para estagiário vs. CLT vs. liderança (requer autenticação + metadados de perfil).

---

## 11. Plano de implementação sugerido

### Fase 1 — Qualidade RAG (1–2 semanas)

- [ ] Metadados nos chunks (source, section, version)
- [ ] Chunking configurável no `application.yml`
- [ ] Re-ingest limpa (delete + add)
- [ ] Citações de fonte na UI

### Fase 2 — Confiança e avaliação (1 semana)

- [ ] Short-circuit por similarity score
- [ ] Dataset de 20–30 perguntas de avaliação
- [ ] Teste automatizado de RAG eval

### Fase 3 — API e operação (1 semana)

- [ ] `GET /chat/history`
- [ ] Validação de entrada (DTO + headers)
- [ ] Health indicators (DB, Redis, Ollama)
- [ ] Métricas básicas no Actuator

### Fase 4 — Segurança (1 semana)

- [ ] API Key no `/ingest`
- [ ] Rate limiting no `/chat/stream`
- [ ] Spring Security básico (opcional)

### Fase 5 — UX e produto (contínuo)

- [ ] Feedback 👍/👎
- [ ] Exportar conversa
- [ ] Múltiplos documentos
- [ ] Busca híbrida

---

## 12. Referências no projeto

| Arquivo | Conteúdo |
|---|---|
| `documento-tecnico-hr-assistant.md` | Arquitetura e comportamento atual |
| `comandos-cmd-cmder-hr-assistant.md` | Comandos operacionais (CMD) |
| `CURL-EXEMPLOS.md` | Exemplos de curl (PowerShell/CMD) |
| `application.yml` | Parâmetros RAG, ingest e memória |
| `ChatClientConfig.java` | System prompt e advisors |
| `context-prompt.st` | Template RAG com fallback |
| `IngestionService.java` | Pipeline de ingestão |
| `static/index.html` | Interface web do chat |

---

## 13. Conclusão

O hr-assistant já demonstra competência em **Spring AI**, **RAG**, **streaming**, **memória distribuída** e **prompt engineering**. As melhorias deste roadmap elevam o projeto de demonstração técnica para **produto confiável**:

1. **RAG de qualidade** — metadados, chunking fino, re-ingest limpa
2. **Confiança mensurável** — dataset de avaliação, short-circuit, citações
3. **Pronto para produção** — segurança, observabilidade, testes
4. **Experiência completa** — histórico, feedback, estados visuais claros

Esses pontos são especialmente valorizados em portfólio e entrevistas técnicas: mostram que o sistema não apenas funciona, mas foi pensado para **operar com qualidade em ambiente real**.

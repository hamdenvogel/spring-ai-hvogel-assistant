# Arquitetura Hexagonal — HV Assistant (guia detalhado)

Documento **mastigado** da migração do HV Assistant para **Ports & Adapters** (arquitetura hexagonal), **sem** Domain-Driven Design completo.

| Item | Valor |
|------|--------|
| Projeto | `projects/hv-assistant` |
| Pacote base | `br.com.hvogel.hv_assistant` |
| Estilo | Hexagonal **enxuta** (ports + adapters + application) |
| O que **não** é | DDD completo (sem agregados, invariantes ricas, bounded contexts) |
| Endpoints | Inalterados (`/chat/stream`, `/chat/feedback`, `/ingest`, `/api/info`) |

---

## 1. Por que existe esse documento

Depois da migração, o código ficou organizado de outro jeito. Sem o “porquê”, a pasta `domain/port/out` parece burocracia. Com o porquê, vira uma decisão de engenharia que você consegue explicar em entrevista.

Este guia cobre:

1. **Conceitos técnicos** da hexagonal (o que é porta, adapter, dependência invertida…)
2. **Como isso aparece no HV Assistant** (arquivo por arquivo)
3. **Antes × depois**
4. **Fluxos reais** (chat, ingestão, feedback)
5. **O que NÃO fizemos** (e por quê)

---

## 2. Conceitos técnicos (aula)

### 2.1 O problema que a hexagonal resolve

Em aplicações Spring “clássicas”, é comum o **domínio** (a intenção do negócio) ficar **misturado** com frameworks:

```text
ChatService
  └── importa ChatClient (Spring AI)
  └── importa ChatMemory (Spring AI)
  └── conhece VectorStore indiretamente via advisors
```

Consequências:

| Problema | Efeito prático |
|----------|----------------|
| Acoplamento forte | Trocar Spring AI / Redis / pgvector mexe em tudo |
| Testes caros | Para testar “perguntar ao assistente”, você mocka tipos da lib |
| Regra escondida | Difícil ver *o que* o sistema faz sem ler framework |

A arquitetura hexagonal (Alistair Cockburn, ~2005) propõe: **o núcleo da aplicação não depende de detalhes externos**. Detalhes (HTTP, banco, LLM, PDF) se **encaixam** no núcleo, não o contrário.

### 2.2 Analogia do hexágono

Imagine um **tomada elétrica universal** no meio da casa (o domínio):

- De um lado, chega energia de fontes diferentes: tomada da rua, gerador, USB-C… → **adapters de entrada** (quem *dispara* o sistema).
- Do outro, a casa *consome* serviços externos: geladeira, Wi‑Fi, TV… → **adapters de saída** (quem o sistema *chama*).
- A “tomada” em si é um **contrato** (a **porta**): “preciso de 110V / preciso de água quente”. Quem implementa o contrato pode mudar sem reformar a casa.

O desenho costuma ser um hexágono (daí o nome), mas o número de lados é só visual: o importante é **centro estável + bordas substituíveis**.

```text
                 ┌─────────────── adapters IN ───────────────┐
                 │  HTTP  ·  CLI  ·  Startup  ·  Mensageria  │
                 └────────────────────┬──────────────────────┘
                                      │ portas IN (use cases)
                                      ▼
                 ┌──────────────────────────────────────────┐
                 │              NÚCLEO (domain +            │
                 │              application)                │
                 │   "o que o sistema FAZ"                  │
                 └────────────────────┬─────────────────────┘
                                      │ portas OUT
                                      ▼
                 ┌─────────────── adapters OUT ──────────────┐
                 │  LLM  ·  Vector DB  ·  Redis  ·  Log/DB   │
                 └───────────────────────────────────────────┘
```

### 2.3 Port (porta) — o contrato

Uma **porta** é uma **interface** que descreve uma capacidade, **sem** dizer *como* ela é feita.

No HV Assistant:

```java
// domain/port/out/AnswerGeneratorPort.java
public interface AnswerGeneratorPort {
    Flux<String> stream(ChatQuestion question);
}
```

Isso diz: *“eu preciso gerar uma resposta em streaming para esta pergunta”*.  
Não diz: *“use ChatClient do Spring AI com QuestionAnswerAdvisor”*.

Há dois tipos:

| Tipo | Nome comum | Quem chama quem | Exemplo no projeto |
|------|------------|-----------------|---------------------|
| **Porta de entrada** (driving / primary) | Use case | O mundo externo chama o núcleo | `AskQuestionUseCase` |
| **Porta de saída** (driven / secondary) | Port / SPI | O núcleo chama o mundo externo | `KnowledgeBasePort` |

**Regra de ouro:** o núcleo depende de **interfaces**. Quem **implementa** a interface fica na borda.

### 2.4 Adapter (adaptador) — a implementação na borda

Um **adapter** traduz o mundo real para o contrato da porta (ou o contrário).

| Direção | Adapter no HV Assistant | Traduz |
|---------|-------------------------|--------|
| **IN** | `ChatController` | JSON HTTP + header → `ChatQuestion` → use case |
| **IN** | `SpringResourceDocumentSource` | `Resource` do Spring → `DocumentSource` do domínio |
| **OUT** | `SpringAiAnswerGenerator` | `AnswerGeneratorPort` → `ChatClient` Spring AI |
| **OUT** | `PgVectorKnowledgeBase` | `KnowledgeBasePort` → `VectorStore` |
| **OUT** | `LoggingFeedbackRecorder` | `FeedbackRecorderPort` → log SLF4J |

Se amanhã o feedback for para Postgres, você cria `JdbcFeedbackRecorder implements FeedbackRecorderPort` e **não mexe** em `RegisterFeedbackService`.

### 2.5 Inversão de dependência (DIP) — o coração técnico

Princípio (SOLID):

> Módulos de alto nível não devem depender de módulos de baixo nível. Ambos devem depender de abstrações.

**Antes (dependência “errada”):**

```text
AskQuestionService  ──depends──►  ChatClient (Spring AI)
     (alto nível)                    (baixo nível / detalhe)
```

**Depois (dependência invertida):**

```text
AskQuestionService  ──depends──►  AnswerGeneratorPort  ◄──implements──  SpringAiAnswerGenerator
     (alto nível)                    (abstração)                         (baixo nível)
```

A seta de **compilação** do núcleo aponta para a interface. O adapter aponta para a interface *e* para o framework. O núcleo **não conhece** Spring AI.

Em Java isso aparece assim:

```java
// application — só conhece a porta
public class AskQuestionService implements AskQuestionUseCase {
    private final AnswerGeneratorPort answerGenerator; // interface!

    public AskQuestionService(AnswerGeneratorPort answerGenerator) {
        this.answerGenerator = answerGenerator;
    }
}
```

```java
// adapter — conhece a porta E o framework
@Component
public class SpringAiAnswerGenerator implements AnswerGeneratorPort {
    private final ChatClient chatClient; // Spring AI

    public Flux<String> stream(ChatQuestion question) { /* ... */ }
}
```

O Spring (DI container) **liga** as duas pontas em runtime via `ApplicationConfig` + `@Component`.

### 2.6 Driving vs Driven (quem dirige quem)

| Termo | Significado | No HV Assistant |
|-------|-------------|-----------------|
| **Driving adapter** | Algo de fora **inicia** um caso de uso | Browser → `ChatController`; boot → `StartupIngestionRunner` |
| **Driven adapter** | O núcleo **solicita** um serviço externo | `SpringAiAnswerGenerator`, `PgVectorKnowledgeBase` |

Frase útil: *driving = quem aperta o botão; driven = quem o botão aciona por baixo.*

### 2.7 Application / use case — a orquestração

A camada **application** implementa as portas de entrada e **coordena** as portas de saída. Ela não sabe HTTP nem Claude — só a sequência:

```text
IngestDocumentService:
  1. documentParser.parse(source)   → List<DocumentChunk>
  2. knowledgeBase.store(chunks)    → int
```

Isso é o “roteiro” do caso de uso. Se a sequência mudar (ex.: validar PDF antes), o lugar certo é aqui — não no controller e não no Tika.

### 2.8 Domain model (no nosso estilo enxuto)

Em DDD “pesado”, o domínio teria entidades com comportamento e invariantes.  
Aqui o domínio tem **modelos simples** (records + uma interface de fonte de documento), porque a regra de RH **não está no Java** — está no PDF + system prompt.

Ainda assim, esses tipos têm valor:

- Desacoplam DTOs web (`ChatRequest`) do núcleo (`ChatQuestion`)
- Evitam `Resource` / `MultipartFile` / `Document` do Spring AI no application

### 2.9 Hexagonal × camadas clássicas × Clean Architecture

| Estilo | Ideia | Relação |
|--------|-------|---------|
| **Camadas (Controller → Service → Repository)** | Separação técnica vertical | Fácil cair em “Service inchado” acoplado ao framework |
| **Hexagonal** | Núcleo + portas + adapters | Foco em **substituir bordas** |
| **Clean Architecture** (Uncle Bob) | Círculos de dependência | Hexagonal é **irmã próxima**; nomes mudam (use case, gateway) |

No HV Assistant usamos a linguagem hexagonal: `port.in` / `port.out` / `adapter`.

### 2.10 Hexagonal × DDD — não são a mesma coisa

| | Hexagonal | DDD |
|-|-----------|-----|
| Pergunta central | *Como isolar o núcleo das bordas?* | *Como modelar o domínio complexo?* |
| Artefatos típicos | Ports, adapters | Agregados, VOs, repositórios de domínio, contextos |
| Necessita domínio rico? | Não | Sim, para valer a pena |

Por isso fizemos **hexagonal sem DDD completo**: o isolamento da borda (Spring AI) vale a pena; inventar agregados para um `Feedback` que só vira log **não**.

### 2.11 O que “vaza” e o que isolamos (pragmatismo)

Hexagonal “100% pura” evitaria até `Flux` (Reactor) nas portas. Neste projeto:

| Decisão | Pura? | Por quê aceitamos |
|---------|-------|-------------------|
| `Flux<String>` nas portas de chat | Não | O produto **é** SSE; inventar `TokenStream` só para doutrina |
| RAG via `QuestionAnswerAdvisor` **dentro** do adapter | Parcial | Evita reescrever retrieval na mão; Spring AI continua encapsulado no adapter |
| SLF4J na application | Leve | Logging transversal; não acopla a Spring AI |

Documentamos isso de propósito: hexagonal **útil**, não hexagonal **dogmática**.

---

## 3. Antes × depois (no HV Assistant)

### Antes (por feature + Spring AI direto)

```text
chat/
  ChatController → ChatService → ChatClient
  FeedbackController → FeedbackService (log)
ingestion/
  IngestionController → IngestionService → Tika + VectorStore
  StartupIngestionRunner → IngestionService + VectorStore
config/
  ChatClientConfig (system prompt + advisors + Redis)
```

Problema: `ChatService` e `IngestionService` **compilavam contra** tipos Spring AI.

### Depois (hexágono)

```text
adapter.in.web  →  port.in  →  application  →  port.out  →  adapter.out.*
```

O `ChatClient` / `VectorStore` / Tika só aparecem em `adapter.out.springai` (e no `ChatClientConfig`).

---

## 4. Estrutura de pastas (detalhada)

```text
br.com.hvogel.hv_assistant
├── HvAssistantApplication.java          # @SpringBootApplication — sobe o container DI
│
├── domain/                              # NÚCLEO: contratos + modelos
│   ├── model/                           # dados do “mundo do problema”
│   │   ├── ChatQuestion.java
│   │   ├── DocumentChunk.java
│   │   ├── DocumentSource.java          # interface (não é Spring Resource)
│   │   └── Feedback.java
│   └── port/
│       ├── in/                          # o que o mundo EXTERNO pode pedir
│       │   ├── AskQuestionUseCase.java
│       │   ├── IngestDocumentUseCase.java
│       │   └── RegisterFeedbackUseCase.java
│       └── out/                         # o que o NÚCLEO precisa do exterior
│           ├── AnswerGeneratorPort.java
│           ├── KnowledgeBasePort.java
│           ├── DocumentParserPort.java
│           └── FeedbackRecorderPort.java
│
├── application/                         # implementa port.in; usa port.out
│   ├── AskQuestionService.java
│   ├── IngestDocumentService.java
│   └── RegisterFeedbackService.java
│
├── adapter/
│   ├── in/web/                          # driving adapters (HTTP + boot)
│   │   ├── ChatController.java
│   │   ├── FeedbackController.java
│   │   ├── IngestionController.java
│   │   ├── AppInfoController.java
│   │   ├── StartupIngestionRunner.java
│   │   ├── SpringResourceDocumentSource.java
│   │   └── dto/                         # JSON da API (não são domain models)
│   │       ├── ChatRequest.java
│   │       └── FeedbackRequest.java
│   └── out/
│       ├── springai/                    # driven adapters (IA / vector / PDF)
│       │   ├── SpringAiAnswerGenerator.java
│       │   ├── PgVectorKnowledgeBase.java
│       │   ├── TikaDocumentParser.java
│       │   └── PromptLoggingAdvisor.java
│       └── logging/
│           └── LoggingFeedbackRecorder.java
│
└── config/                              # composição / wiring (fora do domínio)
    ├── ApplicationConfig.java           # @Bean dos use cases
    └── ChatClientConfig.java            # ChatClient, memória Redis, advisors RAG
```

### Regra de dependência entre pacotes

```text
adapter  →  application / domain.port / domain.model
application  →  domain.port / domain.model
domain  →  (quase nada de framework de negócio; Flux é exceção pragmática)
config  →  liga application + adapters (Spring)
```

Ideal futuro (opcional): teste ArchUnit garantindo que `domain` **não** importa `org.springframework.ai`.

---

## 5. Cada peça, arquivo a arquivo

### 5.1 `domain.model`

| Classe | Tipo | Responsabilidade |
|--------|------|------------------|
| `ChatQuestion` | `record` | Mensagem do usuário + `conversationId` (memória Redis) |
| `DocumentChunk` | `record` | Texto já “picado” para embeddings / vector store |
| `DocumentSource` | `interface` | Ler um documento sem depender de `Resource` |
| `Feedback` | `record` | 👍/👎 + metadados da mensagem |

Por que `DocumentSource` é interface e não record? Porque abrir stream / `exists()` / nome do arquivo são **comportamentos** de I/O — a implementação real (`SpringResourceDocumentSource`) vive no adapter.

### 5.2 `domain.port.in` (driving ports)

| Interface | Método | Significado em português |
|-----------|--------|--------------------------|
| `AskQuestionUseCase` | `ask(ChatQuestion) → Flux<String>` | “Responda esta dúvida de RH em streaming” |
| `IngestDocumentUseCase` | `ingest(DocumentSource) → int` | “Indexe este PDF; devolva quantos chunks” |
| `RegisterFeedbackUseCase` | `register(Feedback)` | “Registre a avaliação do usuário” |

Quem **chama**: controllers e `StartupIngestionRunner`.  
Quem **implementa**: classes em `application/`.

### 5.3 `domain.port.out` (driven ports)

| Interface | Métodos | Quem implementa hoje |
|-----------|---------|----------------------|
| `AnswerGeneratorPort` | `stream(ChatQuestion)` | `SpringAiAnswerGenerator` |
| `KnowledgeBasePort` | `store(chunks)`, `hasAnyDocuments()` | `PgVectorKnowledgeBase` |
| `DocumentParserPort` | `parse(source)` | `TikaDocumentParser` |
| `FeedbackRecorderPort` | `record(feedback)` | `LoggingFeedbackRecorder` |

### 5.4 `application` (use cases)

**`AskQuestionService`**

- Recebe a pergunta
- Loga início / fim / erro do stream
- Delega geração ao `AnswerGeneratorPort`

**`IngestDocumentService`**

- Parse → lista de chunks
- Store → quantidade indexada  
  (orquestração explícita em duas portas)

**`RegisterFeedbackService`**

- Só encaminha ao `FeedbackRecorderPort`  
  (hoje trivial; amanhã pode validar rating, enriquecer, etc.)

Essas classes **não** têm `@Service`. São POJOs registrados em `ApplicationConfig` — o núcleo não “parece” Spring.

### 5.5 `adapter.in.web`

| Classe | Papel técnico |
|--------|----------------|
| `ChatController` | `POST /chat/stream` (SSE); monta `ChatQuestion` |
| `FeedbackController` | `POST /chat/feedback`; DTO → `Feedback` |
| `IngestionController` | `POST /ingest` multipart; `MultipartFile` → `DocumentSource` |
| `AppInfoController` | `GET /api/info` (UI); infra de apresentação |
| `StartupIngestionRunner` | Driving adapter de **boot**: se KB vazio, chama ingest |
| `SpringResourceDocumentSource` | Adapter `Resource` → `DocumentSource` |
| `dto.*` | Formato JSON da API (borda HTTP) |

### 5.6 `adapter.out.*`

| Classe | Detalhe que encapsula |
|--------|------------------------|
| `SpringAiAnswerGenerator` | `ChatClient.prompt().user().advisors(...).stream()` |
| `PgVectorKnowledgeBase` | `Document` do Spring AI + `VectorStore.add` / `similaritySearch` |
| `TikaDocumentParser` | Tika + `TokenTextSplitter` → `DocumentChunk` |
| `PromptLoggingAdvisor` | Advisor Spring AI (log do prompt RAG) |
| `LoggingFeedbackRecorder` | Formato do log de feedback |

### 5.7 `config`

**`ApplicationConfig`** — composição:

```java
@Bean
AskQuestionUseCase askQuestionUseCase(AnswerGeneratorPort port) {
    return new AskQuestionService(port);
}
```

Aqui o Spring injeta a implementação `@Component` de `AnswerGeneratorPort` (`SpringAiAnswerGenerator`).

**`ChatClientConfig`** — monta o `ChatClient` com:

- system prompt de RH
- `MessageChatMemoryAdvisor` (Redis)
- `QuestionAnswerAdvisor` (RAG / pgvector)
- `PromptLoggingAdvisor`

Tudo isso é **detalhe de infraestrutura de IA**, por isso fica em config/adapter — não no `AskQuestionService`.

---

## 6. Fluxos reais (passo a passo)

### 6.1 Chat em streaming (caso principal)

```text
1. Browser POST /chat/stream
   Header: X-Conversation-Id
   Body: { "message": "Quantos dias de férias?" }

2. ChatController (adapter IN)
   → new ChatQuestion(message, conversationId)
   → askQuestionUseCase.ask(...)

3. AskQuestionService (application)
   → answerGenerator.stream(question)

4. SpringAiAnswerGenerator (adapter OUT)
   → ChatClient (advisors: memória Redis + RAG + logging)
   → Flux<String> tokens

5. Controller devolve SSE para o browser
```

**Onde está o RAG?** Dentro do `ChatClient` configurado — o domínio só pediu “gere a resposta”. Trocar o provedor de embeddings/LLM muda o adapter/config, não o use case.

### 6.2 Ingestão de PDF

```text
1. POST /ingest (multipart)  OU  StartupIngestionRunner no boot

2. Adapter IN monta DocumentSource
   (SpringResourceDocumentSource)

3. IngestDocumentService
   a) DocumentParserPort.parse(source)     → List<DocumentChunk>
   b) KnowledgeBasePort.store(chunks)      → int

4. TikaDocumentParser lê PDF e fatia tokens
5. PgVectorKnowledgeBase grava no Postgres/pgvector
```

### 6.3 Feedback 👍/👎

```text
1. POST /chat/feedback + FeedbackRequest (DTO)
2. FeedbackController → Feedback (domain)
3. RegisterFeedbackService → FeedbackRecorderPort
4. LoggingFeedbackRecorder → log INFO
```

Amanhã: novo adapter `JdbcFeedbackRecorder` + um `@Primary` / profile — **mesmo** use case.

---

## 7. Wiring (como o Spring “fecha” o hexágono)

```text
@SpringBootApplication
        │
        ├─ component-scan acha @Component / @RestController nos adapters
        │     SpringAiAnswerGenerator, PgVectorKnowledgeBase, TikaDocumentParser,
        │     LoggingFeedbackRecorder, Controllers, StartupIngestionRunner
        │
        ├─ ChatClientConfig cria ChatClient, ChatMemory, RedisChatMemoryRepository
        │
        └─ ApplicationConfig cria
              AskQuestionUseCase      → new AskQuestionService(AnswerGeneratorPort)
              IngestDocumentUseCase   → new IngestDocumentService(parser, kb)
              RegisterFeedbackUseCase → new RegisterFeedbackService(recorder)
```

Sem o container DI, você faria `new` na mão (útil em testes unitários da application — e é o que os testes fazem com Mockito).

---

## 8. Testes — por que a hexagonal ajuda

| Camada testada | Estratégia | Exemplo |
|----------------|------------|---------|
| **Application** | Mock das **portas out** | `AskQuestionServiceTest` mocka `AnswerGeneratorPort` |
| **Adapter IN** | `@WebMvcTest` + mock do **use case** | `ChatControllerTest` mocka `AskQuestionUseCase` |
| **Adapter OUT** | Mock do framework / recurso real | `SpringAiAnswerGeneratorTest` mocka `ChatClient`; `TikaDocumentParserTest` lê PDF real |
| **Config** | `@SpringBootTest` | `ChatClientConfigTest` |

Antes, testar o service de chat exigia mockar a API fluente do `ChatClient`. Agora:

- Use case: mock de **uma** interface (`AnswerGeneratorPort`)
- Adapter Spring AI: teste isolado da tradução para `ChatClient`

Isso é um dos ganhos mais concretos da hexagonal em código pequeno.

---

## 9. O que foi removido

Pacotes antigos apagados após a migração:

- `...chat.ChatService`, `ChatController`, `Feedback*`, `PromptLoggingAdvisor` (versão antiga)
- `...chat.dto.*`
- `...ingestion.*`
- `...config.AppInfoController` (movido para `adapter.in.web`)

Logging YAML atualizado para o FQN novo:

`br.com.hvogel.hv_assistant.adapter.out.springai.PromptLoggingAdvisor`

---

## 10. Glossário rápido

| Termo | Definição curta |
|-------|-----------------|
| **Hexagonal / Ports & Adapters** | Núcleo isolado; bordas conectadas por contratos |
| **Port** | Interface (contrato) entre núcleo e exterior |
| **Adapter** | Implementação que fala a língua de um detalhe (HTTP, DB, LLM) |
| **Driving / primary** | Adapter que **inicia** um caso de uso |
| **Driven / secondary** | Adapter que o núcleo **usa** como serviço |
| **Use case** | Operação de aplicação (porta de entrada + service) |
| **DIP** | Núcleo depende de abstração, não de detalhe |
| **DTO** | Objeto de transporte da borda (JSON); ≠ modelo de domínio |
| **SSE** | Server-Sent Events — stream HTTP de tokens |
| **RAG** | Retrieval-Augmented Generation — busca contexto + LLM |
| **DDD** | Modelagem rica de domínio (não aplicada por completo aqui) |

---

## 11. Como explicar em entrevista (30 segundos)

> “O HV Assistant usa arquitetura hexagonal enxuta. O núcleo define use cases e ports; controllers HTTP e Spring AI são adapters. Assim o ChatClient e o VectorStore não vazam para a aplicação — consigo trocar ou mockar a borda sem reescrever o fluxo de perguntar, ingerir PDF ou registrar feedback. Não apliquei DDD completo porque a regra de RH está no manual e no prompt, não em invariantes de código.”

---

## 12. Como validar localmente

```cmd
cd /d C:\Hamden\Sistemas\Backend\hvogel\projects\hv-assistant
.\mvnw.cmd clean install
```

Última validação conhecida: **34 testes**, `BUILD SUCCESS`.

---

## 13. Próximos passos (opcionais)

1. **Publicar** no GitHub (PR → CI → merge; tag `v0.2.0` se quiser produção).
2. **ArchUnit** — regra: `domain` não importa `org.springframework.ai`.
3. **Persistir feedback** — novo adapter OUT, mesma porta.
4. (Avançado) Extrair retrieval RAG para portas explícitas, se quiser controle fino fora dos advisors.

---

## 14. Resumo em uma frase

> **Hexagonal no HV Assistant = o núcleo fala em “perguntar / ingerir / registrar”; HTTP e Spring AI só traduzem isso nas bordas.**

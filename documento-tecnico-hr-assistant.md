# Documento tecnico - hr-assistant

## 1. Objetivo do projeto

O hr-assistant e um assistente virtual de RH com RAG (Retrieval-Augmented Generation) para responder perguntas de colaboradores com base em politicas internas da empresa Hvogel Tecnologia Ltda.

A arquitetura foi desenhada para:
- Responder com base documental (manual de RH)
- Reduzir alucinacao usando contexto recuperado
- Manter memoria de conversa por sessao
- Separar claramente chat, ingestao e infraestrutura

## 2. Stack e componentes principais

- Backend: Spring Boot 4.1
- IA de chat: Anthropic (profile anthropic)
- Embeddings: Ollama com modelo bge-m3
- Vector Store: PostgreSQL + pgvector
- Memoria de conversa: Redis (RedisChatMemoryRepository)
- Leitura de documentos: TikaDocumentReader
- Split de documentos: TokenTextSplitter

Arquivos centrais:
- src/main/java/br/com/hvogel/hr_assistant/config/ChatClientConfig.java
- src/main/java/br/com/hvogel/hr_assistant/chat/ChatController.java
- src/main/java/br/com/hvogel/hr_assistant/chat/ChatService.java
- src/main/java/br/com/hvogel/hr_assistant/ingestion/IngestionController.java
- src/main/java/br/com/hvogel/hr_assistant/ingestion/IngestionService.java
- src/main/resources/prompts/context-prompt.st
- src/main/resources/application.yml
- src/main/resources/application-anthropic.yml
- docker-compose.yml

## 3. Arquitetura em camadas

### 3.1 Camada de entrada (API)

- POST /ingest
  - Recebe um arquivo PDF
  - Extrai texto, quebra em chunks e grava no vector store

- POST /chat/stream
  - Recebe ChatRequest com a pergunta
  - Exige X-Conversation-Id no header
  - Retorna resposta em stream (SSE)

### 3.2 Camada de aplicacao (orquestracao)

- ChatService
  - Construi a chamada ao ChatClient
  - Envia mensagem do usuario
  - Injeta conversationId no contexto de memoria
  - Entrega fluxo reativo de texto

- IngestionService
  - Faz parsing do PDF
  - Aplica splitter por tokens
  - Persiste chunks no VectorStore

### 3.3 Camada de IA (advisors + prompt)

Definida em ChatClientConfig:

1. MessageChatMemoryAdvisor
- Injeta historico da conversa no prompt
- Usa ChatMemory com janela configuravel

2. QuestionAnswerAdvisor
- Faz retrieval no vector store
- Monta contexto RAG com base em top-k e similarity-threshold
- Usa template de prompt dedicado

3. PromptLoggingAdvisor
- Loga prompt final (ida) e resposta final (volta)
- Ajuda a debugar o que realmente foi enviado ao modelo

### 3.4 Camada de dados

- PostgreSQL + pgvector
  - Guarda embeddings e metadados dos chunks
  - Busca vetorial por similaridade

- Redis
  - Guarda memoria de conversa por conversationId
  - TTL controla expiracao por inatividade

## 4. Conceitos tecnicos usados

### 4.1 RAG (Retrieval-Augmented Generation)

RAG significa recuperar trechos relevantes de uma base de conhecimento antes da geracao da resposta.

No hr-assistant:
1. O manual de RH e indexado em chunks
2. A pergunta do usuario vira embedding
3. O sistema busca chunks mais similares no pgvector
4. Esses chunks entram no prompt como contexto
5. O modelo responde com base nesse contexto

### 4.2 Chunking

Chunking e a divisao de um documento grande em partes menores.

Motivos:
- Melhorar precisao da busca semantica
- Evitar mandar o documento inteiro ao modelo
- Reduzir custo de tokens

### 4.3 Similaridade

A similaridade mede o quao perto semanticamente dois vetores estao.

- similarity-threshold define o piso minimo para aceitar um chunk
- Valores maiores: menos ruido, mas risco de perder contexto
- Valores menores: mais cobertura, mas pode entrar contexto fraco

#### Glossario rapido (RAG)

- contexto: conjunto final de chunks enviados ao modelo para fundamentar a resposta.
- score de similaridade: "nota" de proximidade semantica entre pergunta e chunk.
- limiar: valor de corte minimo para aprovar ou reprovar um chunk.
- threshold: nome generico em ingles para limiar.
- similarity-threshold: nome especifico do threshold de similaridade no application.yml.
- top-k: quantidade maxima de chunks aprovados que entram no contexto.

Resumo pratico do pipeline:
1. Recupera candidatos no vector store
2. Filtra por similarity-threshold
3. Ordena por score
4. Seleciona ate top-k
5. Injeta no prompt como contexto

Exemplo simples:
- Candidatos com score: 0.82, 0.77, 0.61, 0.43, 0.39, 0.28
- similarity-threshold = 0.40 -> aprovados: 0.82, 0.77, 0.61, 0.43
- top-k = 3 -> entram no contexto: 0.82, 0.77, 0.61

Leitura do exemplo:
- limiar (threshold) define qualidade minima
- top-k define quantidade maxima
- contexto final depende dos dois parametros juntos

### 4.4 Memoria conversacional

A memoria guarda historico de interacoes por conversationId para manter continuidade.

No projeto:
- Tipo: MessageWindowChatMemory
- Repositorio: RedisChatMemoryRepository
- Controle de tamanho: app.memory.max-messages
- Controle de expiracao: spring.ai.chat.memory.redis.time-to-live

## 5. Explicacao do bloco app no application.yml

Trecho:

app:
  rag:
    top-k: 6
    similarity-threshold: 0.38
  memory:
    max-messages: 20

### 5.1 app.rag.top-k

Definicao:
- Quantidade maxima de chunks recuperados para compor o contexto RAG.

Uso no codigo:
- Injetado em ChatClientConfig por @Value("${app.rag.top-k}")
- Aplicado em SearchRequest.builder().topK(topK)

Efeito pratico:
- top-k baixo (ex.: 3): mais foco, menor custo, risco de faltar contexto
- top-k alto (ex.: 10): maior cobertura, mais tokens, risco de ruido

Valor atual (5):
- Um ponto de equilibrio comum para FAQs e politicas internas.

Quando subir top-k:
- quando a resposta vem incompleta
- quando a pergunta exige consolidar varias regras/trechos
- quando a base tem informacao muito distribuida

Quando reduzir top-k:
- quando a resposta vem com ruido ou desviando do foco
- quando ha aumento de custo/token sem ganho de qualidade
- quando o modelo mistura secoes que nao deveriam ser combinadas

### 5.2 app.rag.similarity-threshold

Definicao:
- Limiar minimo de similaridade para incluir chunk no contexto.

O que e limiar, em termos praticos:
- limiar e uma "linha de corte".
- score >= limiar: chunk aprovado.
- score < limiar: chunk descartado.
- no hr-assistant, esse limiar e o similarity-threshold.

Exemplo rapido:
- limiar = 0.40
- scores: 0.72, 0.51, 0.39
- aprovados: 0.72 e 0.51
- descartado: 0.39

Uso no codigo:
- Injetado em ChatClientConfig por @Value("${app.rag.similarity-threshold}")
- Aplicado em SearchRequest.builder().similarityThreshold(similarityThreshold)

Efeito pratico:
- threshold alto (ex.: 0.7): contexto mais estrito, possivel falta de resposta
- threshold medio (ex.: 0.4): equilibrio entre relevancia e cobertura
- threshold baixo (ex.: 0.2): mais contexto, maior chance de ruido

Valor atual (0.4):
- Ajuste moderado para recuperar contexto suficiente sem afrouxar demais.

Quando subir similarity-threshold:
- quando entram trechos pouco relacionados
- quando a resposta fica "generica" mesmo com dados corretos na base
- quando ha confusao entre politicas parecidas

Quando reduzir similarity-threshold:
- quando poucos chunks passam no filtro
- quando o assistente responde com falta de base/contexto
- quando perguntas mais abertas estao retornando fallback com frequencia

Importante:
- "threshold" e "similarity-threshold" aqui representam o mesmo conceito.
- No documento e no codigo, o nome oficial da propriedade e similarity-threshold.

### 5.3 app.memory.max-messages

Definicao:
- Tamanho da janela de memoria mantida por conversa.

Uso no codigo:
- Injetado em ChatClientConfig por @Value("${app.memory.max-messages}")
- Aplicado em MessageWindowChatMemory.builder().maxMessages(maxMessages)

Efeito pratico:
- valor baixo (ex.: 8): menor custo, menos continuidade
- valor alto (ex.: 40): maior continuidade, prompt maior

Valor atual (20):
- Bom compromisso para manter contexto de dialogo sem crescimento excessivo de prompt.

## 6. Relacao entre configuracoes de memoria

Existem dois controles diferentes e complementares:

1. max-messages (janela de mensagens)
- Limita quantas mensagens recentes sao consideradas na conversa

2. time-to-live (TTL no Redis)
- Limita por quanto tempo a memoria fica armazenada sem atividade

No projeto:
- max-messages: 20
- TTL: PT30M

Interpretacao:
- A conversa considera ate 20 mensagens recentes
- Se ficar inativa por 30 minutos, a memoria expira

## 7. Fluxo ponta a ponta

### 7.1 Fluxo de ingestao

1. Cliente envia PDF em POST /ingest
2. TikaDocumentReader extrai texto
3. TokenTextSplitter cria chunks
4. VectorStore grava embeddings/chunks no pgvector
5. API retorna quantidade de chunks armazenados

### 7.2 Fluxo de pergunta e resposta

1. Cliente envia pergunta em POST /chat/stream com X-Conversation-Id
2. ChatService aciona ChatClient
3. MessageChatMemoryAdvisor injeta historico da conversa
4. QuestionAnswerAdvisor busca chunks no pgvector com top-k e threshold
5. context-prompt.st monta a instrucao com CONTEXTO_INICIO/CONTEXTO_FIM
6. Modelo gera resposta em stream
7. PromptLoggingAdvisor registra prompt final e resposta para debug

## 8. Infraestrutura local (docker-compose)

Servicos principais:
- db: PostgreSQL com extensao pgvector
- redis: memoria de conversa e RedisInsight
- ollama: servidor de embeddings com bge-m3

Papel de cada um:
- db: busca vetorial RAG
- redis: estado de conversa
- ollama: vetoriza documentos e perguntas

## 9. Prompt e guardrails

O arquivo context-prompt.st determina que:
- A resposta deve usar exclusivamente o contexto recuperado
- Sem contexto suficiente, retorna mensagem de fallback fixa
- Para assedio/conduta/denuncia, orienta Canal de Etica
- Quando possivel, cita secao do manual

Isso reduz alucinacao e reforca conformidade com politicas internas.

## 10. Diretrizes de tuning (pratico)

Sugestoes iniciais:
- Cenário com respostas curtas e objetivas:
  - top-k: 4 a 6
  - similarity-threshold: 0.4 a 0.6
  - max-messages: 12 a 20

- Cenário com conversa longa e contexto mais amplo:
  - top-k: 6 a 8
  - similarity-threshold: 0.3 a 0.5
  - max-messages: 20 a 30

Regra geral:
- Se resposta vier incompleta: subir top-k ou reduzir threshold
- Se resposta vier com ruido: reduzir top-k ou aumentar threshold
- Se perder contexto de dialogo: aumentar max-messages

### 10.1 Playbook de ajuste (passo a passo)

1. Definir uma bateria fixa de perguntas (ex.: 10 a 20)
- incluir perguntas diretas, ambiguas e multi-topico
- incluir pelo menos 2 perguntas "fora de escopo"

2. Medir baseline
- registrar resposta correta/incorreta
- registrar ruido (trechos nao relevantes)
- registrar fallback indevido

3. Ajustar uma variavel por vez
- primeiro top-k (passos pequenos, ex.: 5 -> 6)
- depois similarity-threshold (ex.: 0.40 -> 0.45)
- evitar alterar tudo ao mesmo tempo

4. Reexecutar a mesma bateria e comparar
- manter o que melhorar precisao sem elevar muito ruido/custo
- reverter ajustes que piorarem consistencia

### 10.2 Matriz de sintomas e acao recomendada

- Sintoma: resposta curta e faltando regra importante
  - Acao 1: subir top-k
  - Acao 2: reduzir similarity-threshold levemente

- Sintoma: resposta longa com informacao lateral
  - Acao 1: reduzir top-k
  - Acao 2: subir similarity-threshold

- Sintoma: resposta erratica para perguntas parecidas
  - Acao 1: subir similarity-threshold
  - Acao 2: manter top-k moderado (4-6)

- Sintoma: muitas respostas de fallback sem necessidade
  - Acao 1: reduzir similarity-threshold
  - Acao 2: reavaliar qualidade do chunking e da ingestao

## 11. Resumo executivo

O bloco app do application.yml controla o comportamento de qualidade do RAG e continuidade da conversa:
- app.rag.top-k controla quantidade de contexto recuperado
- app.rag.similarity-threshold controla qualidade minima desse contexto
- app.memory.max-messages controla quanto historico da conversa e lembrado

Em conjunto com pgvector, Redis e o prompt de contexto restritivo, esses parametros sao o nucleo do comportamento funcional do hr-assistant.

## 12. Operacao local e troubleshooting

### 12.1 Conflito com PostgreSQL local

Durante a validacao operacional, foi identificado um conflito comum em ambiente Windows:
- PostgreSQL local da maquina ocupando a porta 5432
- PostgreSQL do Docker tentando publicar tambem em 5432

Sintoma tipico:
- `Failed to obtain JDBC Connection`
- `password authentication failed for user "postgres"`

Mesmo com credenciais corretas no container, a aplicacao pode acabar conectando no Postgres local por causa da porta conflitante.

### 12.2 Padrao adotado no projeto

Para conviver com Postgres local e Docker ao mesmo tempo, o projeto ficou com:
- Docker Postgres publicado em `5439` (host) -> `5432` (container)
- Datasource da app apontando para `jdbc:postgresql://localhost:5439/ragdb`

Arquivos de configuracao que refletem essa decisao:
- `docker-compose.yml`
- `src/main/resources/application.yml`

### 12.3 Sequencia recomendada de execucao

1. Subir infraestrutura Docker
2. Subir aplicacao Spring Boot (profile anthropic)
3. Validar health
4. Executar ingest
5. Testar chat

Observacao:
- Nao e obrigatorio rodar `mvn clean install` para refletir mudancas de `application.yml` e `docker-compose.yml`.
- Para esse fluxo, `docker compose up -d` + `spring-boot:run` sao suficientes.

### 12.4 Endpoint correto de chat (evitar 404)

Para teste via terminal, o endpoint correto e:
- `POST /chat/stream`

Contrato correto da chamada:
- Header obrigatorio: `X-Conversation-Id`
- Body JSON: `{ "message": "..." }`

Erros comuns:
- Chamar `POST /chat` -> retorna `404 Not Found`
- Enviar `question` no body -> contrato divergente de `ChatRequest`
- Enviar `conversationId` no body -> o controller espera no header

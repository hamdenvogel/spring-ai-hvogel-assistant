# Melhorias da interface web — Assistente de RH Hvogel

Documento de referência das melhorias visuais e de UX implementadas na tela do chat (`http://localhost:8080/`).

Arquivo principal: `src/main/resources/static/index.html`  
Logo: `src/main/resources/static/logo_hvogel_brand.png`

---

## 1. Objetivo

Transformar a tela de perguntas (antes simples e funcional) em uma experiência **profissional, alinhada à marca Hvogel**, com feedback do usuário, exportação de conversa e estados claros durante a resposta da IA.

---

## 2. Visual e identidade

| Item | Descrição |
|---|---|
| Layout | Fundo com gradiente suave, cards com sombra e bordas arredondadas |
| Cores | `#004682` (brand-dark), `#0077b6` (brand-mid), `#48a9e6` (brand-light) |
| Logo | `logo_hvogel_brand.png` no cabeçalho (mesma identidade do PDF de políticas) |
| Badge | “Assistente de RH” |
| Ações no header | Exportar TXT, exportar PDF, Nova conversa |
| Footer | “Desenvolvido por **Hvogel** Tecnologia Ltda. · **v1.0.0.0**” |

### Versão no footer

A versão é carregada de `GET /api/info`, configurada em `application.yml`:

```yaml
app:
  info:
    version: 1.0.0.0
    developer: Hvogel Tecnologia Ltda.
```

Se a API não responder, o footer mantém o fallback estático `v1.0.0.0`.

---

## 3. Experiência do chat

### 3.1 Bolhas de mensagem

- **Usuário:** à direita, gradiente azul Hvogel, texto branco
- **Assistente:** à esquerda, fundo claro, borda sutil, rótulo “Assistente Hvogel”

### 3.2 Tela de boas-vindas

Quando não há conversa ativa, a tela mostra:

- Saudação: “Olá! Como posso ajudar?”
- Texto explicando o escopo (férias, benefícios, licenças, etc.)
- Sugestões rápidas clicáveis:
  - Férias
  - Benefícios
  - Licença-maternidade
  - Home office

### 3.3 Indicador de digitação

Três pontinhos animados (estilo WhatsApp/Telegram) enquanto a resposta ainda não chegou ou está em andamento.

---

## 4. Estados durante a resposta (RAG / geração)

A UI diferencia claramente as fases:

| Fase | Indicador visual | Quando aparece |
|---|---|---|
| Consultando o manual | Pill azul + spinner: “Consultando o manual de RH…” | Antes do primeiro token do stream |
| Redigindo resposta | Pill + ícone de caneta: “Redigindo resposta…” | Quando o texto começa a chegar (SSE) |
| Concluído | Status some; ações da mensagem aparecem | Fim do stream |

### 4.1 Quando o RAG falha (fallback)

Se a resposta contém a frase oficial de fallback do prompt:

> “Não encontrei essa informação nas políticas oficiais da Hvogel…”

A UI:

- Destaca a bolha em **amarelo suave**
- Exibe um **banner**: “Informação não encontrada no manual. Entre em contato com o RH.”

Isso diferencia visualmente “digitando / buscando” de “não há informação no manual”.

---

## 5. Ações por resposta

Após cada resposta concluída, aparecem botões abaixo da bolha:

| Ação | Comportamento |
|---|---|
| **Copiar** | Copia o texto puro da resposta para a área de transferência |
| **👍 (up)** | Envia feedback positivo |
| **👎 (down)** | Envia feedback negativo |

Toasts discretos confirmam as ações (ex.: “Resposta copiada!”, “Obrigado pelo feedback!”).

---

## 6. Feedback — `POST /chat/feedback`

### 6.1 Endpoint

```
POST /chat/feedback
Content-Type: application/json
```

### 6.2 Body enviado pela UI

```json
{
  "messageId": "uuid-da-resposta",
  "conversationId": "uuid-da-conversa",
  "rating": "up",
  "question": "Qual o home office?",
  "answer": "texto completo da resposta do assistente"
}
```

| Campo | Tipo | Descrição |
|---|---|---|
| `messageId` | string (UUID) | Identificador da bolha da resposta |
| `conversationId` | string (UUID) | ID da conversa (mesmo do `localStorage` / header `X-Conversation-Id`) |
| `rating` | `"up"` ou `"down"` | Avaliação do usuário (👍 / 👎) |
| `question` | string | Última pergunta do usuário |
| `answer` | string | Texto completo da resposta avaliada |

### 6.3 Resposta do backend

```json
{ "status": "recorded" }
```

### 6.4 Classes envolvidas

| Classe | Papel |
|---|---|
| `FeedbackRequest` | DTO (record) com os 5 campos acima |
| `FeedbackController` | Expõe `POST /chat/feedback` |
| `FeedbackService` | Método `register(...)` — registra o feedback no log |

### 6.5 Persistência atual

Hoje o feedback **não é gravado em banco**. O `FeedbackService` apenas escreve no log da aplicação, por exemplo:

```text
[feedback] conversationId=abc-123 messageId=msg-1 rating=up question="Qual o home office?"
```

Isso já permite analisar avaliações nos logs e, no futuro, evoluir para tabela/banco (conforme o roadmap).

### 6.6 Exemplo via curl (CMD)

```cmd
curl -X POST -H "Content-Type: application/json" -d "{\"messageId\":\"msg-1\",\"conversationId\":\"teste-1\",\"rating\":\"up\",\"question\":\"Como funciona o home office?\",\"answer\":\"Conforme o manual...\"}" http://localhost:8080/chat/feedback
```

---

## 7. Exportar conversa

No cabeçalho:

| Botão | Comportamento |
|---|---|
| **TXT** | Baixa o histórico completo em arquivo `.txt` |
| **PDF** | Abre janela formatada (cores Hvogel) e usa a impressão do navegador → “Salvar como PDF” |

Os botões ficam desabilitados enquanto não houver mensagens na conversa.

Conteúdo exportado inclui:

- Título: Assistente de RH — Hvogel
- Data/hora da exportação
- Alternância Você / Assistente Hvogel com o texto de cada mensagem
- No PDF: também o `conversationId`

---

## 8. Outros detalhes de UX

- **Nova conversa:** gera novo `conversationId`, limpa o chat e restaura a tela de boas-vindas
- **Streaming SSE:** a resposta continua sendo montada token a token via `POST /chat/stream`
- **Markdown:** respostas do assistente ainda são renderizadas com suporte básico a listas, negrito, código, etc.
- **Responsivo:** em telas menores, a logo reduz e o badge pode ocultar; labels TXT/PDF podem sumir (ficam só os ícones)

---

## 9. Arquivos relacionados

| Arquivo | Conteúdo |
|---|---|
| `src/main/resources/static/index.html` | UI completa (CSS + HTML + JS) |
| `src/main/resources/static/logo_hvogel_brand.png` | Logo da marca |
| `src/main/resources/application.yml` | `app.info.version` / `app.info.developer` |
| `config/AppInfoController.java` | `GET /api/info` |
| `chat/FeedbackController.java` | `POST /chat/feedback` |
| `chat/FeedbackService.java` | Registro do feedback |
| `chat/dto/FeedbackRequest.java` | Contrato JSON do feedback |
| `roadmap-melhorias-hr-assistant.md` | Roadmap geral (inclui UX como item de evolução) |

---

## 10. Como visualizar

1. Subir a aplicação (`spring-boot:run` com profile `anthropic`)
2. Abrir http://localhost:8080/
3. Se necessário, forçar recarregamento sem cache: **Ctrl+F5**

---

## 11. Resumo

A tela passou a oferecer:

1. Identidade visual Hvogel (logo, cores, footer com versão)
2. Chat em bolhas com boas-vindas e sugestões
3. Estados claros: consultando manual → redigindo → concluído / fallback
4. Feedback 👍/👎 via `POST /chat/feedback`
5. Copiar resposta e exportar conversa (TXT/PDF)
6. Toasts e animações discretas para uma experiência mais profissional

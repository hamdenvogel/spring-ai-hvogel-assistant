# Comandos CMD/Cmder - hr-assistant

## 1) Subir app a partir da pasta projects (recomendado)

Use este comando em uma linha:

```cmd
hr-assistant\mvnw.cmd -f hr-assistant\pom.xml spring-boot:run -Dspring-boot.run.profiles=anthropic
```

## 2) Subir app entrando primeiro na pasta hr-assistant

```cmd
mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=anthropic
```

## 3) Validar se a variavel de ambiente da Anthropic esta disponivel

```cmd
echo %ANTHROPIC_API_KEY%
```

Se vier vazio, abra um terminal novo e teste novamente.

## 4) Validar se a app abriu a porta 8080

```cmd
netstat -ano | findstr :8080
```

## 5) Validar health endpoint

```cmd
curl http://localhost:8080/actuator/health
```

## 6) Fazer ingest do PDF (a partir de projects)

```cmd
curl -X POST -F "file=@hr-assistant/src/main/resources/docs/hvogel_politicas_rh.pdf" http://localhost:8080/ingest
```

## 7) Fazer ingest do PDF (a partir de hr-assistant)

```cmd
curl -X POST -F "file=@src/main/resources/docs/hvogel_politicas_rh.pdf" http://localhost:8080/ingest
```

## 8) Comandos Docker uteis

Subir infraestrutura:

```cmd
docker compose up -d
```

Ver status dos containers:

```cmd
docker compose ps
```

Recriar os containers (quando alterar portas/config):

```cmd
docker compose down && docker compose up -d
```

Validar que o Postgres do Docker esta publicado em 5439:

```cmd
docker compose ps
```

Esperado no campo PORTS do db: `0.0.0.0:5439->5432/tcp`

## 9) Sequencia rapida (copiar e colar)

Terminal 1:

```cmd
cd /d %CD%
docker compose up -d
mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=anthropic
```

Terminal 2:

```cmd
cd /d %CD%
curl -X POST -F "file=@src/main/resources/docs/hvogel_politicas_rh.pdf" http://localhost:8080/ingest
```

Resultado esperado do ingest:

```json
{"chunksStored":16}
```

O numero pode variar conforme o PDF.

## 10) Fluxo unico (fim a fim, em uma linha cada)

1) Subir Docker:

```cmd
docker compose down && docker compose up -d
```

2) Subir app:

```cmd
mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=anthropic
```

3) Health check:

```cmd
curl http://localhost:8080/actuator/health
```

4) Ingest do PDF:

```cmd
curl -X POST -F "file=@src/main/resources/docs/hvogel_politicas_rh.pdf" http://localhost:8080/ingest
```

5) Teste de chat (férias):

```cmd
curl -N -X POST -H "Content-Type: application/json" -H "X-Conversation-Id: teste-1" -d "{\"message\":\"Quais sao as principais politicas de ferias?\"}" http://localhost:8080/chat/stream
```

Observacao:
- O endpoint correto de chat e `/chat/stream` (SSE).
- O body usa `message` (nao `question`).
- O `conversationId` vai no header `X-Conversation-Id` (nao no body).
- Troque o texto dentro de `message` para fazer outras perguntas (veja secao 13).

## 13) Perguntas no chat pela linha de comando (CMD)

Subir a aplicacao (com Maven instalado no PATH):

```cmd
mvn spring-boot:run -Dspring-boot.run.profiles=anthropic
```

Alternativa com wrapper do projeto (nao precisa de Maven global):

```cmd
mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=anthropic
```

Perguntas de exemplo (mesmo `X-Conversation-Id` mantem o historico da conversa):

Processo de admissao:

```cmd
curl -N -X POST -H "Content-Type: application/json" -H "X-Conversation-Id: teste-1" -d "{\"message\":\"Como e o processo de admissao?\"}" http://localhost:8080/chat/stream
```

Licenca-paternidade:

```cmd
curl -N -X POST -H "Content-Type: application/json" -H "X-Conversation-Id: teste-1" -d "{\"message\":\"Qual e a licenca-paternidade?\"}" http://localhost:8080/chat/stream
```

Licenca-maternidade:

```cmd
curl -N -X POST -H "Content-Type: application/json" -H "X-Conversation-Id: teste-1" -d "{\"message\":\"Qual e a licenca-maternidade?\"}" http://localhost:8080/chat/stream
```

Politicas de ferias:

```cmd
curl -N -X POST -H "Content-Type: application/json" -H "X-Conversation-Id: teste-1" -d "{\"message\":\"Quais sao as principais politicas de ferias?\"}" http://localhost:8080/chat/stream
```

Beneficios:

```cmd
curl -N -X POST -H "Content-Type: application/json" -H "X-Conversation-Id: teste-1" -d "{\"message\":\"Quais sao os beneficios oferecidos?\"}" http://localhost:8080/chat/stream
```

Home office:

```cmd
curl -N -X POST -H "Content-Type: application/json" -H "X-Conversation-Id: teste-1" -d "{\"message\":\"Como funciona o home office?\"}" http://localhost:8080/chat/stream
```

Interface web (mesmas perguntas no navegador): http://localhost:8080/

## 11) Nota rapida sobre build

Para refletir mudancas em `docker-compose.yml` e `application.yml`, nao precisa rodar `mvn clean install` antes.

Use normalmente:
- `docker compose up -d`
- `mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=anthropic`

## 12) Limpeza de espaco no Docker (containers, imagens e volumes)

1) Remover stack do projeto (containers + rede + volumes do compose):

```cmd
docker compose down -v
```

2) Remover tambem as imagens do compose:

```cmd
docker compose down --rmi all -v
```

3) Limpeza geral do Docker (mais agressiva):

```cmd
docker system prune -a --volumes
```

4) Ver quanto ainda esta ocupando:

```cmd
docker system df
```

Observacoes:
- Com `-v`, voce apaga dados locais dos volumes (Postgres/Redis/Ollama).
- Depois, basta rodar de novo `docker compose up -d` que o Docker recria e baixa tudo automaticamente.
- As configuracoes do projeto (ex.: porta 5439 do Postgres) continuam, porque estao salvas nos arquivos.

## 13) SonarQube (analise de qualidade e cobertura)

Pre-requisito: SonarQube rodando em http://localhost:9000 (`docker start sonarqube`).

Criar projeto no Sonar com key **hr-assistant** (http://localhost:9000/projects/create).

Rodar analise (defina seu token antes):

```cmd
set SONAR_TOKEN=SEU_TOKEN_GERADO && mvnw.cmd clean verify sonar:sonar
```

Ver relatorio:

http://localhost:9000/dashboard?id=hr-assistant

Detalhes: `sonarqube.md`

# SonarQube — hr-assistant

Guia para analisar o projeto no SonarQube local (mesma configuracao do `projeto-java-financial-api`).

## 1. Subir o SonarQube (Docker)

Se ainda nao tiver o container:

```cmd
docker run -d -p 9000:9000 --name=sonarqube -v sonarqube_data:/opt/sonarqube/data -v sonarqube_extensions:/opt/sonarqube/extensions -v sonarqube_logs:/opt/sonarqube/logs sonarqube:community
```

Acesse: http://localhost:9000

## 2. Criar o projeto no SonarQube

1. http://localhost:9000/projects/create
2. Project key: **hr-assistant** (deve coincidir com `sonar.projectKey` no `pom.xml`)
3. Display name: **Assistente de RH - Hvogel**
4. Gerar token em **My Account → Security** (nao commitar o token)

## 3. Rodar a analise

Na pasta `projects/hr-assistant`:

**PowerShell:**

```powershell
cd <pasta-do-projeto>
$env:SONAR_TOKEN = "SEU_TOKEN_GERADO"
.\mvnw.cmd clean verify sonar:sonar
```

**CMD:**

```cmd
set SONAR_TOKEN=SEU_TOKEN_GERADO && mvnw.cmd clean verify sonar:sonar
```

Alternativa via parametro (sem variavel de ambiente):

```powershell
.\mvnw.cmd clean verify sonar:sonar "-Dsonar.token=SEU_TOKEN_GERADO"
```

## 4. Ver o relatorio

Dashboard do projeto:

http://localhost:9000/dashboard?id=hr-assistant

## 5. O que o pom.xml envia ao Sonar

| Propriedade | Valor |
|---|---|
| `sonar.projectKey` | `hr-assistant` |
| `sonar.projectName` | `Assistente de RH - Hvogel` |
| `sonar.host.url` | `http://localhost:9000` |
| Cobertura JaCoCo | `target/site/jacoco/jacoco.xml` |

O token **nao** fica no `pom.xml` — use `SONAR_TOKEN` ou `-Dsonar.token`.

## 6. Comandos uteis do Docker

| Acao | Comando |
|---|---|
| Parar | `docker stop sonarqube` |
| Iniciar | `docker start sonarqube` |
| Logs | `docker logs -f sonarqube` |
| Status | `docker ps --filter name=sonarqube` |

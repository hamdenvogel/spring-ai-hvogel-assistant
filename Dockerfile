# Stage 1: compila o JAR
FROM amazoncorretto:26-alpine AS build
WORKDIR /src
RUN apk add --no-cache maven
COPY pom.xml .
COPY src ./src
RUN mvn -q -DskipTests package \
	&& mv target/hv-assistant-*.jar /src/app.jar

# Stage 2: imagem final
FROM amazoncorretto:26-alpine
WORKDIR /app

RUN addgroup -g 1001 -S appgroup && \
	adduser -u 1001 -S appuser -G appgroup

COPY --from=build /src/app.jar app.jar
RUN chown appuser:appgroup app.jar

USER appuser

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]

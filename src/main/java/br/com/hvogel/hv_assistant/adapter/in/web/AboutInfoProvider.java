package br.com.hvogel.hv_assistant.adapter.in.web;

import br.com.hvogel.hv_assistant.adapter.in.web.dto.AboutInfo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringBootVersion;
import org.springframework.boot.info.BuildProperties;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Provider de apresentação: monta os metadados do modal "Sobre".
 * <p>
 * Vive em {@code adapter.in.web} de propósito — não é caso de uso do núcleo
 * ({@code application}), e sim um detalhe da borda HTTP (mesmo papel do About
 * do FilePack, com nome inequívoco no hexágono).
 * <p>
 * A versão exibida vem do Maven ({@link BuildProperties}) quando disponível,
 * com o sufixo {@code -SNAPSHOT} removido só para a UI (o {@code pom.xml}
 * continua podendo ser {@code 0.0.1-SNAPSHOT}).
 */
@Component
public class AboutInfoProvider {

    private final Optional<BuildProperties> buildProperties;
    private final String autor;
    private final String aplicacao;
    private final String descricao;
    private final String fallbackVersion;

    public AboutInfoProvider(
            Optional<BuildProperties> buildProperties,
            @Value("${app.about.autor:${app.info.developer:Hvogel Tecnologia Ltda.}}") String autor,
            @Value("${app.about.aplicacao:HV Assistant}") String aplicacao,
            @Value("${app.about.descricao:Assistente virtual de RH com RAG (Spring AI + Claude)}") String descricao,
            @Value("${app.about.versao:${app.info.version:0.2.0}}") String fallbackVersion) {
        this.buildProperties = buildProperties;
        this.autor = autor;
        this.aplicacao = aplicacao;
        this.descricao = descricao;
        this.fallbackVersion = fallbackVersion;
    }

    public AboutInfo getAbout() {
        String rawVersion = buildProperties.map(BuildProperties::getVersion).orElse(fallbackVersion);
        String version = toDisplayVersion(rawVersion);
        Instant buildTime = buildProperties.map(BuildProperties::getTime)
                .orElse(Instant.parse("2026-07-25T00:00:00Z"));

        return new AboutInfo(
                autor,
                version,
                aplicacao,
                descricao,
                buildTime,
                Instant.now(),
                System.getProperty("java.version"),
                SpringBootVersion.getVersion(),
                List.of("RAG", "SSE", "PDF"),
                List.of(
                        "POST /chat/stream",
                        "POST /chat/feedback",
                        "POST /ingest",
                        "GET /api/sobre",
                        "GET /api/info",
                        "GET /actuator/health"));
    }

    /**
     * Converte a versão do Maven em texto amigável para a UI.
     * Ex.: {@code 0.0.1-SNAPSHOT} → {@code 0.0.1}
     */
    static String toDisplayVersion(String raw) {
        if (raw == null || raw.isBlank()) {
            return "0.2.0";
        }
        String trimmed = raw.trim();
        final String snapshot = "-SNAPSHOT";
        if (trimmed.length() > snapshot.length()
                && trimmed.regionMatches(true, trimmed.length() - snapshot.length(), snapshot, 0, snapshot.length())) {
            return trimmed.substring(0, trimmed.length() - snapshot.length());
        }
        return trimmed;
    }
}

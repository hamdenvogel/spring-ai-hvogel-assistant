package br.com.hvogel.hr_assistant.support;

public final class TestApplicationProperties {

    public static final String[] BASE = {
            "spring.main.lazy-initialization=true",
            "spring.ai.model.chat=anthropic",
            "spring.ai.model.embedding=ollama",
            "spring.ai.ollama.chat.enabled=false",
            "spring.ai.anthropic.api-key=test-key",
            "spring.ai.chat.memory.redis.initialize-schema=false",
            "app.ingest.auto-on-startup=false",
            "app.info.version=1.0.0.0",
            "app.info.developer=Hvogel Tecnologia Ltda."
    };

    private TestApplicationProperties() {
    }
}

package br.com.hvogel.hv_assistant.support;

public final class TestApplicationProperties {

    public static final String[] BASE = {
            "spring.main.lazy-initialization=true",
            "spring.ai.model.chat=anthropic",
            "spring.ai.model.embedding=openai",
            "spring.ai.openai.chat.enabled=false",
            "spring.ai.anthropic.api-key=test-key",
            "spring.ai.openai.api-key=test-key",
            "spring.ai.chat.memory.redis.initialize-schema=false",
            "app.ingest.auto-on-startup=false",
            "app.info.version=0.2.0",
            "app.info.developer=Hvogel Tecnologia Ltda."
    };

    private TestApplicationProperties() {
    }
}

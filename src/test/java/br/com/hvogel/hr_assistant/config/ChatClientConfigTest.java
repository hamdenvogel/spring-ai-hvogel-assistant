package br.com.hvogel.hr_assistant.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.repository.redis.RedisChatMemoryRepository;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(properties = {
        "spring.main.lazy-initialization=true",
        "spring.ai.model.chat=anthropic",
        "spring.ai.model.embedding=ollama",
        "spring.ai.ollama.chat.enabled=false",
        "spring.ai.anthropic.api-key=test-key",
        "spring.ai.chat.memory.redis.initialize-schema=false",
        "spring.datasource.password=test",
        "app.ingest.auto-on-startup=false",
        "app.info.version=1.0.0.0",
        "app.info.developer=Hvogel Tecnologia Ltda."
})
class ChatClientConfigTest {

    @Autowired
    private ChatClient chatClient;

    @Autowired
    private ChatMemory chatMemory;

    @Autowired
    private RedisChatMemoryRepository redisChatMemoryRepository;

    @MockitoBean
    private VectorStore vectorStore;

    @Test
    void shouldCreateChatClientBean() {
        assertThat(chatClient).isNotNull();
    }

    @Test
    void shouldCreateChatMemoryBean() {
        assertThat(chatMemory).isNotNull();
    }

    @Test
    void shouldCreateRedisChatMemoryRepositoryBean() {
        assertThat(redisChatMemoryRepository).isNotNull();
    }
}

package br.com.hvogel.hr_assistant;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

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
class HrAssistantApplicationTests {

	@Test
	void contextLoads() {
	}

}

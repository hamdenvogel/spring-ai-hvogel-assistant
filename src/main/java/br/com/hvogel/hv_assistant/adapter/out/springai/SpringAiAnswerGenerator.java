package br.com.hvogel.hv_assistant.adapter.out.springai;

import br.com.hvogel.hv_assistant.domain.model.ChatQuestion;
import br.com.hvogel.hv_assistant.domain.port.out.AnswerGeneratorPort;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

/**
 * Adapter: Spring AI ChatClient (com advisors de memória + RAG).
 */
@Component
public class SpringAiAnswerGenerator implements AnswerGeneratorPort {

    private final ChatClient chatClient;

    public SpringAiAnswerGenerator(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @Override
    public Flux<String> stream(ChatQuestion question) {
        return chatClient.prompt()
                .user(question.message())
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, question.conversationId()))
                .stream()
                .content();
    }
}

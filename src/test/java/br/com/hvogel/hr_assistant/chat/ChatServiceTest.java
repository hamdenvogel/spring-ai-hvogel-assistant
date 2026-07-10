package br.com.hvogel.hr_assistant.chat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;

import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock
    private ChatClient chatClient;

    @Mock
    private ChatClient.ChatClientRequestSpec requestSpec;

    @Mock
    private ChatClient.StreamResponseSpec streamResponseSpec;

    private ChatService chatService;

    @BeforeEach
    void setUp() {
        chatService = new ChatService(chatClient);
    }

    @Test
    void stream_shouldReturnContentFromChatClient() {
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.advisors(any(Consumer.class))).thenReturn(requestSpec);
        when(requestSpec.stream()).thenReturn(streamResponseSpec);
        when(streamResponseSpec.content()).thenReturn(Flux.just("Resposta ", "sobre ferias."));

        StepVerifier.create(chatService.stream("Quantos dias de ferias?", "conv-123"))
                .expectNext("Resposta ", "sobre ferias.")
                .verifyComplete();

        verify(requestSpec).user("Quantos dias de ferias?");
        verify(requestSpec).advisors(any(Consumer.class));
    }

    @Test
    void stream_shouldPropagateError() {
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.advisors(any(Consumer.class))).thenReturn(requestSpec);
        when(requestSpec.stream()).thenReturn(streamResponseSpec);
        when(streamResponseSpec.content()).thenReturn(Flux.error(new RuntimeException("falha no modelo")));

        StepVerifier.create(chatService.stream("teste", "conv-err"))
                .expectError(RuntimeException.class)
                .verify();
    }
}

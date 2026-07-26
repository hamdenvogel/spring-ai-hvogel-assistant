package br.com.hvogel.hv_assistant.application;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.hvogel.hv_assistant.domain.model.ChatQuestion;
import br.com.hvogel.hv_assistant.domain.port.out.AnswerGeneratorPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class AskQuestionServiceTest {

    @Mock
    private AnswerGeneratorPort answerGenerator;

    private AskQuestionService service;

    @BeforeEach
    void setUp() {
        service = new AskQuestionService(answerGenerator);
    }

    @Test
    void ask_shouldReturnContentFromAnswerGenerator() {
        ChatQuestion question = new ChatQuestion("Quantos dias de ferias?", "conv-123");
        when(answerGenerator.stream(any(ChatQuestion.class)))
                .thenReturn(Flux.just("Resposta ", "sobre ferias."));

        StepVerifier.create(service.ask(question))
                .expectNext("Resposta ", "sobre ferias.")
                .verifyComplete();

        verify(answerGenerator).stream(question);
    }

    @Test
    void ask_shouldPropagateError() {
        ChatQuestion question = new ChatQuestion("teste", "conv-err");
        when(answerGenerator.stream(any(ChatQuestion.class)))
                .thenReturn(Flux.error(new RuntimeException("falha no modelo")));

        StepVerifier.create(service.ask(question))
                .expectError(RuntimeException.class)
                .verify();
    }
}

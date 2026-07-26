package br.com.hvogel.hv_assistant.application;

import static org.mockito.Mockito.verify;

import br.com.hvogel.hv_assistant.domain.model.Feedback;
import br.com.hvogel.hv_assistant.domain.port.out.FeedbackRecorderPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RegisterFeedbackServiceTest {

    @Mock
    private FeedbackRecorderPort feedbackRecorder;

    private RegisterFeedbackService service;

    @BeforeEach
    void setUp() {
        service = new RegisterFeedbackService(feedbackRecorder);
    }

    @Test
    void register_shouldDelegateToRecorder() {
        Feedback feedback = new Feedback("msg-1", "conv-1", "up", "Pergunta", "Resposta");

        service.register(feedback);

        verify(feedbackRecorder).record(feedback);
    }
}

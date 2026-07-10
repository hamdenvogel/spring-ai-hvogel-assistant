package br.com.hvogel.hr_assistant.chat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import br.com.hvogel.hr_assistant.chat.dto.FeedbackRequest;

class FeedbackServiceTest {

    private FeedbackService feedbackService;

    @BeforeEach
    void setUp() {
        feedbackService = new FeedbackService();
    }

    @Test
    void register_shouldAcceptPositiveFeedback() {
        FeedbackRequest request = new FeedbackRequest(
                "msg-1",
                "conv-1",
                "up",
                "Qual o home office?",
                "O home office segue a politica interna.");

        assertThatCode(() -> feedbackService.register(request)).doesNotThrowAnyException();
    }

    @Test
    void register_shouldAcceptNegativeFeedback() {
        FeedbackRequest request = new FeedbackRequest(
                "msg-2", "conv-2", "down", "Pergunta", "Resposta");

        assertThatCode(() -> feedbackService.register(request)).doesNotThrowAnyException();
    }

    @Test
    void register_shouldHandleNullQuestion() {
        FeedbackRequest request = new FeedbackRequest(
                "msg-3", "conv-3", "down", null, "resposta");

        assertThatCode(() -> feedbackService.register(request)).doesNotThrowAnyException();
    }

    @Test
    void register_shouldHandleLongQuestion() {
        String longQuestion = "a".repeat(200);
        FeedbackRequest request = new FeedbackRequest(
                "msg-4", "conv-4", "up", longQuestion, "ok");

        assertThatCode(() -> feedbackService.register(request)).doesNotThrowAnyException();
        assertThat(request.question()).hasSize(200);
    }
}

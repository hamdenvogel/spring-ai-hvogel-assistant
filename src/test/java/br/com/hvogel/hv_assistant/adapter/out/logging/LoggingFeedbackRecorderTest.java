package br.com.hvogel.hv_assistant.adapter.out.logging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import br.com.hvogel.hv_assistant.domain.model.Feedback;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LoggingFeedbackRecorderTest {

    private LoggingFeedbackRecorder recorder;

    @BeforeEach
    void setUp() {
        recorder = new LoggingFeedbackRecorder();
    }

    @Test
    void record_shouldAcceptPositiveFeedback() {
        Feedback feedback = new Feedback(
                "msg-1",
                "conv-1",
                "up",
                "Qual o home office?",
                "O home office segue a politica interna.");

        assertThatCode(() -> recorder.record(feedback)).doesNotThrowAnyException();
    }

    @Test
    void record_shouldAcceptNegativeFeedback() {
        Feedback feedback = new Feedback("msg-2", "conv-2", "down", "Pergunta", "Resposta");

        assertThatCode(() -> recorder.record(feedback)).doesNotThrowAnyException();
    }

    @Test
    void record_shouldHandleNullQuestion() {
        Feedback feedback = new Feedback("msg-3", "conv-3", "down", null, "resposta");

        assertThatCode(() -> recorder.record(feedback)).doesNotThrowAnyException();
    }

    @Test
    void record_shouldHandleLongQuestion() {
        String longQuestion = "a".repeat(200);
        Feedback feedback = new Feedback("msg-4", "conv-4", "up", longQuestion, "ok");

        assertThatCode(() -> recorder.record(feedback)).doesNotThrowAnyException();
        assertThat(feedback.question()).hasSize(200);
    }
}

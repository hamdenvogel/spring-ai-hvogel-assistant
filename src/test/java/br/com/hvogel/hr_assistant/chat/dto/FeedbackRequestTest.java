package br.com.hvogel.hr_assistant.chat.dto;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class FeedbackRequestTest {

    @Test
    void shouldExposeAllFields() {
        FeedbackRequest request = new FeedbackRequest(
                "msg-1", "conv-1", "up", "Pergunta", "Resposta");

        assertThat(request.messageId()).isEqualTo("msg-1");
        assertThat(request.conversationId()).isEqualTo("conv-1");
        assertThat(request.rating()).isEqualTo("up");
        assertThat(request.question()).isEqualTo("Pergunta");
        assertThat(request.answer()).isEqualTo("Resposta");
    }
}

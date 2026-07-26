package br.com.hvogel.hv_assistant.adapter.in.web.dto;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ChatRequestTest {

    @Test
    void shouldExposeMessageField() {
        ChatRequest request = new ChatRequest("Como funciona o home office?");

        assertThat(request.message()).isEqualTo("Como funciona o home office?");
    }
}

package br.com.hvogel.hv_assistant.adapter.in.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.hvogel.hv_assistant.domain.model.ChatQuestion;
import br.com.hvogel.hv_assistant.domain.port.in.AskQuestionUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import reactor.core.publisher.Flux;

@WebMvcTest(ChatController.class)
class ChatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AskQuestionUseCase askQuestionUseCase;

    @Test
    void stream_shouldReturnSseWithConversationHeader() throws Exception {
        when(askQuestionUseCase.ask(any(ChatQuestion.class)))
                .thenReturn(Flux.just("Voce tem ", "30 dias."));

        mockMvc.perform(post("/chat/stream")
                        .header("X-Conversation-Id", "conv-test")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"Quantos dias de ferias?\"}"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", org.hamcrest.Matchers.containsString("text/event-stream")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Voce tem ")));
    }

    @Test
    void stream_shouldRejectMessageLongerThan200Characters() throws Exception {
        String longMessage = "a".repeat(201);

        mockMvc.perform(post("/chat/stream")
                        .header("X-Conversation-Id", "conv-test")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"" + longMessage + "\"}"))
                .andExpect(status().isBadRequest());

        verify(askQuestionUseCase, never()).ask(any());
    }

    @Test
    void stream_shouldRejectBlankMessage() throws Exception {
        mockMvc.perform(post("/chat/stream")
                        .header("X-Conversation-Id", "conv-test")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"   \"}"))
                .andExpect(status().isBadRequest());

        verify(askQuestionUseCase, never()).ask(any());
    }
}

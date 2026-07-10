package br.com.hvogel.hr_assistant.chat;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
    private ChatService chatService;

    @Test
    void stream_shouldReturnSseWithConversationHeader() throws Exception {
        when(chatService.stream("Quantos dias de ferias?", "conv-test"))
                .thenReturn(Flux.just("Voce tem ", "30 dias."));

        mockMvc.perform(post("/chat/stream")
                        .header("X-Conversation-Id", "conv-test")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"Quantos dias de ferias?\"}"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", org.hamcrest.Matchers.containsString("text/event-stream")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Voce tem ")));
    }
}

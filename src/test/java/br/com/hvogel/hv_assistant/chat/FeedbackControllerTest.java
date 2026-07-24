package br.com.hvogel.hv_assistant.chat;

import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(FeedbackController.class)
class FeedbackControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FeedbackService feedbackService;

    @Test
    void feedback_shouldRecordAndReturnStatus() throws Exception {
        mockMvc.perform(post("/chat/feedback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "messageId": "msg-1",
                                  "conversationId": "conv-1",
                                  "rating": "up",
                                  "question": "Qual o home office?",
                                  "answer": "Consulte o manual."
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("recorded"));

        verify(feedbackService).register(org.mockito.ArgumentMatchers.argThat(req ->
                req.messageId().equals("msg-1")
                        && req.conversationId().equals("conv-1")
                        && req.rating().equals("up")));
    }
}

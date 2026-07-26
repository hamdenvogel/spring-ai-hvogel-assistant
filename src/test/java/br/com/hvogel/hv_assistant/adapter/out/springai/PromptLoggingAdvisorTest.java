package br.com.hvogel.hv_assistant.adapter.out.springai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

class PromptLoggingAdvisorTest {

    @Test
    void getOrder_shouldReturnConfiguredOrder() {
        assertThat(new PromptLoggingAdvisor(1000).getOrder()).isEqualTo(1000);
    }

    @Test
    void before_shouldReturnSameRequest() {
        PromptLoggingAdvisor advisor = new PromptLoggingAdvisor(1000);
        ChatClientRequest request = mock(ChatClientRequest.class);
        AdvisorChain chain = mock(AdvisorChain.class);

        assertThat(advisor.before(request, chain)).isSameAs(request);
    }

    @Test
    void after_shouldReturnSameResponse() {
        PromptLoggingAdvisor advisor = new PromptLoggingAdvisor(1000);
        ChatClientResponse response = mock(ChatClientResponse.class);
        AdvisorChain chain = mock(AdvisorChain.class);

        assertThat(advisor.after(response, chain)).isSameAs(response);
    }

    @Test
    void adviseCall_shouldDelegateToChainAndReturnResponse() {
        PromptLoggingAdvisor advisor = new PromptLoggingAdvisor(1000);
        ChatClientRequest request = chatClientRequest("pergunta de ferias");
        CallAdvisorChain chain = mock(CallAdvisorChain.class);
        ChatClientResponse response = mock(ChatClientResponse.class);

        when(chain.nextCall(same(request))).thenReturn(response);

        assertThat(advisor.adviseCall(request, chain)).isSameAs(response);
        verify(chain).nextCall(request);
    }

    @Test
    void adviseStream_shouldAggregateAndReturnFlux() {
        PromptLoggingAdvisor advisor = new PromptLoggingAdvisor(1000);
        ChatClientRequest request = chatClientRequest("pergunta de beneficios");
        StreamAdvisorChain chain = mock(StreamAdvisorChain.class);
        ChatClientResponse chunk = chatClientResponse("token");

        when(chain.nextStream(same(request))).thenReturn(Flux.just(chunk));

        StepVerifier.create(advisor.adviseStream(request, chain))
                .expectNextCount(1)
                .verifyComplete();
    }

    @Test
    void adviseCall_shouldHandleNullChatResponse() {
        PromptLoggingAdvisor advisor = new PromptLoggingAdvisor(1000);
        ChatClientRequest request = chatClientRequest("pergunta");
        CallAdvisorChain chain = mock(CallAdvisorChain.class);
        ChatClientResponse response = ChatClientResponse.builder()
                .chatResponse(null)
                .build();

        when(chain.nextCall(any())).thenReturn(response);

        assertThat(advisor.adviseCall(request, chain)).isSameAs(response);
    }

    private static ChatClientRequest chatClientRequest(String userText) {
        return ChatClientRequest.builder()
                .prompt(new Prompt(new UserMessage(userText)))
                .build();
    }

    private static ChatClientResponse chatClientResponse(String text) {
        ChatClientResponse response = mock(ChatClientResponse.class);
        ChatResponse chatResponse = mock(ChatResponse.class);
        Generation generation = mock(Generation.class);
        AssistantMessage assistantMessage = new AssistantMessage(text);

        when(response.chatResponse()).thenReturn(chatResponse);
        when(chatResponse.getResult()).thenReturn(generation);
        when(generation.getOutput()).thenReturn(assistantMessage);

        return response;
    }
}

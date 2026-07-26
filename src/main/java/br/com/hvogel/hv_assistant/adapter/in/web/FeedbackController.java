package br.com.hvogel.hv_assistant.adapter.in.web;

import br.com.hvogel.hv_assistant.adapter.in.web.dto.FeedbackRequest;
import br.com.hvogel.hv_assistant.domain.model.Feedback;
import br.com.hvogel.hv_assistant.domain.port.in.RegisterFeedbackUseCase;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class FeedbackController {

    private final RegisterFeedbackUseCase registerFeedbackUseCase;

    public FeedbackController(RegisterFeedbackUseCase registerFeedbackUseCase) {
        this.registerFeedbackUseCase = registerFeedbackUseCase;
    }

    @PostMapping("/chat/feedback")
    public Map<String, String> feedback(@RequestBody FeedbackRequest request) {
        registerFeedbackUseCase.register(new Feedback(
                request.messageId(),
                request.conversationId(),
                request.rating(),
                request.question(),
                request.answer()));
        return Map.of("status", "recorded");
    }
}

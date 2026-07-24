package br.com.hvogel.hv_assistant.chat;

import br.com.hvogel.hv_assistant.chat.dto.FeedbackRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class FeedbackController {

    private final FeedbackService feedbackService;

    public FeedbackController(FeedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }

    @PostMapping("/chat/feedback")
    public Map<String, String> feedback(@RequestBody FeedbackRequest request) {
        feedbackService.register(request);
        return Map.of("status", "recorded");
    }
}

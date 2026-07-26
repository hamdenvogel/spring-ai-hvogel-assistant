package br.com.hvogel.hv_assistant.adapter.in.web;

import br.com.hvogel.hv_assistant.adapter.in.web.dto.ChatRequest;
import br.com.hvogel.hv_assistant.domain.model.ChatQuestion;
import br.com.hvogel.hv_assistant.domain.port.in.AskQuestionUseCase;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;

@RestController
public class ChatController {

    public static final int MAX_MESSAGE_LENGTH = 200;

    private final AskQuestionUseCase askQuestionUseCase;

    public ChatController(AskQuestionUseCase askQuestionUseCase) {
        this.askQuestionUseCase = askQuestionUseCase;
    }

    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> stream(
            @RequestBody ChatRequest request,
            @RequestHeader("X-Conversation-Id") String conversationId) {
        String message = request.message() == null ? "" : request.message().trim();
        if (message.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mensagem obrigatoria");
        }
        if (message.length() > MAX_MESSAGE_LENGTH) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Mensagem excede o limite de " + MAX_MESSAGE_LENGTH + " caracteres");
        }
        return askQuestionUseCase.ask(new ChatQuestion(message, conversationId));
    }
}

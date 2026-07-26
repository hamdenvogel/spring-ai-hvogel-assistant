package br.com.hvogel.hv_assistant.domain.port.in;

import br.com.hvogel.hv_assistant.domain.model.ChatQuestion;
import reactor.core.publisher.Flux;

/**
 * Caso de uso: responder pergunta de RH em streaming.
 * <p>
 * {@link Flux} é aceito na borda de forma pragmática — o produto é SSE.
 */
public interface AskQuestionUseCase {

    Flux<String> ask(ChatQuestion question);
}

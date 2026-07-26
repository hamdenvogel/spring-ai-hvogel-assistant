package br.com.hvogel.hv_assistant.domain.port.out;

import br.com.hvogel.hv_assistant.domain.model.ChatQuestion;
import reactor.core.publisher.Flux;

/**
 * Porta de saída: gera resposta (LLM + RAG + memória) em streaming.
 */
public interface AnswerGeneratorPort {

    Flux<String> stream(ChatQuestion question);
}

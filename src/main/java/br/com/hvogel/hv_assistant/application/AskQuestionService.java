package br.com.hvogel.hv_assistant.application;

import br.com.hvogel.hv_assistant.domain.model.ChatQuestion;
import br.com.hvogel.hv_assistant.domain.port.in.AskQuestionUseCase;
import br.com.hvogel.hv_assistant.domain.port.out.AnswerGeneratorPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;

/**
 * Orquestra a pergunta de RH via porta de geração de resposta.
 */
public class AskQuestionService implements AskQuestionUseCase {

    private static final Logger logger = LoggerFactory.getLogger(AskQuestionService.class);

    private final AnswerGeneratorPort answerGenerator;

    public AskQuestionService(AnswerGeneratorPort answerGenerator) {
        this.answerGenerator = answerGenerator;
    }

    @Override
    public Flux<String> ask(ChatQuestion question) {
        logger.info("[conversationId={}] Iniciando stream de chat (mensagem com {} caracteres)",
                question.conversationId(), question.message().length());
        return answerGenerator.stream(question)
                .doOnComplete(() -> logger.info("[conversationId={}] Stream de chat concluido com sucesso",
                        question.conversationId()))
                .doOnError(error -> logger.error("[conversationId={}] Erro durante o stream de chat: {}",
                        question.conversationId(), error.getMessage(), error));
    }
}

package br.com.hvogel.hv_assistant.config;

import br.com.hvogel.hv_assistant.application.AskQuestionService;
import br.com.hvogel.hv_assistant.application.IngestDocumentService;
import br.com.hvogel.hv_assistant.application.RegisterFeedbackService;
import br.com.hvogel.hv_assistant.domain.port.in.AskQuestionUseCase;
import br.com.hvogel.hv_assistant.domain.port.in.IngestDocumentUseCase;
import br.com.hvogel.hv_assistant.domain.port.in.RegisterFeedbackUseCase;
import br.com.hvogel.hv_assistant.domain.port.out.AnswerGeneratorPort;
import br.com.hvogel.hv_assistant.domain.port.out.DocumentParserPort;
import br.com.hvogel.hv_assistant.domain.port.out.FeedbackRecorderPort;
import br.com.hvogel.hv_assistant.domain.port.out.KnowledgeBasePort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wiring dos casos de uso (application) sem anotar o núcleo com Spring.
 */
@Configuration
public class ApplicationConfig {

    @Bean
    public AskQuestionUseCase askQuestionUseCase(AnswerGeneratorPort answerGenerator) {
        return new AskQuestionService(answerGenerator);
    }

    @Bean
    public IngestDocumentUseCase ingestDocumentUseCase(
            DocumentParserPort documentParser,
            KnowledgeBasePort knowledgeBase) {
        return new IngestDocumentService(documentParser, knowledgeBase);
    }

    @Bean
    public RegisterFeedbackUseCase registerFeedbackUseCase(FeedbackRecorderPort feedbackRecorder) {
        return new RegisterFeedbackService(feedbackRecorder);
    }
}

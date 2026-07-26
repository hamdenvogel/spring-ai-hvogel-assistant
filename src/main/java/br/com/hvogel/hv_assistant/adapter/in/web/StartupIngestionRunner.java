package br.com.hvogel.hv_assistant.adapter.in.web;

import br.com.hvogel.hv_assistant.domain.port.in.IngestDocumentUseCase;
import br.com.hvogel.hv_assistant.domain.port.out.KnowledgeBasePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

/**
 * Adapter de entrada (startup): dispara ingestão automática se o knowledge base estiver vazio.
 */
@Component
@ConditionalOnProperty(name = "app.ingest.auto-on-startup", havingValue = "true")
public class StartupIngestionRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(StartupIngestionRunner.class);

    private final IngestDocumentUseCase ingestDocumentUseCase;
    private final KnowledgeBasePort knowledgeBase;
    private final Resource document;

    public StartupIngestionRunner(
            IngestDocumentUseCase ingestDocumentUseCase,
            KnowledgeBasePort knowledgeBase,
            @Value("${app.ingest.document}") Resource document) {
        this.ingestDocumentUseCase = ingestDocumentUseCase;
        this.knowledgeBase = knowledgeBase;
        this.document = document;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (knowledgeBase.hasAnyDocuments()) {
            log.info("Vector store ja contem documentos; ingestao automatica ignorada.");
            return;
        }

        SpringResourceDocumentSource source = new SpringResourceDocumentSource(document);
        if (!source.exists()) {
            log.warn("Documento de ingestao nao encontrado: {}", document);
            return;
        }

        int chunks = ingestDocumentUseCase.ingest(source);
        log.info("Ingestao automatica concluida: {} chunks indexados de {}", chunks, source.filename());
    }
}

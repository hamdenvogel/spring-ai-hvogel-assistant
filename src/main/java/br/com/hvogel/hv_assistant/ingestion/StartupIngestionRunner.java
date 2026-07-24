package br.com.hvogel.hv_assistant.ingestion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.ingest.auto-on-startup", havingValue = "true")
public class StartupIngestionRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(StartupIngestionRunner.class);

    private final IngestionService ingestionService;
    private final VectorStore vectorStore;
    private final Resource document;

    public StartupIngestionRunner(
            IngestionService ingestionService,
            VectorStore vectorStore,
            @Value("${app.ingest.document}") Resource document) {
        this.ingestionService = ingestionService;
        this.vectorStore = vectorStore;
        this.document = document;
    }

    @Override
    public void run(ApplicationArguments args) {
        var existing = vectorStore.similaritySearch(
                SearchRequest.builder().query("manual politicas rh hvogel").topK(1).build());

        if (!existing.isEmpty()) {
            log.info("Vector store ja contem documentos ({} chunk(s) encontrado(s)); ingestao automatica ignorada.",
                    existing.size());
            return;
        }

        if (!document.exists()) {
            log.warn("Documento de ingestao nao encontrado: {}", document);
            return;
        }

        int chunks = ingestionService.ingest(document);
        log.info("Ingestao automatica concluida: {} chunks indexados de {}", chunks, document.getFilename());
    }
}

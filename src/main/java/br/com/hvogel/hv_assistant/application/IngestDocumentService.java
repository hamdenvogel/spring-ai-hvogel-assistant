package br.com.hvogel.hv_assistant.application;

import br.com.hvogel.hv_assistant.domain.model.DocumentChunk;
import br.com.hvogel.hv_assistant.domain.model.DocumentSource;
import br.com.hvogel.hv_assistant.domain.port.in.IngestDocumentUseCase;
import br.com.hvogel.hv_assistant.domain.port.out.DocumentParserPort;
import br.com.hvogel.hv_assistant.domain.port.out.KnowledgeBasePort;

import java.util.List;

/**
 * Orquestra parse do documento + persistência no knowledge base.
 */
public class IngestDocumentService implements IngestDocumentUseCase {

    private final DocumentParserPort documentParser;
    private final KnowledgeBasePort knowledgeBase;

    public IngestDocumentService(DocumentParserPort documentParser, KnowledgeBasePort knowledgeBase) {
        this.documentParser = documentParser;
        this.knowledgeBase = knowledgeBase;
    }

    @Override
    public int ingest(DocumentSource source) {
        List<DocumentChunk> chunks = documentParser.parse(source);
        return knowledgeBase.store(chunks);
    }
}

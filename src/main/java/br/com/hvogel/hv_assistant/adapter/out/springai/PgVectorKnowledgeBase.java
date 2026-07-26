package br.com.hvogel.hv_assistant.adapter.out.springai;

import br.com.hvogel.hv_assistant.domain.model.DocumentChunk;
import br.com.hvogel.hv_assistant.domain.port.out.KnowledgeBasePort;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Adapter: Spring AI VectorStore (pgvector).
 */
@Component
public class PgVectorKnowledgeBase implements KnowledgeBasePort {

    private final VectorStore vectorStore;

    public PgVectorKnowledgeBase(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    @Override
    public int store(List<DocumentChunk> chunks) {
        List<Document> documents = chunks.stream()
                .map(chunk -> new Document(chunk.text()))
                .toList();
        vectorStore.add(documents);
        return documents.size();
    }

    @Override
    public boolean hasAnyDocuments() {
        var existing = vectorStore.similaritySearch(
                SearchRequest.builder().query("manual politicas rh hvogel").topK(1).build());
        return !existing.isEmpty();
    }
}

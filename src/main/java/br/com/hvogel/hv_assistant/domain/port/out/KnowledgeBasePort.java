package br.com.hvogel.hv_assistant.domain.port.out;

import br.com.hvogel.hv_assistant.domain.model.DocumentChunk;

import java.util.List;

/**
 * Porta de saída: vector store / base de conhecimento para RAG.
 */
public interface KnowledgeBasePort {

    int store(List<DocumentChunk> chunks);

    boolean hasAnyDocuments();
}

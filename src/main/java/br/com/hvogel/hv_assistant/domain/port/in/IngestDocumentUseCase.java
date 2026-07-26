package br.com.hvogel.hv_assistant.domain.port.in;

import br.com.hvogel.hv_assistant.domain.model.DocumentSource;

/**
 * Caso de uso: ingerir documento no knowledge base (RAG).
 */
public interface IngestDocumentUseCase {

    /**
     * @return quantidade de chunks indexados
     */
    int ingest(DocumentSource source);
}

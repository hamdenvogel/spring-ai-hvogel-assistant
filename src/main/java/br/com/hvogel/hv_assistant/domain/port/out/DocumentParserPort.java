package br.com.hvogel.hv_assistant.domain.port.out;

import br.com.hvogel.hv_assistant.domain.model.DocumentChunk;
import br.com.hvogel.hv_assistant.domain.model.DocumentSource;

import java.util.List;

/**
 * Porta de saída: lê documento e divide em chunks.
 */
public interface DocumentParserPort {

    List<DocumentChunk> parse(DocumentSource source);
}

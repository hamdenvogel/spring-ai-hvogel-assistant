package br.com.hvogel.hv_assistant.adapter.out.springai;

import br.com.hvogel.hv_assistant.domain.model.DocumentChunk;
import br.com.hvogel.hv_assistant.domain.model.DocumentSource;
import br.com.hvogel.hv_assistant.domain.port.out.DocumentParserPort;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;

/**
 * Adapter: Apache Tika + TokenTextSplitter (via Spring AI).
 */
@Component
public class TikaDocumentParser implements DocumentParserPort {

    @Override
    public List<DocumentChunk> parse(DocumentSource source) {
        try (InputStream in = source.openStream()) {
            InputStreamResource resource = new InputStreamResource(in) {
                @Override
                public String getFilename() {
                    return source.filename();
                }
            };
            TikaDocumentReader reader = new TikaDocumentReader(resource);
            List<Document> documents = reader.read();
            List<Document> chunks = TokenTextSplitter.builder().build().apply(documents);
            return chunks.stream()
                    .map(doc -> new DocumentChunk(doc.getText()))
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException("Falha ao ler documento: " + source.filename(), e);
        }
    }
}

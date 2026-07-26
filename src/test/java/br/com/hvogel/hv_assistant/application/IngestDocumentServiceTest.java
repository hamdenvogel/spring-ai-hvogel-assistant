package br.com.hvogel.hv_assistant.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.hvogel.hv_assistant.domain.model.DocumentChunk;
import br.com.hvogel.hv_assistant.domain.model.DocumentSource;
import br.com.hvogel.hv_assistant.domain.port.out.DocumentParserPort;
import br.com.hvogel.hv_assistant.domain.port.out.KnowledgeBasePort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

@ExtendWith(MockitoExtension.class)
class IngestDocumentServiceTest {

    @Mock
    private DocumentParserPort documentParser;

    @Mock
    private KnowledgeBasePort knowledgeBase;

    @Mock
    private DocumentSource source;

    private IngestDocumentService service;

    @BeforeEach
    void setUp() {
        service = new IngestDocumentService(documentParser, knowledgeBase);
    }

    @Test
    void ingest_shouldParseAndStoreChunks() {
        List<DocumentChunk> chunks = List.of(new DocumentChunk("chunk-1"), new DocumentChunk("chunk-2"));
        when(documentParser.parse(source)).thenReturn(chunks);
        when(knowledgeBase.store(anyList())).thenReturn(2);

        int stored = service.ingest(source);

        assertThat(stored).isEqualTo(2);
        verify(documentParser).parse(source);
        verify(knowledgeBase).store(chunks);
    }
}

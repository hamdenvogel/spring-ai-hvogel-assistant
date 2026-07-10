package br.com.hvogel.hr_assistant.ingestion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.ClassPathResource;

@ExtendWith(MockitoExtension.class)
class IngestionServiceTest {

    @Mock
    private VectorStore vectorStore;

    @Captor
    private ArgumentCaptor<java.util.List<Document>> documentsCaptor;

    private IngestionService ingestionService;

    @BeforeEach
    void setUp() {
        ingestionService = new IngestionService(vectorStore);
    }

    @Test
    void ingest_shouldReadPdfAndStoreChunks() {
        ClassPathResource pdf = new ClassPathResource("docs/hvogel_politicas_rh.pdf");

        int chunks = ingestionService.ingest(pdf);

        assertThat(chunks).isPositive();
        verify(vectorStore).add(documentsCaptor.capture());
        assertThat(documentsCaptor.getValue()).hasSize(chunks);
        assertThat(documentsCaptor.getValue().getFirst().getText()).isNotBlank();
    }
}

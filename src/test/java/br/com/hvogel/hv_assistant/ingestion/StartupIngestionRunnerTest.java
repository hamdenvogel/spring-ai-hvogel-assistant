package br.com.hvogel.hv_assistant.ingestion;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

@ExtendWith(MockitoExtension.class)
class StartupIngestionRunnerTest {

    @Mock
    private IngestionService ingestionService;

    @Mock
    private VectorStore vectorStore;

    private StartupIngestionRunner runner;

    @BeforeEach
    void setUp() {
        runner = new StartupIngestionRunner(ingestionService, vectorStore, sampleDocument());
    }

    @Test
    void run_shouldSkipIngestWhenVectorStoreAlreadyHasDocuments() {
        when(vectorStore.similaritySearch(org.mockito.ArgumentMatchers.any(SearchRequest.class)))
                .thenReturn(List.of(new Document("chunk existente")));

        runner.run(new DefaultApplicationArguments());

        verify(ingestionService, never()).ingest(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void run_shouldSkipIngestWhenDocumentDoesNotExist() {
        when(vectorStore.similaritySearch(org.mockito.ArgumentMatchers.any(SearchRequest.class)))
                .thenReturn(List.of());
        Resource missing = new ByteArrayResource(new byte[0]) {
            @Override
            public boolean exists() {
                return false;
            }

            @Override
            public String getFilename() {
                return "missing.pdf";
            }
        };
        runner = new StartupIngestionRunner(ingestionService, vectorStore, missing);

        runner.run(new DefaultApplicationArguments());

        verify(ingestionService, never()).ingest(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void run_shouldIngestWhenVectorStoreIsEmptyAndDocumentExists() {
        when(vectorStore.similaritySearch(org.mockito.ArgumentMatchers.any(SearchRequest.class)))
                .thenReturn(List.of());
        when(ingestionService.ingest(org.mockito.ArgumentMatchers.any())).thenReturn(16);

        runner.run(new DefaultApplicationArguments());

        verify(ingestionService).ingest(sampleDocument());
    }

    private static Resource sampleDocument() {
        return new ClassPathResource("docs/hvogel_politicas_rh.pdf") {
            @Override
            public boolean exists() {
                return true;
            }

            @Override
            public String getFilename() {
                return "hvogel_politicas_rh.pdf";
            }
        };
    }
}

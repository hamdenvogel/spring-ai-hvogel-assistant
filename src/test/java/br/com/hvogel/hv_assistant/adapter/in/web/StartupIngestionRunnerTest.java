package br.com.hvogel.hv_assistant.adapter.in.web;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.hvogel.hv_assistant.domain.model.DocumentSource;
import br.com.hvogel.hv_assistant.domain.port.in.IngestDocumentUseCase;
import br.com.hvogel.hv_assistant.domain.port.out.KnowledgeBasePort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

@ExtendWith(MockitoExtension.class)
class StartupIngestionRunnerTest {

    @Mock
    private IngestDocumentUseCase ingestDocumentUseCase;

    @Mock
    private KnowledgeBasePort knowledgeBase;

    private StartupIngestionRunner runner;

    @BeforeEach
    void setUp() {
        runner = new StartupIngestionRunner(ingestDocumentUseCase, knowledgeBase, sampleDocument());
    }

    @Test
    void run_shouldSkipIngestWhenKnowledgeBaseAlreadyHasDocuments() {
        when(knowledgeBase.hasAnyDocuments()).thenReturn(true);

        runner.run(new DefaultApplicationArguments());

        verify(ingestDocumentUseCase, never()).ingest(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void run_shouldSkipIngestWhenDocumentDoesNotExist() {
        when(knowledgeBase.hasAnyDocuments()).thenReturn(false);
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
        runner = new StartupIngestionRunner(ingestDocumentUseCase, knowledgeBase, missing);

        runner.run(new DefaultApplicationArguments());

        verify(ingestDocumentUseCase, never()).ingest(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void run_shouldIngestWhenKnowledgeBaseIsEmptyAndDocumentExists() {
        when(knowledgeBase.hasAnyDocuments()).thenReturn(false);
        when(ingestDocumentUseCase.ingest(org.mockito.ArgumentMatchers.any(DocumentSource.class))).thenReturn(16);

        runner.run(new DefaultApplicationArguments());

        verify(ingestDocumentUseCase).ingest(org.mockito.ArgumentMatchers.any(DocumentSource.class));
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

package br.com.hvogel.hv_assistant.adapter.out.springai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.hvogel.hv_assistant.domain.model.DocumentChunk;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

import java.util.List;

@ExtendWith(MockitoExtension.class)
class PgVectorKnowledgeBaseTest {

    @Mock
    private VectorStore vectorStore;

    @Captor
    private ArgumentCaptor<List<Document>> documentsCaptor;

    private PgVectorKnowledgeBase knowledgeBase;

    @BeforeEach
    void setUp() {
        knowledgeBase = new PgVectorKnowledgeBase(vectorStore);
    }

    @Test
    void store_shouldMapChunksToDocumentsAndAddToVectorStore() {
        int stored = knowledgeBase.store(List.of(
                new DocumentChunk("chunk A"),
                new DocumentChunk("chunk B")));

        assertThat(stored).isEqualTo(2);
        verify(vectorStore).add(documentsCaptor.capture());
        assertThat(documentsCaptor.getValue()).hasSize(2);
        assertThat(documentsCaptor.getValue().getFirst().getText()).isEqualTo("chunk A");
    }

    @Test
    void hasAnyDocuments_shouldReturnTrueWhenSearchFindsChunks() {
        when(vectorStore.similaritySearch(any(SearchRequest.class)))
                .thenReturn(List.of(new Document("existente")));

        assertThat(knowledgeBase.hasAnyDocuments()).isTrue();
    }

    @Test
    void hasAnyDocuments_shouldReturnFalseWhenSearchIsEmpty() {
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of());

        assertThat(knowledgeBase.hasAnyDocuments()).isFalse();
    }
}

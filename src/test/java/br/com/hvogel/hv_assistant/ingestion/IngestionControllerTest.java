package br.com.hvogel.hv_assistant.ingestion;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(IngestionController.class)
class IngestionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IngestionService ingestionService;

    @Test
    void ingest_shouldReturnChunksStored() throws Exception {
        when(ingestionService.ingest(any())).thenReturn(16);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "hvogel_politicas_rh.pdf",
                "application/pdf",
                "conteudo-pdf-falso".getBytes());

        mockMvc.perform(multipart("/ingest").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.chunksStored").value(16));
    }
}

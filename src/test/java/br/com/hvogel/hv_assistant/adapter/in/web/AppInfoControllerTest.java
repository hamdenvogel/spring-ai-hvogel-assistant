package br.com.hvogel.hv_assistant.adapter.in.web;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

import br.com.hvogel.hv_assistant.adapter.in.web.dto.AboutInfo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AppInfoController.class)
class AppInfoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AboutInfoProvider aboutInfoProvider;

    @Test
    void info_shouldReturnApplicationMetadata() throws Exception {
        when(aboutInfoProvider.getAbout()).thenReturn(sampleAbout("0.2.0"));

        mockMvc.perform(get("/api/info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value("0.2.0"))
                .andExpect(jsonPath("$.developer").value("Hvogel Tecnologia Ltda."))
                .andExpect(jsonPath("$.product").value("Assistente de RH"));
    }

    @Test
    void sobre_shouldReturnRichMetadata() throws Exception {
        when(aboutInfoProvider.getAbout()).thenReturn(sampleAbout("0.2.0"));

        mockMvc.perform(get("/api/sobre"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.autor").value("Hvogel Tecnologia Ltda."))
                .andExpect(jsonPath("$.versao").value("0.2.0"))
                .andExpect(jsonPath("$.aplicacao").value("HV Assistant"))
                .andExpect(jsonPath("$.descricao").value("Assistente virtual de RH com RAG"))
                .andExpect(jsonPath("$.javaVersion").value("25"))
                .andExpect(jsonPath("$.springBootVersion").value("4.1.0"))
                .andExpect(jsonPath("$.formatosSuportados[0]").value("RAG"))
                .andExpect(jsonPath("$.endpoints[0]").value("GET /api/sobre"));
    }

    private static AboutInfo sampleAbout(String versao) {
        return new AboutInfo(
                "Hvogel Tecnologia Ltda.",
                versao,
                "HV Assistant",
                "Assistente virtual de RH com RAG",
                Instant.parse("2026-07-25T00:00:00Z"),
                Instant.parse("2026-07-26T12:00:00Z"),
                "25",
                "4.1.0",
                List.of("RAG", "SSE", "PDF"),
                List.of("GET /api/sobre", "POST /chat/stream"));
    }
}

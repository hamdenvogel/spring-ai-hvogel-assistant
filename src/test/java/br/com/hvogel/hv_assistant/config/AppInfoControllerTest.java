package br.com.hvogel.hv_assistant.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AppInfoController.class)
@TestPropertySource(properties = {
        "app.info.version=1.0.0.0",
        "app.info.developer=Hvogel Tecnologia Ltda."
})
class AppInfoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void info_shouldReturnApplicationMetadata() throws Exception {
        mockMvc.perform(get("/api/info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value("1.0.0.0"))
                .andExpect(jsonPath("$.developer").value("Hvogel Tecnologia Ltda."))
                .andExpect(jsonPath("$.product").value("Assistente de RH"));
    }
}

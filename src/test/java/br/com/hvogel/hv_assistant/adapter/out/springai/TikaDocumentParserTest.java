package br.com.hvogel.hv_assistant.adapter.out.springai;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.hvogel.hv_assistant.adapter.in.web.SpringResourceDocumentSource;
import br.com.hvogel.hv_assistant.domain.model.DocumentChunk;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.util.List;

class TikaDocumentParserTest {

    @Test
    void parse_shouldReadPdfAndReturnChunks() {
        TikaDocumentParser parser = new TikaDocumentParser();
        SpringResourceDocumentSource source = new SpringResourceDocumentSource(
                new ClassPathResource("docs/hvogel_politicas_rh.pdf"));

        List<DocumentChunk> chunks = parser.parse(source);

        assertThat(chunks).isNotEmpty();
        assertThat(chunks.getFirst().text()).isNotBlank();
    }
}

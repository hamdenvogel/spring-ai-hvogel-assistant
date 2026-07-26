package br.com.hvogel.hv_assistant.adapter.in.web;

import br.com.hvogel.hv_assistant.domain.port.in.IngestDocumentUseCase;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
public class IngestionController {

    private final IngestDocumentUseCase ingestDocumentUseCase;

    public IngestionController(IngestDocumentUseCase ingestDocumentUseCase) {
        this.ingestDocumentUseCase = ingestDocumentUseCase;
    }

    @PostMapping(value = "/ingest", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, Integer> ingest(@RequestParam("file") MultipartFile file) {
        int chunks = ingestDocumentUseCase.ingest(new SpringResourceDocumentSource(file.getResource()));
        return Map.of("chunksStored", chunks);
    }
}

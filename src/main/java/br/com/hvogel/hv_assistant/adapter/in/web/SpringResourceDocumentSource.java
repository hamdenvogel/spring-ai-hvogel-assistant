package br.com.hvogel.hv_assistant.adapter.in.web;

import br.com.hvogel.hv_assistant.domain.model.DocumentSource;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.io.InputStream;

/**
 * Adapta {@link Resource} do Spring para {@link DocumentSource} do domínio.
 */
public final class SpringResourceDocumentSource implements DocumentSource {

    private final Resource resource;

    public SpringResourceDocumentSource(Resource resource) {
        this.resource = resource;
    }

    @Override
    public boolean exists() {
        return resource.exists();
    }

    @Override
    public String filename() {
        String name = resource.getFilename();
        return name != null ? name : "document";
    }

    @Override
    public InputStream openStream() throws IOException {
        return resource.getInputStream();
    }

    public Resource resource() {
        return resource;
    }
}

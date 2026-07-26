package br.com.hvogel.hv_assistant.domain.model;

import java.io.IOException;
import java.io.InputStream;

/**
 * Fonte de um documento a ser ingerido (PDF, etc.).
 * Abstração sem dependência de Spring {@code Resource}.
 */
public interface DocumentSource {

    boolean exists();

    String filename();

    InputStream openStream() throws IOException;
}

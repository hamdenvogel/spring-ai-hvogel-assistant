package br.com.hvogel.hv_assistant.domain.port.out;

import br.com.hvogel.hv_assistant.domain.model.Feedback;

/**
 * Porta de saída: persiste ou registra feedback (hoje: log).
 */
public interface FeedbackRecorderPort {

    void record(Feedback feedback);
}

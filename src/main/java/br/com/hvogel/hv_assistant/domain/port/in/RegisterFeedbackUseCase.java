package br.com.hvogel.hv_assistant.domain.port.in;

import br.com.hvogel.hv_assistant.domain.model.Feedback;

/**
 * Caso de uso: registrar feedback do usuário sobre uma resposta.
 */
public interface RegisterFeedbackUseCase {

    void register(Feedback feedback);
}

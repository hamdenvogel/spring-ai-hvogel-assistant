package br.com.hvogel.hv_assistant.domain.model;

/**
 * Avaliação do usuário sobre uma resposta do assistente.
 */
public record Feedback(
        String messageId,
        String conversationId,
        String rating,
        String question,
        String answer) {
}

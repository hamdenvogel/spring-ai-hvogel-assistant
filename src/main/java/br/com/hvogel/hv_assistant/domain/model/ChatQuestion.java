package br.com.hvogel.hv_assistant.domain.model;

/**
 * Pergunta do colaborador em uma conversa de RH.
 */
public record ChatQuestion(String message, String conversationId) {
}

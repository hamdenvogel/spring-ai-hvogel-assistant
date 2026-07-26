package br.com.hvogel.hv_assistant.adapter.in.web.dto;

public record FeedbackRequest(
        String messageId,
        String conversationId,
        String rating,
        String question,
        String answer) {
}

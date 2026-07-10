package br.com.hvogel.hr_assistant.chat.dto;

public record FeedbackRequest(
        String messageId,
        String conversationId,
        String rating,
        String question,
        String answer) {
}

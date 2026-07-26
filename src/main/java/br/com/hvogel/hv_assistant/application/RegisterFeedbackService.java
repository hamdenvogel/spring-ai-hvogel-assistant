package br.com.hvogel.hv_assistant.application;

import br.com.hvogel.hv_assistant.domain.model.Feedback;
import br.com.hvogel.hv_assistant.domain.port.in.RegisterFeedbackUseCase;
import br.com.hvogel.hv_assistant.domain.port.out.FeedbackRecorderPort;

/**
 * Orquestra o registro de feedback do usuário.
 */
public class RegisterFeedbackService implements RegisterFeedbackUseCase {

    private final FeedbackRecorderPort feedbackRecorder;

    public RegisterFeedbackService(FeedbackRecorderPort feedbackRecorder) {
        this.feedbackRecorder = feedbackRecorder;
    }

    @Override
    public void register(Feedback feedback) {
        feedbackRecorder.record(feedback);
    }
}

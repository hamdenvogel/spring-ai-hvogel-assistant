package br.com.hvogel.hv_assistant.adapter.out.logging;

import br.com.hvogel.hv_assistant.domain.model.Feedback;
import br.com.hvogel.hv_assistant.domain.port.out.FeedbackRecorderPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Adapter: registra feedback em log (sem persistência ainda).
 */
@Component
public class LoggingFeedbackRecorder implements FeedbackRecorderPort {

    private static final Logger log = LoggerFactory.getLogger(LoggingFeedbackRecorder.class);

    @Override
    public void record(Feedback feedback) {
        if (log.isInfoEnabled()) {
            log.info("[feedback] conversationId={} messageId={} rating={} question=\"{}\"",
                    feedback.conversationId(),
                    feedback.messageId(),
                    feedback.rating(),
                    truncate(feedback.question(), 120));
        }
    }

    private static String truncate(String text, int max) {
        if (text == null) {
            return "";
        }
        return text.length() <= max ? text : text.substring(0, max) + "...";
    }
}

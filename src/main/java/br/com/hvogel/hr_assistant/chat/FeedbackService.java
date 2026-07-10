package br.com.hvogel.hr_assistant.chat;

import br.com.hvogel.hr_assistant.chat.dto.FeedbackRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class FeedbackService {

    private static final Logger log = LoggerFactory.getLogger(FeedbackService.class);

    public void register(FeedbackRequest feedback) {
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

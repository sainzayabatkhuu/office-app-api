package com.sol.office.report.messaging;

import com.sol.office.common.event.MessagingConstants;
import com.sol.office.common.event.NotificationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReportMessagePublisher {

    private final RabbitTemplate rabbitTemplate;

    /** Throws if the broker is unreachable: the caller must not report the job as queued. */
    public void requestExecution(String executionId) {
        rabbitTemplate.convertAndSend(MessagingConstants.REPORT_EXCHANGE,
                MessagingConstants.REPORT_EXECUTION_ROUTING_KEY, new ExecutionRequestedMessage(executionId));
    }

    /** Best effort: a lost notification must not fail the report. */
    public void notifyUser(NotificationEvent event) {
        try {
            rabbitTemplate.convertAndSend(MessagingConstants.NOTIFICATION_EXCHANGE,
                    MessagingConstants.NOTIFICATION_ROUTING_KEY, event);
        } catch (AmqpException e) {
            log.warn("Could not publish notification for {}: {}", event.recipient(), e.getMessage());
        }
    }
}

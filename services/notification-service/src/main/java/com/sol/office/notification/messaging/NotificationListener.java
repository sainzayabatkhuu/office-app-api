package com.sol.office.notification.messaging;

import com.sol.office.common.event.NotificationEvent;
import com.sol.office.notification.config.RabbitConfig;
import com.sol.office.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationListener {

    private final NotificationService notificationService;

    @RabbitListener(queues = RabbitConfig.USER_NOTIFICATION_QUEUE)
    public void onNotification(NotificationEvent event) {
        if (event.recipient() == null || event.recipient().isBlank()) {
            throw new AmqpRejectAndDontRequeueException("Notification without recipient from " + event.source());
        }
        notificationService.deliver(event);
    }
}

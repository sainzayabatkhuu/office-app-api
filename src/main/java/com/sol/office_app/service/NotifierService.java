package com.sol.office_app.service;

import com.sol.office_app.domain.response.NotificationMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class NotifierService {

    /** Clients subscribe to {@code /user/queue/notifications}. */
    public static final String USER_QUEUE = "/queue/notifications";

    private static final Logger log = LoggerFactory.getLogger(NotifierService.class);

    private final SimpMessagingTemplate messagingTemplate;

    public NotifierService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    /** Delivers to every open session of {@code username} only. */
    public void notifyUser(String username, NotificationMessage message) {
        try {
            messagingTemplate.convertAndSendToUser(username, USER_QUEUE, message);
        } catch (MessagingException e) {
            log.warn("Notification to {} failed: {}", username, e.getMessage());
        }
    }
}

package com.sol.office_app.service;

import com.sol.office_app.dto.NotificationMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class NotifierService {
    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    public void runNotifier(String msgType, String title, String msgText) {
        // Notify frontend when done
        NotificationMessage message = new NotificationMessage(msgType, "", msgText);
        messagingTemplate.convertAndSend("/topic/notification", message);
    }
}

package com.sol.office.notification.service;

import com.sol.office.common.api.PageResponse;
import com.sol.office.common.event.NotificationEvent;
import com.sol.office.common.exception.NotFoundException;
import com.sol.office.common.security.CurrentUser;
import com.sol.office.notification.config.NotificationProperties;
import com.sol.office.notification.config.WebSocketConfig;
import com.sol.office.notification.domain.Notification;
import com.sol.office.notification.dto.NotificationDtos.NotificationResponse;
import com.sol.office.notification.dto.NotificationDtos.SendRequest;
import com.sol.office.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Clock;
import java.time.Duration;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository repository;
    private final SimpMessagingTemplate messagingTemplate;
    private final NotificationProperties properties;
    private final Clock clock;

    /** Stores the notification and pushes it to the user's open sessions once committed. */
    @Transactional
    public NotificationResponse deliver(NotificationEvent event) {
        Notification n = new Notification();
        n.setRecipient(event.recipient());
        n.setLevel(event.level() == null ? "INFO" : event.level());
        n.setCategory(event.category() == null ? "GENERAL" : event.category());
        n.setTitle(truncate(event.title(), 200));
        n.setMessage(truncate(event.message(), 1000));
        n.setLink(event.link());
        n.setData(event.data() == null ? Map.of() : event.data());
        n.setSource(event.source() == null ? "unknown" : event.source());
        n.setCreatedAt(event.occurredAt() != null ? event.occurredAt() : clock.instant());
        NotificationResponse response = toResponse(repository.save(n));

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                push(event.recipient(), response);
            }
        });
        return response;
    }

    @Transactional
    public int send(CurrentUser sender, SendRequest request) {
        for (String recipient : request.recipients()) {
            deliver(NotificationEvent.of("user:" + sender.username(), recipient, request.level(), "MESSAGE",
                    request.title(), request.message(), request.link(), Map.of("from", sender.username())));
        }
        return request.recipients().size();
    }

    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> list(CurrentUser user, boolean unreadOnly, Pageable pageable) {
        var page = unreadOnly
                ? repository.findByRecipientAndReadFalse(user.username(), pageable)
                : repository.findByRecipient(user.username(), pageable);
        return PageResponse.of(page, this::toResponse);
    }

    @Transactional(readOnly = true)
    public long unreadCount(CurrentUser user) {
        return repository.countByRecipientAndReadFalse(user.username());
    }

    @Transactional
    public NotificationResponse markRead(CurrentUser user, Long id) {
        Notification n = repository.findByIdAndRecipient(id, user.username())
                .orElseThrow(() -> NotFoundException.of("Notification", id));
        if (!n.isRead()) {
            n.setRead(true);
            n.setReadAt(clock.instant());
        }
        return toResponse(n);
    }

    @Transactional
    public int markAllRead(CurrentUser user) {
        return repository.markAllRead(user.username(), clock.instant());
    }

    @Scheduled(cron = "0 45 3 * * *")
    @Transactional
    public void purgeOld() {
        int deleted = repository.deleteOlderThan(clock.instant().minus(Duration.ofDays(properties.retentionDays())));
        log.info("Purged {} notifications older than {} days", deleted, properties.retentionDays());
    }

    private void push(String recipient, NotificationResponse response) {
        try {
            messagingTemplate.convertAndSendToUser(recipient, WebSocketConfig.USER_QUEUE, response);
        } catch (MessagingException e) {
            // The notification is stored; the client will see it on its next poll/reload.
            log.warn("WebSocket push to {} failed: {}", recipient, e.getMessage());
        }
    }

    private NotificationResponse toResponse(Notification n) {
        return new NotificationResponse(n.getId(), n.getLevel(), n.getCategory(), n.getTitle(), n.getMessage(),
                n.getLink(), n.getData(), n.getSource(), n.isRead(), n.getReadAt(), n.getCreatedAt());
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return "";
        }
        return s.length() > max ? s.substring(0, max) : s;
    }
}

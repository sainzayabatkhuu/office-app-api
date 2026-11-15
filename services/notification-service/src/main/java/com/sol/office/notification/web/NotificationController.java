package com.sol.office.notification.web;

import com.sol.office.common.api.MessageResponse;
import com.sol.office.common.api.PageResponse;
import com.sol.office.common.security.SecurityUtils;
import com.sol.office.notification.dto.NotificationDtos.NotificationResponse;
import com.sol.office.notification.dto.NotificationDtos.SendRequest;
import com.sol.office.notification.dto.NotificationDtos.UnreadCount;
import com.sol.office.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Tag(name = "Notifications")
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public PageResponse<NotificationResponse> list(@RequestParam(defaultValue = "false") boolean unreadOnly,
                                                   @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return notificationService.list(SecurityUtils.requireCurrentUser(), unreadOnly, pageable);
    }

    @GetMapping("/unread-count")
    public UnreadCount unreadCount() {
        return new UnreadCount(notificationService.unreadCount(SecurityUtils.requireCurrentUser()));
    }

    @PostMapping("/{id}/read")
    public NotificationResponse markRead(@PathVariable Long id) {
        return notificationService.markRead(SecurityUtils.requireCurrentUser(), id);
    }

    @PostMapping("/read-all")
    public MessageResponse markAllRead() {
        int updated = notificationService.markAllRead(SecurityUtils.requireCurrentUser());
        return MessageResponse.of("Marked as read", Map.of("updated", updated));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('NOTIFICATION_SEND')")
    public MessageResponse send(@Valid @RequestBody SendRequest request) {
        int sent = notificationService.send(SecurityUtils.requireCurrentUser(), request);
        return MessageResponse.of("Sent", Map.of("recipients", sent));
    }
}

package com.sol.office.notification.repository;

import com.sol.office.notification.domain.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Page<Notification> findByRecipient(String recipient, Pageable pageable);

    Page<Notification> findByRecipientAndReadFalse(String recipient, Pageable pageable);

    long countByRecipientAndReadFalse(String recipient);

    Optional<Notification> findByIdAndRecipient(Long id, String recipient);

    @Modifying
    @Query("update Notification n set n.read = true, n.readAt = :now where n.recipient = :recipient and n.read = false")
    int markAllRead(@Param("recipient") String recipient, @Param("now") Instant now);

    @Modifying
    @Query("delete from Notification n where n.createdAt < :before")
    int deleteOlderThan(@Param("before") Instant before);
}

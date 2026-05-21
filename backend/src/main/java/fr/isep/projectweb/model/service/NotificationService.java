package fr.isep.projectweb.model.service;

import fr.isep.projectweb.model.dao.NotificationRepository;
import fr.isep.projectweb.model.dto.response.NotificationResponse;
import fr.isep.projectweb.model.dto.response.UnreadNotificationCountResponse;
import fr.isep.projectweb.model.entity.Notification;
import fr.isep.projectweb.model.entity.User;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
public class NotificationService {

    public static final String FRIEND_REQUEST_RECEIVED = "FRIEND_REQUEST_RECEIVED";
    public static final String FRIEND_REQUEST_ACCEPTED = "FRIEND_REQUEST_ACCEPTED";
    public static final String FRIEND_REQUEST_REJECTED = "FRIEND_REQUEST_REJECTED";
    public static final String CHAT_MESSAGE_RECEIVED = "CHAT_MESSAGE_RECEIVED";
    public static final String EVENT_REGISTRATION_CREATED = "EVENT_REGISTRATION_CREATED";
    public static final String EVENT_REGISTRATION_STATUS_CHANGED = "EVENT_REGISTRATION_STATUS_CHANGED";
    public static final String POST_REVIEW_CREATED = "POST_REVIEW_CREATED";
    public static final String EVENT_REVIEW_CREATED = "EVENT_REVIEW_CREATED";
    public static final String EVENT_UPDATED = "EVENT_UPDATED";
    public static final String EVENT_CANCELLED = "EVENT_CANCELLED";
    public static final String EVENT_STARTS_IN_1_DAY = "EVENT_STARTS_IN_1_DAY";
    public static final String EVENT_STARTS_IN_2_HOURS = "EVENT_STARTS_IN_2_HOURS";

    private static final Set<String> ALLOWED_TYPES = Set.of(
            FRIEND_REQUEST_RECEIVED,
            FRIEND_REQUEST_ACCEPTED,
            FRIEND_REQUEST_REJECTED,
            CHAT_MESSAGE_RECEIVED,
            EVENT_REGISTRATION_CREATED,
            EVENT_REGISTRATION_STATUS_CHANGED,
            POST_REVIEW_CREATED,
            EVENT_REVIEW_CREATED,
            EVENT_UPDATED,
            EVENT_CANCELLED,
            EVENT_STARTS_IN_1_DAY,
            EVENT_STARTS_IN_2_HOURS
    );
    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 100;

    private final NotificationRepository notificationRepository;
    private final CurrentUserService currentUserService;

    public NotificationService(NotificationRepository notificationRepository,
                               CurrentUserService currentUserService) {
        this.notificationRepository = notificationRepository;
        this.currentUserService = currentUserService;
    }

    public List<NotificationResponse> getMyNotifications(String status,
                                                         String type,
                                                         LocalDateTime before,
                                                         Integer limit,
                                                         Jwt jwt) {
        UUID currentUserId = currentUserService.getCurrentUserId(jwt);
        return notificationRepository.findPageForRecipient(
                        currentUserId,
                        normalizeType(type),
                        before,
                        normalizeStatus(status),
                        PageRequest.of(0, normalizeLimit(limit))
                )
                .stream()
                .map(ResponseMapper::toNotificationResponse)
                .toList();
    }

    public UnreadNotificationCountResponse getUnreadCount(Jwt jwt) {
        UUID currentUserId = currentUserService.getCurrentUserId(jwt);
        return new UnreadNotificationCountResponse(
                notificationRepository.countByRecipientIdAndReadAtIsNullAndArchivedAtIsNull(currentUserId)
        );
    }

    @Transactional
    public NotificationResponse markAsRead(UUID notificationId, Jwt jwt) {
        UUID currentUserId = currentUserService.getCurrentUserId(jwt);
        Notification notification = findMyNotification(notificationId, currentUserId);
        if (notification.getReadAt() == null) {
            notification.setReadAt(LocalDateTime.now());
        }
        return ResponseMapper.toNotificationResponse(notificationRepository.save(notification));
    }

    @Transactional
    public UnreadNotificationCountResponse markAllAsRead(String type, Jwt jwt) {
        UUID currentUserId = currentUserService.getCurrentUserId(jwt);
        notificationRepository.markAllAsRead(currentUserId, normalizeType(type), LocalDateTime.now());
        return getUnreadCount(jwt);
    }

    @Transactional
    public void archive(UUID notificationId, Jwt jwt) {
        UUID currentUserId = currentUserService.getCurrentUserId(jwt);
        Notification notification = findMyNotification(notificationId, currentUserId);
        notification.setArchivedAt(LocalDateTime.now());
        notificationRepository.save(notification);
    }

    @Transactional
    public void create(User recipient,
                       User actor,
                       String type,
                       String title,
                       String body,
                       String targetType,
                       UUID targetId,
                       String sourceType,
                       UUID sourceId,
                       String dedupeKey,
                       Map<String, Object> payload) {
        if (recipient == null || recipient.getId() == null) {
            return;
        }
        if (actor != null && Objects.equals(recipient.getId(), actor.getId())) {
            return;
        }
        String normalizedType = normalizeType(type);
        String normalizedDedupeKey = requireText(dedupeKey, "dedupeKey");
        if (notificationRepository.existsByRecipientIdAndDedupeKey(recipient.getId(), normalizedDedupeKey)) {
            return;
        }

        Notification notification = new Notification();
        notification.setRecipient(recipient);
        notification.setActor(actor);
        notification.setType(normalizedType);
        notification.setTitle(requireText(title, "title"));
        notification.setBody(normalizeOptional(body));
        notification.setTargetType(requireText(targetType, "targetType"));
        notification.setTargetId(requireUuid(targetId, "targetId"));
        notification.setSourceType(requireText(sourceType, "sourceType"));
        notification.setSourceId(requireUuid(sourceId, "sourceId"));
        notification.setDedupeKey(normalizedDedupeKey);
        notification.setPayload(payload);

        try {
            notificationRepository.save(notification);
        } catch (DataIntegrityViolationException ignored) {
            // Concurrent duplicate notifications are harmless when the database has the dedupe unique constraint.
        }
    }

    private Notification findMyNotification(UUID notificationId, UUID currentUserId) {
        return notificationRepository.findByIdAndRecipientIdAndArchivedAtIsNull(notificationId, currentUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found"));
    }

    private String normalizeStatus(String status) {
        String normalized = normalizeOptional(status);
        if (normalized == null) {
            return "ALL";
        }
        normalized = normalized.toUpperCase(Locale.ROOT);
        if (!Set.of("ALL", "UNREAD", "READ").contains(normalized)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Notification status must be all, unread, or read");
        }
        return normalized;
    }

    private String normalizeType(String type) {
        String normalized = normalizeOptional(type);
        if (normalized == null) {
            return null;
        }
        normalized = normalized.toUpperCase(Locale.ROOT);
        if (!ALLOWED_TYPES.contains(normalized)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported notification type");
        }
        return normalized;
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null) {
            return DEFAULT_LIMIT;
        }
        if (limit < 1) {
            return 1;
        }
        return Math.min(limit, MAX_LIMIT);
    }

    private String requireText(String value, String fieldName) {
        String normalized = normalizeOptional(value);
        if (normalized == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, fieldName + " must not be blank");
        }
        return normalized;
    }

    private UUID requireUuid(UUID value, String fieldName) {
        if (value == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, fieldName + " must not be null");
        }
        return value;
    }

    private String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}

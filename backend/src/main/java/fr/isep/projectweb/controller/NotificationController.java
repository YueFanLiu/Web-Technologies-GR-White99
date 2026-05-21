package fr.isep.projectweb.controller;

import fr.isep.projectweb.model.dto.request.NotificationReadAllRequest;
import fr.isep.projectweb.model.dto.response.NotificationResponse;
import fr.isep.projectweb.model.dto.response.UnreadNotificationCountResponse;
import fr.isep.projectweb.model.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
@Tag(name = "Notifications", description = "Current user notification endpoints")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    @Operation(summary = "Get the current authenticated user's notifications")
    public List<NotificationResponse> getMyNotifications(@RequestParam(required = false) String status,
                                                         @RequestParam(required = false) String type,
                                                         @RequestParam(required = false) LocalDateTime before,
                                                         @RequestParam(required = false) Integer limit,
                                                         @AuthenticationPrincipal Jwt jwt) {
        return notificationService.getMyNotifications(status, type, before, limit, jwt);
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Get the current authenticated user's unread notification count")
    public UnreadNotificationCountResponse getUnreadCount(@AuthenticationPrincipal Jwt jwt) {
        return notificationService.getUnreadCount(jwt);
    }

    @PostMapping("/{id}/read")
    @Operation(summary = "Mark one notification as read")
    public NotificationResponse markAsRead(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        return notificationService.markAsRead(id, jwt);
    }

    @PostMapping("/read-all")
    @Operation(summary = "Mark all current user notifications as read")
    public UnreadNotificationCountResponse markAllAsRead(@RequestBody(required = false) NotificationReadAllRequest request,
                                                         @AuthenticationPrincipal Jwt jwt) {
        return notificationService.markAllAsRead(request != null ? request.getType() : null, jwt);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Archive one notification")
    public ResponseEntity<Void> archive(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        notificationService.archive(id, jwt);
        return ResponseEntity.noContent().build();
    }
}

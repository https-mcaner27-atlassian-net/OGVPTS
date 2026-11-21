package edu.maltepe.ogvpts.controller;

import edu.maltepe.ogvpts.dto.NotificationDtos.NotificationResponse;
import edu.maltepe.ogvpts.dto.NotificationDtos.UnreadCount;
import edu.maltepe.ogvpts.security.CurrentUserProvider;
import edu.maltepe.ogvpts.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@Tag(name = "6. Bildirimler", description = "FR-08")
public class NotificationController {

    private final NotificationService notifications;
    private final CurrentUserProvider currentUser;

    public NotificationController(NotificationService notifications, CurrentUserProvider currentUser) {
        this.notifications = notifications;
        this.currentUser = currentUser;
    }

    @GetMapping
    @Operation(summary = "Son 100 bildirim")
    public List<NotificationResponse> list() {
        return notifications.list(currentUser.get());
    }

    @GetMapping("/unread-count")
    public UnreadCount unreadCount() {
        return new UnreadCount(notifications.unreadCount(currentUser.get()));
    }

    @PostMapping("/{id}/read")
    public ResponseEntity<Void> markRead(@PathVariable Integer id) {
        notifications.markRead(currentUser.get(), id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/read-all")
    public ResponseEntity<Void> markAllRead() {
        notifications.markAllRead(currentUser.get());
        return ResponseEntity.noContent().build();
    }
}

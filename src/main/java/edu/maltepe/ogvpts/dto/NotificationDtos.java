package edu.maltepe.ogvpts.dto;

import java.time.LocalDateTime;

public final class NotificationDtos {

    private NotificationDtos() {
    }

    public record NotificationResponse(Integer id, String type, String message, boolean read, LocalDateTime createdAt) {
    }

    public record UnreadCount(long count) {
    }
}

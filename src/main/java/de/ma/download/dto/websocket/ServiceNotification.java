package de.ma.download.dto.websocket;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceNotification {
    private NotificationType type;
    private String message;
    private Instant timestamp;

    public enum NotificationType {
        INFO, WARNING, ERROR
    }
}

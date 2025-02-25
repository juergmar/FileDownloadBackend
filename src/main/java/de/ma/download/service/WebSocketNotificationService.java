package de.ma.download.service;

import de.ma.download.dto.websocket.JobStatusUpdateMessage;
import de.ma.download.dto.websocket.ServiceNotification;
import de.ma.download.entity.JobEntity;
import de.ma.download.model.JobStatusEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebSocketNotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    public void notifyJobStatusChange(JobEntity job) {
        String destination = "/queue/job-updates/" + job.getJobId();
        String userId = job.getUserId();

        JobStatusUpdateMessage message = JobStatusUpdateMessage.builder()
                .jobId(job.getJobId())
                .fileType(job.getFileType())
                .status(job.getStatus())
                .updatedAt(Instant.now())
                .fileName(job.getFileName())
                .fileSize(job.getFileSize())
                .errorMessage(job.getFailureReason())
                .build();

        try {
            messagingTemplate.convertAndSendToUser(userId, destination, message);

            if (job.getStatus() == JobStatusEnum.COMPLETED) {
                sendServiceNotification(userId,
                        ServiceNotification.NotificationType.INFO,
                        "Your file " + job.getFileName() + " is ready for download.");
            } else if (job.getStatus() == JobStatusEnum.FAILED) {
                sendServiceNotification(userId,
                        ServiceNotification.NotificationType.ERROR,
                        "File generation failed: " + job.getFailureReason());
            }

            log.debug("Sent job status update via WebSocket to user {}: {}", userId, message);
        } catch (Exception e) {
            log.error("Failed to send WebSocket notification", e);
        }
    }

    public void sendServiceNotification(String userId, ServiceNotification.NotificationType type, String message) {
        ServiceNotification notification = ServiceNotification.builder()
                .type(type)
                .message(message)
                .timestamp(Instant.now())
                .build();

        messagingTemplate.convertAndSendToUser(
                userId,
                "/queue/notifications",
                notification);
    }
}

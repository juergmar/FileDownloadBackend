package de.ma.download.controller;

import de.ma.download.dto.websocket.JobStatusUpdateMessage;
import de.ma.download.dto.websocket.JobSubscriptionRequest;
import de.ma.download.entity.JobEntity;
import de.ma.download.exception.JobNotFoundException;
import de.ma.download.exception.ResourceAccessDeniedException;
import de.ma.download.repository.JobRepository;
import de.ma.download.service.UserContextService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.time.Instant;
import java.util.Map;

@Slf4j
@Controller
@RequiredArgsConstructor
public class WebSocketController {

    private final SimpMessagingTemplate messagingTemplate;
    private final JobRepository jobRepository;
    private final UserContextService userContextService;

    @MessageMapping("/subscribe-job")
    public void subscribeToJobUpdates(
            @Payload JobSubscriptionRequest request,
            SimpMessageHeaderAccessor headerAccessor) {

        Principal principal = headerAccessor.getUser();
        if (principal == null) {
            log.error("No Principal found in WebSocket message");
            return;
        }

        String userId = userContextService.getUserIdFromPrincipal(principal);
        String jobId = request.getJobId();
        log.debug("User {} subscribed to updates for job {}", userId, jobId);

        try {
            JobEntity job = jobRepository.findById(jobId)
                    .orElseThrow(() -> new JobNotFoundException(jobId));

            boolean isAdmin = userContextService.isPrincipalAdmin(principal);
            if (!job.getUserId().equals(userId) && !isAdmin) {
                log.warn("User {} attempted to access job {} owned by {}", userId, jobId, job.getUserId());
                throw new ResourceAccessDeniedException("You are not authorized to access this job");
            }

            JobStatusUpdateMessage message = JobStatusUpdateMessage.builder()
                    .jobId(jobId)
                    .fileType(job.getFileType())
                    .status(job.getStatus())
                    .updatedAt(Instant.now())
                    .fileName(job.getFileName())
                    .fileSize(job.getFileSize())
                    .errorMessage(job.getFailureReason())
                    .build();

            messagingTemplate.convertAndSendToUser(
                    userId,
                    "/queue/job-updates/" + jobId,
                    message);

            log.debug("Sent update for job {} to user {}", jobId, userId);

        } catch (JobNotFoundException e) {
            log.error("Job not found: {}", jobId);
            messagingTemplate.convertAndSendToUser(
                    userId,
                    "/queue/notifications",
                    Map.of(
                            "type", "ERROR",
                            "message", "Job not found: " + jobId,
                            "timestamp", Instant.now().toString()
                    )
            );
        } catch (ResourceAccessDeniedException e) {
            log.error("Access denied for user {} to job {}", userId, jobId);
            messagingTemplate.convertAndSendToUser(
                    userId,
                    "/queue/notifications",
                    Map.of(
                            "type", "ERROR",
                            "message", "You are not authorized to access this job",
                            "timestamp", Instant.now().toString()
                    )
            );
        } catch (Exception e) {
            log.error("Error processing subscription for job {}: {}", jobId, e.getMessage(), e);
            messagingTemplate.convertAndSendToUser(
                    userId,
                    "/queue/notifications",
                    Map.of(
                            "type", "ERROR",
                            "message", "Error processing your request",
                            "timestamp", Instant.now().toString()
                    )
            );
        }
    }
}

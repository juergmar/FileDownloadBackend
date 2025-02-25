package de.ma.download.controller;

import de.ma.download.dto.JobStatusDTO;
import de.ma.download.dto.websocket.JobStatusUpdateMessage;
import de.ma.download.dto.websocket.JobSubscriptionRequest;
import de.ma.download.entity.JobEntity;
import de.ma.download.exception.JobNotFoundException;
import de.ma.download.exception.ResourceAccessDeniedException;
import de.ma.download.repository.JobRepository;
import de.ma.download.service.JobManagementService;
import de.ma.download.service.UserContextService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Slf4j
@Controller
@RequiredArgsConstructor
public class WebSocketController {

    private final SimpMessagingTemplate messagingTemplate;
    private final JobManagementService jobManagementService;
    private final JobRepository jobRepository;
    private final UserContextService userContextService;

    @MessageMapping("/subscribe-job")
    public void subscribeToJobUpdates(
            @Payload JobSubscriptionRequest request,
            Principal principal) {

        String jobId = request.getJobId();
        String userId = principal.getName();

        log.debug("User {} subscribed to updates for job {}", userId, jobId);

        JobEntity job = jobRepository.findById(jobId)
                .orElseThrow(() -> new JobNotFoundException(jobId));

        if (!job.getUserId().equals(userId) && !userContextService.isCurrentUserAdmin()) {
            throw new ResourceAccessDeniedException("You are not authorized to access this job");
        }

        JobStatusDTO status = jobManagementService.getJobStatus(jobId);

        JobStatusUpdateMessage message = JobStatusUpdateMessage.builder()
                .jobId(jobId)
                .fileType(status.getFileType())
                .status(status.getStatus())
                .updatedAt(status.getLastAccessed())
                .errorMessage(status.getFailureReason())
                .build();

        messagingTemplate.convertAndSendToUser(
                userId,
                "/queue/job-updates/" + jobId,
                message);
    }
}

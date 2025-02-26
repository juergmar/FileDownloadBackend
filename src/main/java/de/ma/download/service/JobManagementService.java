package de.ma.download.service;

import de.ma.download.dto.JobDTO;
import de.ma.download.dto.PagedJobResponse;
import de.ma.download.dto.ReportRequest;
import de.ma.download.entity.JobEntity;
import de.ma.download.event.FileGenerationEvent;
import de.ma.download.event.model.JobCreatedEvent;
import de.ma.download.event.store.JobEventStore;
import de.ma.download.exception.JobAlreadyExistsException;
import de.ma.download.exception.JobNotFoundException;
import de.ma.download.exception.ResourceAccessDeniedException;
import de.ma.download.exception.ServiceOverloadedException;
import de.ma.download.generator.GeneratorRegistry;
import de.ma.download.mapper.JobMapper;
import de.ma.download.model.FileType;
import de.ma.download.model.JobStatusEnum;
import de.ma.download.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.Principal;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobManagementService {

    private final JobRepository jobRepository;
    private final JobMapper jobMapper;
    private final UserContextService userContextService;
    private final JobCleanupService jobCleanupService;
    private final WebSocketNotificationService notificationService;
    private final ApplicationEventPublisher eventPublisher;
    private final JobEventStore eventStore;
    private final JobCommandService jobCommandService;
    private final GeneratorRegistry generatorRegistry;

    @Value("${file.generation.max-jobs:1000}")
    private int maxJobs;

    /**
     * Initiate a job with a specific report request
     */
    @Transactional
    public String initiateJob(ReportRequest request, Principal principal) {
        FileType fileType = request.getFileType();

        if (jobRepository.count() >= maxJobs) {
            log.warn("Max job capacity reached ({}). Triggering cleanup.", maxJobs);
            jobCleanupService.cleanupExpiredJobs();

            if (jobRepository.count() >= maxJobs) {
                throw new ServiceOverloadedException("System is currently processing too many jobs. Please try again later.");
            }
        }

        String userId = principal != null ?
                userContextService.getUserIdFromPrincipal(principal) :
                userContextService.getCurrentUserId();

        List<JobStatusEnum> inProgressStatuses = Arrays.asList(
                JobStatusEnum.PENDING, JobStatusEnum.IN_PROGRESS);

        List<JobEntity> existingJobs = jobRepository.findByUserIdAndFileTypeAndStatusIn(
                userId, fileType, inProgressStatuses);

        if (!existingJobs.isEmpty()) {
            JobEntity existingJob = existingJobs.getFirst();
            throw new JobAlreadyExistsException("You already have a " + fileType +
                    " generation in progress (Job ID: " + existingJob.getJobId() + ")");
        }

        String jobId = UUID.randomUUID().toString();
        Instant now = Instant.now();

        JobCreatedEvent event = JobCreatedEvent.builder()
                .jobId(jobId)
                .fileType(fileType)
                .userId(userId)
                .timestamp(now)
                .build();

        eventStore.saveEvent(event);

        JobEntity job = new JobEntity();
        job.setJobId(jobId);
        job.setFileType(fileType);
        job.setStatus(JobStatusEnum.PENDING);
        job.setCreatedAt(now);
        job.setUserId(userId);

        jobRepository.save(job);

        log.info("Job initiated: {}, Type: {}, User: {}", jobId, fileType, userId);

        notificationService.notifyJobStatusChange(job);

        eventPublisher.publishEvent(new FileGenerationEvent(this, jobId, fileType, request));

        return jobId;
    }

    /**
     * Initiate a job without a principal
     */
    @Transactional
    public String initiateJob(ReportRequest request) {
        return initiateJob(request, null);
    }

    // Rest of the methods remain the same with minimal changes
    // ...

    @Transactional
    public boolean cancelJob(String jobId, Principal principal) {
        JobEntity job = jobRepository.findById(jobId)
                .orElseThrow(() -> new JobNotFoundException(jobId));

        verifyOwnership(job, principal);

        if (job.getStatus() == JobStatusEnum.PENDING || job.getStatus() == JobStatusEnum.IN_PROGRESS) {
            try {
                jobCommandService.updateJobStatus(jobId, JobStatusEnum.CANCELLED);
                log.info("Job cancelled: {}", jobId);
                return true;
            } catch (Exception e) {
                log.error("Failed to cancel job: {}", jobId, e);
                return false;
            }
        }

        return false;
    }

    @Transactional
    public boolean cancelJob(String jobId) {
        return cancelJob(jobId, null);
    }

    @Transactional
    public String retryJob(String jobId, Principal principal) {
        JobEntity originalJob = jobRepository.findById(jobId)
                .orElseThrow(() -> new JobNotFoundException(jobId));

        verifyOwnership(originalJob, principal);

        if (originalJob.getStatus() != JobStatusEnum.FAILED) {
            throw new IllegalStateException("Only failed jobs can be retried");
        }

        // Create a default request for the file type
        FileType fileType = originalJob.getFileType();
        try {
            var generator = generatorRegistry.getGenerator(fileType);
            var requestClass = generator.getRequestType();
            var request = requestClass.getDeclaredConstructor().newInstance();

            return initiateJob(request, principal);
        } catch (Exception e) {
            log.error("Failed to create default request for retry", e);
            throw new IllegalStateException("Could not retry job", e);
        }
    }

    @Transactional
    public String retryJob(String jobId) {
        return retryJob(jobId, null);
    }

    @Transactional(readOnly = true)
    public PagedJobResponse getRecentJobs(int page, int size, Principal principal) {
        String userId = principal != null ?
                userContextService.getUserIdFromPrincipal(principal) :
                userContextService.getCurrentUserId();

        Page<JobEntity> jobsPage = jobRepository.findByUserId(
                userId,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        );

        List<JobDTO> jobDTOs = jobsPage.getContent()
                .stream()
                .map(jobMapper::toJobDTO)
                .toList();

        return PagedJobResponse.builder()
                .jobs(jobDTOs)
                .totalItems(jobsPage.getTotalElements())
                .totalPages(jobsPage.getTotalPages())
                .currentPage(page)
                .pageSize(size)
                .build();
    }

    @Transactional(readOnly = true)
    public PagedJobResponse getRecentJobs(int page, int size) {
        return getRecentJobs(page, size, null);
    }

    private void verifyOwnership(JobEntity job) {
        verifyOwnership(job, null);
    }

    private void verifyOwnership(JobEntity job, Principal principal) {
        String currentUserId;
        boolean isAdmin;

        if (principal != null) {
            currentUserId = userContextService.getUserIdFromPrincipal(principal);
            isAdmin = userContextService.isPrincipalAdmin(principal);
        } else {
            currentUserId = userContextService.getCurrentUserId();
            isAdmin = userContextService.isCurrentUserAdmin();
        }

        if (!currentUserId.equals(job.getUserId()) && !isAdmin) {
            log.warn("Access denied: User {} attempted to access job {} owned by {}",
                    currentUserId, job.getJobId(), job.getUserId());
            throw new ResourceAccessDeniedException("You are not authorized to access this job");
        }
    }
}

package de.ma.download.service;

import de.ma.download.dto.JobDTO;
import de.ma.download.dto.JobStatusDTO;
import de.ma.download.dto.PagedJobResponse;
import de.ma.download.entity.JobEntity;
import de.ma.download.exception.JobAlreadyExistsException;
import de.ma.download.exception.JobNotFoundException;
import de.ma.download.exception.ResourceAccessDeniedException;
import de.ma.download.exception.ServiceOverloadedException;
import de.ma.download.mapper.JobMapper;
import de.ma.download.model.FileType;
import de.ma.download.model.JobStatusEnum;
import de.ma.download.repository.JobRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

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
    private final FileGenerationService fileGenerationService;
    private final JobCleanupService jobCleanupService;
    private final WebSocketNotificationService notificationService;

    @Value("${file.generation.max-jobs:1000}")
    private int maxJobs;

    @Transactional
    public String initiateJob(FileType fileType, Object parameters) {
        if (jobRepository.count() >= maxJobs) {
            log.warn("Max job capacity reached ({}). Triggering cleanup.", maxJobs);
            jobCleanupService.cleanupExpiredJobs();

            if (jobRepository.count() >= maxJobs) {
                throw new ServiceOverloadedException("System is currently processing too many jobs. Please try again later.");
            }
        }

        String userId = userContextService.getCurrentUserId();

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

        JobEntity job = JobEntity.builder()
                .jobId(jobId)
                .fileType(fileType)
                .status(JobStatusEnum.PENDING)
                .createdAt(now)
                .lastAccessed(now)
                .userId(userId)
                .build();

        jobRepository.save(job);
        log.info("Job initiated: {}, Type: {}, User: {}",
                jobId, fileType, userId);

        notificationService.notifyJobStatusChange(job);

        fileGenerationService.startGeneration(jobId, fileType, parameters);

        return jobId;
    }

    @Transactional
    @Cacheable(value = "jobStatus", key = "#jobId",
            condition = "#result != null && #result.status == T(de.ma.download.model.JobStatusEnum).COMPLETED")
    public JobStatusDTO getJobStatus(String jobId) {
        JobEntity job = jobRepository.findById(jobId)
                .orElseThrow(() -> new JobNotFoundException(jobId));

        verifyOwnership(job);

        job.updateAccessTime();
        jobRepository.save(job);

        return jobMapper.toJobStatusDTO(job);
    }

    @Transactional
    public boolean cancelJob(String jobId) {
        JobEntity job = jobRepository.findById(jobId)
                .orElseThrow(() -> new JobNotFoundException(jobId));

        verifyOwnership(job);

        if (job.getStatus() == JobStatusEnum.PENDING || job.getStatus() == JobStatusEnum.IN_PROGRESS) {
            job.setStatus(JobStatusEnum.CANCELLED);
            job.setLastAccessed(Instant.now());
            jobRepository.save(job);

            notificationService.notifyJobStatusChange(job);

            log.info("Job cancelled: {}", jobId);
            return true;
        }

        return false;
    }

    @Transactional
    public String retryJob(String jobId) {
        JobEntity originalJob = jobRepository.findById(jobId)
                .orElseThrow(() -> new JobNotFoundException(jobId));

        verifyOwnership(originalJob);

        if (originalJob.getStatus() != JobStatusEnum.FAILED) {
            throw new IllegalStateException("Only failed jobs can be retried");
        }

        return initiateJob(originalJob.getFileType(), null);
    }

    @Transactional
    public PagedJobResponse getRecentJobs(int page, int size) {
        String userId = userContextService.getCurrentUserId();

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



    private void verifyOwnership(JobEntity job) {
        String currentUserId = userContextService.getCurrentUserId();
        if (!currentUserId.equals(job.getUserId()) && !userContextService.isCurrentUserAdmin()) {
            throw new ResourceAccessDeniedException("You are not authorized to access this job");
        }
    }
}

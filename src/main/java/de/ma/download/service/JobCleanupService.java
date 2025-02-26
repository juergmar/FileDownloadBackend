package de.ma.download.service;

import de.ma.download.entity.JobEntity;
import de.ma.download.model.JobStatusEnum;
import de.ma.download.repository.JobRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobCleanupService {
    private final JobRepository jobRepository;
    private final JobCommandService jobCommandService;

    @Value("${file.generation.job-expiry-hours:24}")
    private int jobExpiryHours;

    @Scheduled(fixedDelayString = "${file.generation.cleanup-interval-minutes:30}")
    @CacheEvict(value = "jobStatus", allEntries = true)
    @Transactional
    public void cleanupExpiredJobs() {
        Instant expiryTime = Instant.now().minus(Duration.ofHours(jobExpiryHours));

        // Since we no longer have a lastAccessed field, we'll use a different approach
        // to find stale jobs. We'll consider any job that's been in-progress or pending
        // for more than the expiry time (based on creation date) as stale.
        List<JobStatusEnum> inProgressStatuses = Arrays.asList(
                JobStatusEnum.PENDING, JobStatusEnum.IN_PROGRESS);

        List<JobEntity> staleJobs = jobRepository.findByStatusInAndCreatedAtBefore(
                inProgressStatuses, expiryTime);

        // Mark stale in-progress jobs as failed using command service
        for (JobEntity job : staleJobs) {
            try {
                jobCommandService.failJob(job.getJobId(), "Job timed out");
                log.info("Marked stale job as failed: {}", job.getJobId());
            } catch (Exception e) {
                log.error("Failed to mark job as timed out: {}", job.getJobId(), e);
            }
        }

        // Delete very old jobs (data retention policy)
        Instant retentionCutoff = Instant.now().minus(Duration.ofDays(30)); // 30-day retention
        int deletedCount = jobRepository.deleteJobsOlderThan(retentionCutoff);

        if (deletedCount > 0) {
            log.info("Deleted {} jobs older than 30 days", deletedCount);
        }
    }
}

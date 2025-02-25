// File: JobCleanupService.java
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

    @Value("${file.generation.job-expiry-hours:24}")
    private int jobExpiryHours;

    /**
     * Scheduled job to clean up old/expired jobs
     */
    @Scheduled(fixedDelayString = "${file.generation.cleanup-interval-minutes:30}")
    @CacheEvict(value = "jobStatus", allEntries = true)
    @Transactional
    public void cleanupExpiredJobs() {
        Instant expiryTime = Instant.now().minus(Duration.ofHours(jobExpiryHours));

        // Find stale jobs
        List<JobStatusEnum> inProgressStatuses = Arrays.asList(
                JobStatusEnum.PENDING, JobStatusEnum.IN_PROGRESS);
        List<JobEntity> staleJobs = jobRepository.findStaleJobs(inProgressStatuses, expiryTime);

        // Mark stale in-progress jobs as failed
        for (JobEntity job : staleJobs) {
            job.markFailed("Job timed out");
            jobRepository.save(job);
            log.info("Marked stale job as failed: {}", job.getJobId());
        }

        // Delete very old jobs (data retention policy)
        Instant retentionCutoff = Instant.now().minus(Duration.ofDays(30)); // 30-day retention
        int deletedCount = jobRepository.deleteJobsOlderThan(retentionCutoff);

        if (deletedCount > 0) {
            log.info("Deleted {} jobs older than 30 days", deletedCount);
        }
    }
}

package de.ma.download.service;

import de.ma.download.dto.GeneratedFile;
import de.ma.download.entity.JobEntity;
import de.ma.download.event.JobEventSourcingService;
import de.ma.download.event.model.*;
import de.ma.download.event.store.JobEventStore;
import de.ma.download.exception.JobNotFoundException;
import de.ma.download.model.JobStatusEnum;
import de.ma.download.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobCommandService {
    private final JobRepository jobRepository;
    private final JobEventStore eventStore;
    private final JobEventSourcingService eventSourcingService;
    private final WebSocketNotificationService notificationService;

    private static final int MAX_RETRY_ATTEMPTS = 3;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public JobEntity updateJobStatus(String jobId, JobStatusEnum newStatus) {
        for (int attempt = 1; attempt <= MAX_RETRY_ATTEMPTS; attempt++) {
            try {
                JobEntity job = jobRepository.findById(jobId)
                        .orElseThrow(() -> new JobNotFoundException(jobId));

                JobStatusChangedEvent event = JobStatusChangedEvent.builder()
                        .jobId(jobId)
                        .oldStatus(job.getStatus())
                        .newStatus(newStatus)
                        .userId(job.getUserId())
                        .timestamp(Instant.now())
                        .build();

                eventStore.saveEvent(event);

                JobStatusEnum oldStatus = job.getStatus();
                job.setStatus(newStatus);

                try {
                    jobRepository.save(job);
                    notificationService.notifyJobStatusChange(job);
                    return job;
                } catch (OptimisticLockingFailureException e) {
                    if (attempt == MAX_RETRY_ATTEMPTS) {
                        log.warn("Optimistic locking failed after {} attempts for job {}, reconstructing from events",
                                attempt, jobId);

                        job = eventSourcingService.reconstructJobState(jobId);
                        job.setStatus(newStatus);

                        try {
                            jobRepository.save(job);
                            notificationService.notifyJobStatusChange(job);
                            return job;
                        } catch (Exception ex) {
                            log.error("Failed to save reconstructed job: {}", jobId, ex);
                            throw ex;
                        }
                    }

                    log.warn("Optimistic locking failure on attempt {}/{} for job {}, retrying...",
                            attempt, MAX_RETRY_ATTEMPTS, jobId);
                    try {
                        Thread.sleep(100L * attempt);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                }
            } catch (JobNotFoundException e) {
                throw e;
            } catch (Exception e) {
                if (attempt == MAX_RETRY_ATTEMPTS) {
                    log.error("Failed to update job status after {} attempts: {}", MAX_RETRY_ATTEMPTS, jobId, e);
                    throw e;
                }

                log.warn("Error updating job status on attempt {}/{}: {}",
                        attempt, MAX_RETRY_ATTEMPTS, e.getMessage());
                try {
                    Thread.sleep(100L * attempt);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        throw new IllegalStateException("Failed to update job status after " + MAX_RETRY_ATTEMPTS + " attempts");
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public JobEntity completeJob(String jobId, GeneratedFile file) {
        for (int attempt = 1; attempt <= MAX_RETRY_ATTEMPTS; attempt++) {
            try {
                JobEntity job = jobRepository.findById(jobId)
                        .orElseThrow(() -> new JobNotFoundException(jobId));

                Instant now = Instant.now();

                JobCompletedEvent event = JobCompletedEvent.builder()
                        .jobId(jobId)
                        .fileName(file.getFileName())
                        .contentType(file.getContentType())
                        .fileSize((long) file.getFileData().length)
                        .fileData(file.getFileData())
                        .userId(job.getUserId())
                        .timestamp(now)
                        .build();

                eventStore.saveEvent(event);

                job.setStatus(JobStatusEnum.COMPLETED);
                job.setFileName(file.getFileName());
                job.setContentType(file.getContentType());
                job.setFileSize((long) file.getFileData().length);
                job.setFileData(file.getFileData());
                job.setCompletedAt(now);

                try {
                    jobRepository.save(job);
                    notificationService.notifyJobStatusChange(job);
                    return job;
                } catch (OptimisticLockingFailureException e) {
                    if (attempt == MAX_RETRY_ATTEMPTS) {
                        log.warn("Optimistic locking failed after {} attempts for job {}, reconstructing from events",
                                attempt, jobId);

                        job = eventSourcingService.reconstructJobState(jobId);

                        try {
                            jobRepository.save(job);
                            notificationService.notifyJobStatusChange(job);
                            return job;
                        } catch (Exception ex) {
                            log.error("Failed to save reconstructed job: {}", jobId, ex);
                            throw ex;
                        }
                    }

                    log.warn("Optimistic locking failure on attempt {}/{} for job {}, retrying...",
                            attempt, MAX_RETRY_ATTEMPTS, jobId);
                    try {
                        Thread.sleep(100L * attempt);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                }
            } catch (JobNotFoundException e) {
                throw e;
            } catch (Exception e) {
                if (attempt == MAX_RETRY_ATTEMPTS) {
                    log.error("Failed to complete job after {} attempts: {}", MAX_RETRY_ATTEMPTS, jobId, e);
                    throw e;
                }

                log.warn("Error completing job on attempt {}/{}: {}",
                        attempt, MAX_RETRY_ATTEMPTS, e.getMessage());
                try {
                    Thread.sleep(100L * attempt);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        throw new IllegalStateException("Failed to complete job after " + MAX_RETRY_ATTEMPTS + " attempts");
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public JobEntity failJob(String jobId, String reason) {
        for (int attempt = 1; attempt <= MAX_RETRY_ATTEMPTS; attempt++) {
            try {
                JobEntity job = jobRepository.findById(jobId)
                        .orElseThrow(() -> new JobNotFoundException(jobId));

                JobFailedEvent event = JobFailedEvent.builder()
                        .jobId(jobId)
                        .failureReason(reason)
                        .userId(job.getUserId())
                        .timestamp(Instant.now())
                        .build();

                eventStore.saveEvent(event);

                job.setStatus(JobStatusEnum.FAILED);
                job.setFailureReason(reason);

                try {
                    jobRepository.save(job);
                    notificationService.notifyJobStatusChange(job);
                    return job;
                } catch (OptimisticLockingFailureException e) {
                    if (attempt == MAX_RETRY_ATTEMPTS) {
                        log.warn("Optimistic locking failed after {} attempts for job {}, reconstructing from events",
                                attempt, jobId);

                        job = eventSourcingService.reconstructJobState(jobId);

                        try {
                            jobRepository.save(job);
                            notificationService.notifyJobStatusChange(job);
                            return job;
                        } catch (Exception ex) {
                            log.error("Failed to save reconstructed job: {}", jobId, ex);
                            throw ex;
                        }
                    }

                    log.warn("Optimistic locking failure on attempt {}/{} for job {}, retrying...",
                            attempt, MAX_RETRY_ATTEMPTS, jobId);
                    try {
                        Thread.sleep(100L * attempt);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                }
            } catch (JobNotFoundException e) {
                throw e;
            } catch (Exception e) {
                if (attempt == MAX_RETRY_ATTEMPTS) {
                    log.error("Failed to fail job after {} attempts: {}", MAX_RETRY_ATTEMPTS, jobId, e);
                    throw e;
                }

                log.warn("Error failing job on attempt {}/{}: {}",
                        attempt, MAX_RETRY_ATTEMPTS, e.getMessage());
                try {
                    Thread.sleep(100L * attempt);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        throw new IllegalStateException("Failed to mark job as failed after " + MAX_RETRY_ATTEMPTS + " attempts");
    }

    @Transactional(readOnly = true)
    public boolean isJobCancelled(String jobId) {
        try {
            JobEntity job = jobRepository.findById(jobId).orElse(null);

            if (job == null) {
                job = eventSourcingService.reconstructJobState(jobId);
            }

            return job == null || job.getStatus() == JobStatusEnum.CANCELLED;
        } catch (Exception e) {
            log.error("Error checking if job is cancelled: {}", jobId, e);
            return false;
        }
    }
}

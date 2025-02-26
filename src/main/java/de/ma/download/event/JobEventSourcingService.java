package de.ma.download.event;

import de.ma.download.entity.JobEntity;
import de.ma.download.event.model.*;
import de.ma.download.event.store.JobEventStore;
import de.ma.download.model.JobStatusEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobEventSourcingService {
    private final JobEventStore eventStore;

    @Transactional(readOnly = true)
    public JobEntity reconstructJobState(String jobId) {
        List<JobEvent> events = eventStore.getEventsForJob(jobId);

        if (events.isEmpty()) {
            log.warn("No events found for job: {}", jobId);
            return null;
        }

        JobEntity job = new JobEntity();
        job.setJobId(jobId);

        for (JobEvent event : events) {
            applyEvent(job, event);
        }

        return job;
    }

    private void applyEvent(JobEntity job, JobEvent event) {
        if (event instanceof JobCreatedEvent createdEvent) {
            job.setJobId(createdEvent.getJobId());
            job.setFileType(createdEvent.getFileType());
            job.setStatus(JobStatusEnum.PENDING);
            job.setCreatedAt(createdEvent.getTimestamp());
            job.setUserId(createdEvent.getUserId());
        } else if (event instanceof JobStatusChangedEvent statusEvent) {
            job.setStatus(statusEvent.getNewStatus());
        } else if (event instanceof JobCompletedEvent completedEvent) {
            job.setStatus(JobStatusEnum.COMPLETED);
            job.setFileName(completedEvent.getFileName());
            job.setContentType(completedEvent.getContentType());
            job.setFileSize(completedEvent.getFileSize());
            job.setFileData(completedEvent.getFileData());
            job.setCompletedAt(completedEvent.getTimestamp());
        } else if (event instanceof JobFailedEvent failedEvent) {
            job.setStatus(JobStatusEnum.FAILED);
            job.setFailureReason(failedEvent.getFailureReason());
        } else if (event instanceof JobCancelledEvent) {
            job.setStatus(JobStatusEnum.CANCELLED);
        }
    }
}

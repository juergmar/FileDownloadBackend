package de.ma.download.event.store;


import de.ma.download.event.model.JobEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
class JpaJobEventStore implements JobEventStore {
    private final JobEventRepository eventRepository;
    private final JobEventSerializer eventSerializer;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveEvent(JobEvent event) {
        try {
            JobEventType eventType = determineEventType(event);
            byte[] eventData = eventSerializer.serialize(event);

            // Get next sequence number
            Long sequence = eventRepository.findMaxSequenceByJobId(event.getJobId());
            sequence = (sequence == null) ? 1L : sequence + 1;

            JobEventEntity entity = JobEventEntity.builder()
                    .jobId(event.getJobId())
                    .eventType(eventType)
                    .timestamp(event.getTimestamp())
                    .userId(event.getUserId())
                    .eventData(eventData)
                    .sequence(sequence)
                    .build();

            eventRepository.save(entity);
            log.debug("Saved event: {} for job: {}", eventType, event.getJobId());
        } catch (Exception e) {
            log.error("Failed to save event for job: {}", event.getJobId(), e);
            throw new RuntimeException("Failed to save event", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<JobEvent> getEventsForJob(String jobId) {
        List<JobEventEntity> entities = eventRepository.findByJobIdOrderBySequenceAsc(jobId);

        return entities.stream()
                .map(this::convertToEvent)
                .toList();
    }

    private JobEventType determineEventType(JobEvent event) {
        return switch (event) {
            case de.ma.download.event.model.JobCreatedEvent ignored -> JobEventType.JOB_CREATED;
            case de.ma.download.event.model.JobStatusChangedEvent ignored ->
                    JobEventType.JOB_STATUS_CHANGED;
            case de.ma.download.event.model.JobCompletedEvent ignored -> JobEventType.JOB_COMPLETED;
            case de.ma.download.event.model.JobFailedEvent ignored -> JobEventType.JOB_FAILED;
            case de.ma.download.event.model.JobCancelledEvent ignored -> JobEventType.JOB_CANCELLED;
            case null, default -> {
                assert event != null;
                throw new IllegalArgumentException("Unknown event type: " + event.getClass().getName());
            }
        };
    }

    private JobEvent convertToEvent(JobEventEntity entity) {
        try {
            return eventSerializer.deserialize(entity.getEventData(), entity.getEventType());
        } catch (Exception e) {
            log.error("Failed to deserialize event: {} for job: {}", entity.getEventType(), entity.getJobId(), e);
            throw new RuntimeException("Failed to deserialize event", e);
        }
    }
}

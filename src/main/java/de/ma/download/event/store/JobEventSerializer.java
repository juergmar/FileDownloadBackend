package de.ma.download.event.store;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.ma.download.event.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class JobEventSerializer {
    private final ObjectMapper objectMapper;

    public byte[] serialize(JobEvent event) {
        try {
            return objectMapper.writeValueAsBytes(event);
        } catch (Exception e) {
            log.error("Failed to serialize event: {}", event, e);
            throw new RuntimeException("Failed to serialize event", e);
        }
    }

    public JobEvent deserialize(byte[] data, JobEventType type) {
        try {
            return switch (type) {
                case JOB_CREATED -> objectMapper.readValue(data, JobCreatedEvent.class);
                case JOB_STATUS_CHANGED -> objectMapper.readValue(data, JobStatusChangedEvent.class);
                case JOB_COMPLETED -> objectMapper.readValue(data, JobCompletedEvent.class);
                case JOB_FAILED -> objectMapper.readValue(data, JobFailedEvent.class);
                case JOB_CANCELLED -> objectMapper.readValue(data, JobCancelledEvent.class);
            };
        } catch (Exception e) {
            log.error("Failed to deserialize event of type: {}", type, e);
            throw new RuntimeException("Failed to deserialize event", e);
        }
    }
}

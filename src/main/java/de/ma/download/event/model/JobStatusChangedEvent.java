package de.ma.download.event.model;

import de.ma.download.model.JobStatusEnum;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;

@Value
@Builder
public class JobStatusChangedEvent implements JobEvent {
    String jobId;
    JobStatusEnum oldStatus;
    JobStatusEnum newStatus;
    String userId;
    Instant timestamp;
}

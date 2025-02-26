package de.ma.download.event.model;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;

@Value
@Builder
public class JobFailedEvent implements JobEvent {
    String jobId;
    String failureReason;
    String userId;
    Instant timestamp;
}

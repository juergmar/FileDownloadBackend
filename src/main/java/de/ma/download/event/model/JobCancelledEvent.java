package de.ma.download.event.model;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;
@Value
@Builder
public class JobCancelledEvent implements JobEvent {
    String jobId;
    String userId;
    Instant timestamp;
}

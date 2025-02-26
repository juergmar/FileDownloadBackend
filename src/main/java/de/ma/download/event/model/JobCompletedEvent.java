package de.ma.download.event.model;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;

@Value
@Builder
public class JobCompletedEvent implements JobEvent {
    String jobId;
    String fileName;
    String contentType;
    Long fileSize;
    byte[] fileData;
    String userId;
    Instant timestamp;
}

package de.ma.download.event.model;

import de.ma.download.model.FileType;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;

@Value
@Builder
public class JobCreatedEvent implements JobEvent {
    String jobId;
    FileType fileType;
    String userId;
    Instant timestamp;
}

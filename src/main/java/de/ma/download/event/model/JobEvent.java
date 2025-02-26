package de.ma.download.event.model;

import java.time.Instant;

public interface JobEvent {
    String getJobId();
    Instant getTimestamp();
    String getUserId();
}

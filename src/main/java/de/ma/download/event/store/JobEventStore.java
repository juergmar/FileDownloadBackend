package de.ma.download.event.store;

import de.ma.download.event.model.JobEvent;

import java.util.List;

public interface JobEventStore {
    void saveEvent(JobEvent event);
    List<JobEvent> getEventsForJob(String jobId);
}

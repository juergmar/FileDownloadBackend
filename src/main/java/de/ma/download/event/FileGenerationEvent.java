package de.ma.download.event;

import de.ma.download.model.FileType;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class FileGenerationEvent extends ApplicationEvent {
    private final String jobId;
    private final FileType fileType;
    private final Object parameters;

    public FileGenerationEvent(Object source, String jobId, FileType fileType, Object parameters) {
        super(source);
        this.jobId = jobId;
        this.fileType = fileType;
        this.parameters = parameters;
    }
}

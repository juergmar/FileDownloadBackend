package de.ma.download.dto.websocket;

import com.fasterxml.jackson.annotation.JsonInclude;
import de.ma.download.model.FileType;
import de.ma.download.model.JobStatusEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class JobStatusUpdateMessage {
    private String jobId;
    private FileType fileType;
    private JobStatusEnum status;
    private Instant updatedAt;
    private String fileName;
    private Long fileSize;
    private String errorMessage;
}

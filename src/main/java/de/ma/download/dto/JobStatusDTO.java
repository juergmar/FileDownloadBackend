package de.ma.download.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import de.ma.download.model.FileType;
import de.ma.download.model.JobStatusEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Status information for a file generation job")
public class JobStatusDTO {

    @Schema(description = "Unique identifier for the job")
    private String jobId;

    @Schema(description = "Type of file being generated")
    private FileType fileType;

    @Schema(description = "Current status of the job")
    private JobStatusEnum status;

    @Schema(description = "When the job was created")
    private Instant createdAt;

    @Schema(description = "When the job was completed (if finished)")
    private Instant completedAt;

    @Schema(description = "Error message (if failed)")
    private String failureReason;
}

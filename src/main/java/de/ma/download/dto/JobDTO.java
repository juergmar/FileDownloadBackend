package de.ma.download.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import de.ma.download.model.FileType;
import de.ma.download.model.JobStatusEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Data Transfer Object for job information, excluding file data
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Information about a file generation job")
public class JobDTO {

    @Schema(description = "Unique identifier for the job")
    private String jobId;

    @Schema(description = "Type of file being generated")
    private FileType fileType;

    @Schema(description = "Current status of the job")
    private JobStatusEnum status;

    @Schema(description = "Progress percentage (0-100), may be null for external processing")
    private Integer progress;

    @Schema(description = "When the job was created")
    private Instant createdAt;

    @Schema(description = "When the job was last accessed")
    private Instant lastAccessed;

    @Schema(description = "When the job was completed (if finished)")
    private Instant completedAt;

    @Schema(description = "Error message (if failed)")
    private String failureReason;

    @Schema(description = "ID of the user who owns this job")
    private String userId;

    @Schema(description = "Name of the generated file")
    private String fileName;

    @Schema(description = "Size of the file in bytes")
    private Long fileSize;

    @Schema(description = "MIME type of the file")
    private String contentType;

    @Schema(description = "Whether the job has file data available")
    private boolean fileDataAvailable;

    /**
     * Check if the job is completed
     */
    @JsonIgnore
    public boolean isCompleted() {
        return status == JobStatusEnum.COMPLETED;
    }

    /**
     * Check if the job is in progress
     */
    @JsonIgnore
    public boolean isInProgress() {
        return status == JobStatusEnum.PENDING || status == JobStatusEnum.IN_PROGRESS;
    }

    /**
     * Check if the job has failed
     */
    @JsonIgnore
    public boolean isFailed() {
        return status == JobStatusEnum.FAILED;
    }
}

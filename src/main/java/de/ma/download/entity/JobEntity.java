package de.ma.download.entity;

import de.ma.download.model.FileType;
import de.ma.download.model.JobStatusEnum;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Entity
@Table(name = "file_generation_jobs")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobEntity {

    @Id
    @Column(name = "job_id", nullable = false, updatable = false)
    private String jobId;

    @Enumerated(EnumType.STRING)
    @Column(name = "file_type", nullable = false)
    private FileType fileType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private JobStatusEnum status;

    @Lob
    @Column(name = "file_data")
    private byte[] fileData;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "last_accessed")
    private Instant lastAccessed;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "failure_reason")
    private String failureReason;

    @Column(name = "user_id")
    private String userId;

    @Column(name = "file_name")
    private String fileName;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "content_type")
    private String contentType;

    @Version
    private Long version;

    public void updateAccessTime() {
        this.lastAccessed = Instant.now();
    }

    public void markCompleted() {
        this.status = JobStatusEnum.COMPLETED;
        this.completedAt = Instant.now();
        updateAccessTime();
    }

    public void markFailed(String reason) {
        this.status = JobStatusEnum.FAILED;
        this.failureReason = reason;
        updateAccessTime();
    }
}

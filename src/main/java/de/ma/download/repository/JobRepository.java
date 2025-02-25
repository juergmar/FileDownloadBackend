package de.ma.download.repository;

import de.ma.download.entity.JobEntity;
import de.ma.download.model.FileType;
import de.ma.download.model.JobStatusEnum;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface JobRepository extends JpaRepository<JobEntity, String> {

    List<JobEntity> findByUserIdOrderByCreatedAtDesc(String userId);

    Page<JobEntity> findByUserId(String userId, Pageable pageable);

    List<JobEntity> findByStatus(JobStatusEnum status);

    List<JobEntity> findByFileTypeAndStatus(FileType fileType, JobStatusEnum status);

    List<JobEntity> findByUserIdAndFileTypeAndStatus(
            String userId, FileType fileType, JobStatusEnum status);

    List<JobEntity> findByUserIdAndFileTypeAndStatusIn(
            String userId, FileType fileType, List<JobStatusEnum> statuses);

    @Query("SELECT j FROM JobEntity j WHERE j.status IN :statuses AND j.lastAccessed < :cutoffTime")
    List<JobEntity> findStaleJobs(
            @Param("statuses") List<JobStatusEnum> statuses,
            @Param("cutoffTime") Instant cutoffTime);

    @Modifying
    @Query("UPDATE JobEntity j SET j.status = :status, j.lastAccessed = CURRENT_TIMESTAMP WHERE j.jobId = :jobId")
    int updateJobStatus(@Param("jobId") String jobId, @Param("status") JobStatusEnum status);

    @Modifying
    @Query("UPDATE JobEntity j SET j.status = 'COMPLETED', " +
            "j.completedAt = CURRENT_TIMESTAMP, j.lastAccessed = CURRENT_TIMESTAMP " +
            "WHERE j.jobId = :jobId")
    int markJobCompleted(@Param("jobId") String jobId);

    @Modifying
    @Query("UPDATE JobEntity j SET j.status = 'FAILED', " +
            "j.failureReason = :reason, j.lastAccessed = CURRENT_TIMESTAMP " +
            "WHERE j.jobId = :jobId")
    int markJobFailed(@Param("jobId") String jobId, @Param("reason") String reason);

    List<JobEntity> findByCreatedAtBefore(Instant cutoffDate);

    @Query("SELECT new JobEntity(j.jobId, j.fileType, j.status, null, " +
            "j.createdAt, j.lastAccessed, j.completedAt, j.failureReason, j.userId, " +
            "j.fileName, j.fileSize, j.contentType, j.version) " +
            "FROM JobEntity j WHERE j.userId = :userId ORDER BY j.createdAt DESC")
    List<JobEntity> findByUserIdWithoutFileData(@Param("userId") String userId);

    @Modifying
    @Query("DELETE FROM JobEntity j WHERE j.createdAt < :cutoffDate")
    int deleteJobsOlderThan(@Param("cutoffDate") Instant cutoffDate);
}

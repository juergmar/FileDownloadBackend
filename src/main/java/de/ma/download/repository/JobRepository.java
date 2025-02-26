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
    Page<JobEntity> findByUserId(String userId, Pageable pageable);

    List<JobEntity> findByUserIdAndFileTypeAndStatusIn(
            String userId, FileType fileType, List<JobStatusEnum> statuses);

    List<JobEntity> findByStatusInAndCreatedAtBefore(
            List<JobStatusEnum> statuses, Instant cutoffTime);

    @Modifying
    @Query("DELETE FROM JobEntity j WHERE j.createdAt < :cutoffDate")
    int deleteJobsOlderThan(@Param("cutoffDate") Instant cutoffDate);
}

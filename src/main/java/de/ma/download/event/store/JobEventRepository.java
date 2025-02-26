package de.ma.download.event.store;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
interface JobEventRepository extends JpaRepository<JobEventEntity, Long> {
    List<JobEventEntity> findByJobIdOrderBySequenceAsc(String jobId);

    @Query("SELECT MAX(e.sequence) FROM JobEventEntity e WHERE e.jobId = :jobId")
    Long findMaxSequenceByJobId(@Param("jobId") String jobId);
}

// File: FileDownloadService.java
package de.ma.download.service;

import de.ma.download.dto.JobDTO;
import de.ma.download.entity.JobEntity;
import de.ma.download.exception.FileNotReadyException;
import de.ma.download.exception.JobNotFoundException;
import de.ma.download.exception.ResourceAccessDeniedException;
import de.ma.download.mapper.JobMapper;
import de.ma.download.model.JobStatusEnum;
import de.ma.download.repository.JobRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileDownloadService {

    private final JobRepository jobRepository;
    private final JobMapper jobMapper;
    private final UserContextService userContextService;

    /**
     * Prepare a job for download, verifying it's ready
     */
    @Transactional
    public JobDTO prepareForDownload(String jobId) {
        JobEntity job = jobRepository.findById(jobId)
                .orElseThrow(() -> new JobNotFoundException(jobId));

        verifyOwnership(job);

        if (job.getStatus() != JobStatusEnum.COMPLETED) {
            throw new FileNotReadyException(jobId);
        }

        // Update last accessed time
        job.updateAccessTime();
        jobRepository.save(job);

        return jobMapper.toJobDTO(job);
    }

    /**
     * Get the file data for download
     */
    @Transactional
    public byte[] getFileData(String jobId) {
        JobEntity job = jobRepository.findById(jobId)
                .orElseThrow(() -> new JobNotFoundException(jobId));

        verifyOwnership(job);

        if (job.getFileData() == null || job.getFileData().length == 0) {
            throw new IllegalStateException("No file data available for job: " + jobId);
        }

        log.info("File download initiated for job: {}, size: {} bytes",
                jobId, job.getFileSize());

        return job.getFileData();
    }

    /**
     * Verify the current user is authorized to access this job
     */
    private void verifyOwnership(JobEntity job) {
        String currentUserId = userContextService.getCurrentUserId();
        if (!currentUserId.equals(job.getUserId()) && !userContextService.isCurrentUserAdmin()) {
            throw new ResourceAccessDeniedException("You are not authorized to access this job");
        }
    }

}

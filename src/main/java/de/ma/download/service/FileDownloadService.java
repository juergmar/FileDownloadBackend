package de.ma.download.service;

import de.ma.download.dto.JobDTO;
import de.ma.download.entity.JobEntity;
import de.ma.download.exception.FileNotReadyException;
import de.ma.download.exception.JobNotFoundException;
import de.ma.download.exception.ResourceAccessDeniedException;
import de.ma.download.mapper.JobMapper;
import de.ma.download.model.JobStatusEnum;
import de.ma.download.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.Principal;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileDownloadService {

    private final JobRepository jobRepository;
    private final JobMapper jobMapper;
    private final UserContextService userContextService;

    @Transactional
    public JobDTO prepareForDownload(String jobId, Principal principal) {
        JobEntity job = jobRepository.findById(jobId)
                .orElseThrow(() -> new JobNotFoundException(jobId));

        verifyOwnership(job, principal);

        if (job.getStatus() != JobStatusEnum.COMPLETED) {
            throw new FileNotReadyException(jobId);
        }

        return jobMapper.toJobDTO(job);
    }

    @Transactional
    public JobDTO prepareForDownload(String jobId) {
        return prepareForDownload(jobId, null);
    }

    @Transactional
    public byte[] getFileData(String jobId, Principal principal) {
        JobEntity job = jobRepository.findById(jobId)
                .orElseThrow(() -> new JobNotFoundException(jobId));

        verifyOwnership(job, principal);

        if (job.getFileData() == null || job.getFileData().length == 0) {
            throw new IllegalStateException("No file data available for job: " + jobId);
        }

        log.info("File download initiated for job: {}, size: {} bytes",
                jobId, job.getFileSize());

        return job.getFileData();
    }

    @Transactional
    public byte[] getFileData(String jobId) {
        return getFileData(jobId, null);
    }

    private void verifyOwnership(JobEntity job, Principal principal) {
        String currentUserId;
        boolean isAdmin;

        if (principal != null) {
            currentUserId = userContextService.getUserIdFromPrincipal(principal);
            isAdmin = userContextService.isPrincipalAdmin(principal);
        } else {
            currentUserId = userContextService.getCurrentUserId();
            isAdmin = userContextService.isCurrentUserAdmin();
        }

        if (!currentUserId.equals(job.getUserId()) && !isAdmin) {
            log.warn("Access denied: User {} attempted to access job {} owned by {}",
                    currentUserId, job.getJobId(), job.getUserId());
            throw new ResourceAccessDeniedException("You are not authorized to access this job");
        }
    }
}

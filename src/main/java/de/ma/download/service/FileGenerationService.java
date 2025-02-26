package de.ma.download.service;

import de.ma.download.dto.GeneratedFile;
import de.ma.download.entity.JobEntity;
import de.ma.download.event.FileGenerationEvent;
import de.ma.download.exception.JobNotFoundException;
import de.ma.download.generator.FileGenerator;
import de.ma.download.generator.FileGeneratorFactory;
import de.ma.download.model.JobStatusEnum;
import de.ma.download.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileGenerationService {

    private final FileGeneratorFactory fileGeneratorFactory;
    private final JobCommandService jobCommandService;
    private final JobRepository jobRepository;

    @EventListener
    @Async("fileGenerationTaskExecutor")
    @Transactional
    public void handleFileGenerationEvent(FileGenerationEvent event) {
        String jobId = event.getJobId();
        log.info("Received file generation event for job: {}", jobId);
        generateFile(jobId, event.getFileType(), event.getParameters());
    }

    @Transactional
    public void generateFile(String jobId, de.ma.download.model.FileType fileType, Object parameters) {
        try {
            JobEntity job = jobCommandService.updateJobStatus(jobId, JobStatusEnum.IN_PROGRESS);
            String userId = job.getUserId();

            log.info("Starting file generation for job: {}, user: {}", jobId, userId);

            if (jobCommandService.isJobCancelled(jobId)) {
                log.info("Job cancelled before generation: {}", jobId);
                return;
            }

            FileGenerator generator = fileGeneratorFactory.getGenerator(fileType);

            simulateProcessingTime(jobId);

            if (jobCommandService.isJobCancelled(jobId)) {
                log.info("Job cancelled during generation: {}", jobId);
                return;
            }

            GeneratedFile generatedFile = generator.generate(jobId, userId, parameters);

            if (jobCommandService.isJobCancelled(jobId)) {
                log.info("Job cancelled after generation: {}", jobId);
                return;
            }

            jobCommandService.completeJob(jobId, generatedFile);

            log.info("File generation completed: {}, size: {} bytes, type: {}",
                    jobId, generatedFile.getFileData().length, generatedFile.getContentType());

        } catch (JobNotFoundException e) {
            log.error("Job not found during generation: {}", jobId, e);
        } catch (Exception e) {
            log.error("Error during file generation: {}", jobId, e);
            try {
                jobCommandService.failJob(jobId, e.getMessage());
            } catch (Exception ex) {
                log.error("Failed to mark job as failed: {}", jobId, ex);
            }
        }
    }

    private void simulateProcessingTime(String jobId) {
        try {
            for (int i = 0; i < 5; i++) {
                if (jobCommandService.isJobCancelled(jobId)) {
                    break;
                }
                Thread.sleep(500);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

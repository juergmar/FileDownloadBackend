package de.ma.download.service;

import de.ma.download.dto.GeneratedFile;
import de.ma.download.dto.ReportRequest;
import de.ma.download.entity.JobEntity;
import de.ma.download.event.FileGenerationEvent;
import de.ma.download.exception.JobNotFoundException;
import de.ma.download.generator.FileGenerator;
import de.ma.download.generator.GeneratorRegistry;
import de.ma.download.model.FileType;
import de.ma.download.model.JobStatusEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileGenerationService {

    private final GeneratorRegistry generatorRegistry;
    private final JobCommandService jobCommandService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async("fileGenerationTaskExecutor")
    public void handleFileGenerationEvent(FileGenerationEvent event) {
        String jobId = event.getJobId();
        log.info("Received file generation event for job: {}", jobId);
        generateFile(jobId, event.getFileType(), event.getParameters());
    }

    public void generateFile(String jobId, FileType fileType, Object parameters) {
        try {
            JobEntity job = jobCommandService.updateJobStatus(jobId, JobStatusEnum.IN_PROGRESS);
            String userId = job.getUserId();

            log.info("Starting file generation for job: {}, user: {}", jobId, userId);

            if (jobCommandService.isJobCancelled(jobId)) {
                log.info("Job cancelled before generation: {}", jobId);
                return;
            }

            // Get the appropriate generator
            FileGenerator<ReportRequest> generator = generatorRegistry.getGenerator(fileType);

            // Cast parameters to the expected type (which will be a ReportRequest subclass)
            ReportRequest request = parameters instanceof ReportRequest ?
                    (ReportRequest) parameters :
                    createDefaultRequest(fileType);

            simulateProcessingTime(jobId);

            if (jobCommandService.isJobCancelled(jobId)) {
                log.info("Job cancelled during generation: {}", jobId);
                return;
            }

            // Generate the file using the type-safe approach
            GeneratedFile generatedFile = generator.generate(jobId, userId, request);

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

    /**
     * Create a default request for the given file type
     */
    private ReportRequest createDefaultRequest(FileType fileType) {
        try {
            FileGenerator<?> generator = generatorRegistry.getGenerator(fileType);
            Class<? extends ReportRequest> requestClass = generator.getRequestType();
            return requestClass.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            log.error("Failed to create default request for file type: {}", fileType, e);
            throw new IllegalStateException("Could not create default request for " + fileType, e);
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

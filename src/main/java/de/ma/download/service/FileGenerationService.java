package de.ma.download.service;

import de.ma.download.dto.GeneratedFile;
import de.ma.download.entity.JobEntity;
import de.ma.download.exception.JobNotFoundException;
import de.ma.download.generator.FileGenerator;
import de.ma.download.generator.FileGeneratorFactory;
import de.ma.download.model.FileType;
import de.ma.download.model.JobStatusEnum;
import de.ma.download.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileGenerationService {

    private final JobRepository jobRepository;
    private final FileGeneratorFactory fileGeneratorFactory;
    private final WebSocketNotificationService notificationService;

    public void startGeneration(String jobId, FileType fileType, Object parameters) {
        generateFile(jobId, fileType, parameters);
    }

    @Async("fileGenerationTaskExecutor")
    protected CompletableFuture<Void> generateFile(String jobId, FileType fileType, Object parameters) {
        try {
            JobEntity job = jobRepository.findById(jobId)
                    .orElseThrow(() -> new JobNotFoundException(jobId));

            log.info("Starting file generation for job: {}", jobId);
            job.setStatus(JobStatusEnum.IN_PROGRESS);
            jobRepository.save(job);

            // Notify status change via WebSocket
            notificationService.notifyJobStatusChange(job);

            FileGenerator generator = fileGeneratorFactory.getGenerator(fileType);

            if (checkJobCancelled(jobId)) {
                log.info("Job cancelled before generation: {}", jobId);
                return CompletableFuture.completedFuture(null);
            }

            // Simulate progress updates during processing
            simulateProgressUpdates(jobId);

            GeneratedFile generatedFile = generator.generate(jobId, parameters);

            if (checkJobCancelled(jobId)) {
                log.info("Job cancelled during generation: {}", jobId);
                return CompletableFuture.completedFuture(null);
            }

            job = jobRepository.findById(jobId)
                    .orElseThrow(() -> new JobNotFoundException(jobId));

            job.setFileData(generatedFile.getFileData());
            job.setFileSize((long) generatedFile.getFileData().length);
            job.setContentType(generatedFile.getContentType());
            job.setFileName(generatedFile.getFileName());
            job.markCompleted();
            jobRepository.save(job);

            // Notify completion via WebSocket
            notificationService.notifyJobStatusChange(job);

            log.info("File generation completed: {}, size: {} bytes, type: {}",
                    jobId, generatedFile.getFileData().length, generatedFile.getContentType());
        } catch (Exception e) {
            log.error("Error during file generation: {}", jobId, e);
            JobEntity job = jobRepository.findById(jobId).orElse(null);
            if (job != null) {
                job.markFailed(e.getMessage());
                jobRepository.save(job);

                // Notify failure via WebSocket
                notificationService.notifyJobStatusChange(job);
            } else {
                jobRepository.markJobFailed(jobId, e.getMessage());
            }
        }

        return CompletableFuture.completedFuture(null);
    }

    private boolean checkJobCancelled(String jobId) {
        return jobRepository.findById(jobId)
                .map(j -> j.getStatus() == JobStatusEnum.CANCELLED)
                .orElse(false);
    }

    private void simulateProgressUpdates(String jobId) {
        try {
            for (int progress = 10; progress <= 90; progress += 10) {
                // Check if job was cancelled
                if (checkJobCancelled(jobId)) {
                    break;
                }

                // Update progress
                JobEntity job = jobRepository.findById(jobId)
                        .orElseThrow(() -> new JobNotFoundException(jobId));

                // Send progress update via WebSocket
                notificationService.notifyJobStatusChange(job);

                // Simulate processing time
                Thread.sleep(500);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.error("Error during progress updates: {}", jobId, e);
        }
    }
}

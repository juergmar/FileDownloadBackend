package de.ma.download.controller;

import de.ma.download.dto.GenerateFileRequest;
import de.ma.download.dto.JobDTO;
import de.ma.download.dto.PagedJobResponse;
import de.ma.download.model.FileType;
import de.ma.download.service.FileDownloadService;
import de.ma.download.service.JobManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
@Tag(name = "File Download API", description = "Endpoints for managing file generation and download")
public class FileDownloadController {

    private final FileDownloadService fileDownloadService;
    private final JobManagementService jobManagementService;

    @Operation(summary = "Generate a new file")
    @PostMapping("/generate")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, String>> generateFile(@RequestBody @Valid GenerateFileRequest request) {
        String jobId = jobManagementService.initiateJob(request.getFileType(), request.getParameters());
        return ResponseEntity.accepted().body(Map.of("jobId", jobId));
    }

    @Operation(summary = "Download generated file")
    @GetMapping("/download/{jobId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<byte[]> downloadFile(@PathVariable String jobId) {
        JobDTO job = fileDownloadService.prepareForDownload(jobId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(job.getContentType()));
        headers.setContentDispositionFormData("attachment", job.getFileName());
        headers.setContentLength(job.getFileSize());

        return ResponseEntity.ok()
                .headers(headers)
                .body(fileDownloadService.getFileData(jobId));
    }

    @Operation(summary = "Cancel job")
    @PostMapping("/cancel/{jobId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Boolean>> cancelJob(@PathVariable String jobId) {
        boolean cancelled = jobManagementService.cancelJob(jobId);
        return ResponseEntity.ok().body(Map.of("cancelled", cancelled));
    }

    @Operation(summary = "Get recent jobs with pagination")
    @GetMapping("/recent")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PagedJobResponse> getRecentJobs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(jobManagementService.getRecentJobs(page, size));
    }

    @Operation(summary = "Retry failed job")
    @PostMapping("/retry/{jobId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, String>> retryJob(@PathVariable String jobId) {
        String newJobId = jobManagementService.retryJob(jobId);
        return ResponseEntity.accepted().body(Map.of("jobId", newJobId));
    }
}

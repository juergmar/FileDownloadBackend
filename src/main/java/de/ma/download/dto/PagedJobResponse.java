package de.ma.download.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Paginated response for job listings")
public class PagedJobResponse {

    @Schema(description = "List of jobs for the current page")
    private List<JobDTO> jobs;

    @Schema(description = "Total number of items across all pages")
    private long totalItems;

    @Schema(description = "Total number of pages")
    private int totalPages;

    @Schema(description = "Current page number (zero-based)")
    private int currentPage;

    @Schema(description = "Number of items per page")
    private int pageSize;
}

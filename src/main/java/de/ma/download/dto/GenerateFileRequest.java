package de.ma.download.dto;

import de.ma.download.model.FileType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request object for file generation")
public class GenerateFileRequest {

    @NotNull(message = "File type is required")
    @Schema(description = "Type of report to generate")
    private FileType fileType;

    @Schema(description = "Parameters for report generation")
    private Map<String, Object> parameters;
}

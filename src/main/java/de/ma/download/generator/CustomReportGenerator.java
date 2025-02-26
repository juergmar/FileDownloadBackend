package de.ma.download.generator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.ma.download.dto.CustomReportRequest;
import de.ma.download.dto.GeneratedFile;
import de.ma.download.model.FileType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class CustomReportGenerator implements FileGenerator<CustomReportRequest> {

    private final ObjectMapper objectMapper;

    @Override
    public GeneratedFile generate(String jobId, String userId, CustomReportRequest request) {
        log.info("Generating Custom Report for job: {}, user: {}", jobId, userId);

        try {
            String reportName = request != null && request.getReportName() != null ?
                    request.getReportName() : "Custom";

            ObjectNode rootNode = objectMapper.createObjectNode();
            addReportMetadata(rootNode, reportName + " Report", userId);

            // Custom Report data
            ObjectNode dataNode = rootNode.putObject("data");
            dataNode.put("message", "Custom report generated successfully");
            dataNode.put("reportName", reportName);
            dataNode.put("customField", "This is a custom field");

            byte[] fileData = objectMapper.writeValueAsBytes(rootNode);

            return GeneratedFile.builder()
                    .fileName(reportName.toLowerCase().replace(" ", "-") + "-report-" + System.currentTimeMillis() + ".json")
                    .contentType("application/json")
                    .fileData(fileData)
                    .build();

        } catch (Exception e) {
            log.error("Error generating custom report", e);
            throw new RuntimeException("Failed to generate custom report: " + e.getMessage(), e);
        }
    }

    private void addReportMetadata(ObjectNode rootNode, String reportName, String userId) {
        ObjectNode metadataNode = rootNode.putObject("metadata");
        metadataNode.put("reportName", reportName);
        metadataNode.put("generatedFor", userId);
        metadataNode.put("generatedAt", LocalDateTime.now()
                .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        metadataNode.put("reportId", UUID.randomUUID().toString());
    }

    @Override
    public FileType getSupportedType() {
        return FileType.CUSTOM_REPORT;
    }

    @Override
    public Class<CustomReportRequest> getRequestType() {
        return CustomReportRequest.class;
    }
}

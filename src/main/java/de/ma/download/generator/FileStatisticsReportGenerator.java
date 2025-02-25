package de.ma.download.generator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.ma.download.dto.GeneratedFile;
import de.ma.download.model.FileType;
import de.ma.download.service.UserContextService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class FileStatisticsReportGenerator implements FileGenerator {

    private final ObjectMapper objectMapper;
    private final UserContextService userContextService;

    @Override
    public GeneratedFile generate(String jobId, Object parameters) {
        log.info("Generating File Statistics Report for job: {}", jobId);

        try {
            String userId = userContextService.getCurrentUserId();

            ObjectNode rootNode = objectMapper.createObjectNode();
            addReportMetadata(rootNode, "File Statistics Report", userId);

            ObjectNode dataNode = rootNode.putObject("data");

            ObjectNode countsByTypeNode = dataNode.putObject("fileCountsByType");
            countsByTypeNode.put("PDF", 34);
            countsByTypeNode.put("CSV", 28);
            countsByTypeNode.put("EXCEL", 16);
            countsByTypeNode.put("JSON", 9);

            ObjectNode storageNode = dataNode.putObject("storageUsage");
            storageNode.put("total", "256MB");
            storageNode.put("used", "98MB");
            storageNode.put("available", "158MB");

            ArrayNode fileSizesNode = dataNode.putArray("recentFileSizes");
            for (int i = 0; i < 5; i++) {
                ObjectNode fileSize = fileSizesNode.addObject();
                fileSize.put("fileType", "TYPE_" + i);
                fileSize.put("avgSize", (i + 1) * 100 + "KB");
                fileSize.put("maxSize", (i + 1) * 300 + "KB");
            }

            byte[] fileData = objectMapper.writeValueAsBytes(rootNode);

            return GeneratedFile.builder()
                    .fileName("file-statistics-report-" + System.currentTimeMillis() + ".json")
                    .contentType("application/json")
                    .fileData(fileData)
                    .build();

        } catch (Exception e) {
            log.error("Error generating file statistics report", e);
            throw new RuntimeException("Failed to generate file statistics report: " + e.getMessage(), e);
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
        return FileType.FILE_STATISTICS_REPORT;
    }
}

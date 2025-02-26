package de.ma.download.generator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.ma.download.dto.FileStatisticsReportRequest;
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
public class FileStatisticsReportGenerator implements FileGenerator<FileStatisticsReportRequest> {

    private final ObjectMapper objectMapper;

    @Override
    public GeneratedFile generate(String jobId, String userId, FileStatisticsReportRequest request) {
        log.info("Generating File Statistics Report for job: {}, user: {}", jobId, userId);

        try {
            boolean includeHistoricalData = request != null &&
                    request.getIncludeHistoricalData() != null &&
                    request.getIncludeHistoricalData();

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

            if (includeHistoricalData) {
                ArrayNode historicalDataNode = dataNode.putArray("historicalData");
                for (int i = 1; i <= 12; i++) {
                    ObjectNode monthData = historicalDataNode.addObject();
                    monthData.put("month", "2024-" + String.format("%02d", i));
                    monthData.put("filesCreated", 80 + (int)(Math.random() * 40));
                    monthData.put("storageUsed", 90 + (int)(Math.random() * 20) + "MB");
                }
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

    @Override
    public Class<FileStatisticsReportRequest> getRequestType() {
        return FileStatisticsReportRequest.class;
    }
}

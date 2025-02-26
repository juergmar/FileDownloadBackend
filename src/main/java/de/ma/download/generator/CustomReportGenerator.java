package de.ma.download.generator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.ma.download.dto.GeneratedFile;
import de.ma.download.model.FileType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class CustomReportGenerator implements FileGenerator {

    private final ObjectMapper objectMapper;

    @Override
    public GeneratedFile generate(String jobId, String userId, Object parameters) {
        log.info("Generating Custom Report for job: {}, user: {}", jobId, userId);

        try {
            Map<String, Object> reportParams = parameters instanceof Map ?
                    (Map<String, Object>) parameters :
                    Map.of();

            String reportName = reportParams.getOrDefault("reportName", "Custom").toString();

            ObjectNode rootNode = objectMapper.createObjectNode();
            addReportMetadata(rootNode, reportName + " Report", userId);

            ObjectNode paramsNode = rootNode.putObject("parameters");
            reportParams.forEach((key, value) -> {
                switch (value) {
                    case Integer i -> paramsNode.put(key, i);
                    case Long l -> paramsNode.put(key, l);
                    case Double v -> paramsNode.put(key, v);
                    case Float v -> paramsNode.put(key, v);
                    case Boolean b -> paramsNode.put(key, b);
                    case null, default -> paramsNode.put(key, String.valueOf(value));
                }
            });

            rootNode.putObject("data")
                    .put("message", "Custom report generated successfully");

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
}

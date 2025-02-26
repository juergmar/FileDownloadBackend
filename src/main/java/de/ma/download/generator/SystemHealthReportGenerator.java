package de.ma.download.generator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.ma.download.dto.GeneratedFile;
import de.ma.download.dto.SystemHealthReportRequest;
import de.ma.download.model.FileType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class SystemHealthReportGenerator implements FileGenerator<SystemHealthReportRequest> {

    private final ObjectMapper objectMapper;

    @Override
    public GeneratedFile generate(String jobId, String userId, SystemHealthReportRequest request) {
        log.info("Generating System Health Report for job: {}, user: {}", jobId, userId);

        try {
            boolean includeDetailedMetrics = request != null &&
                    request.getIncludeDetailedMetrics() != null &&
                    request.getIncludeDetailedMetrics();

            ObjectNode rootNode = objectMapper.createObjectNode();
            addReportMetadata(rootNode, "System Health Report", userId);

            ObjectNode dataNode = rootNode.putObject("data");
            dataNode.put("cpuUsage", "32%");
            dataNode.put("memoryUsage", "64%");
            dataNode.put("diskUsage", "48%");
            dataNode.put("activeJobs", 12);
            dataNode.put("failedJobs", 3);
            dataNode.put("completedJobs", 127);

            ArrayNode servicesNode = dataNode.putArray("services");
            String[] serviceNames = {"Database", "FileStorage", "Authentication", "JobProcessor"};
            String[] serviceStatuses = {"Healthy", "Healthy", "Healthy", "Degraded"};

            for (int i = 0; i < serviceNames.length; i++) {
                ObjectNode service = servicesNode.addObject();
                service.put("name", serviceNames[i]);
                service.put("status", serviceStatuses[i]);
                service.put("lastChecked", Instant.now().toString());

                if (includeDetailedMetrics) {
                    service.put("responseTime", Math.random() * 100);
                    service.put("availability", "99.95%");
                    service.put("errorRate", "0.05%");
                }
            }

            byte[] fileData = objectMapper.writeValueAsBytes(rootNode);

            return GeneratedFile.builder()
                    .fileName("system-health-report-" + System.currentTimeMillis() + ".json")
                    .contentType("application/json")
                    .fileData(fileData)
                    .build();

        } catch (Exception e) {
            log.error("Error generating system health report", e);
            throw new RuntimeException("Failed to generate system health report: " + e.getMessage(), e);
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
        return FileType.SYSTEM_HEALTH_REPORT;
    }

    @Override
    public Class<SystemHealthReportRequest> getRequestType() {
        return SystemHealthReportRequest.class;
    }
}

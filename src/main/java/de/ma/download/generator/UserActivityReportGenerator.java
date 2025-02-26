package de.ma.download.generator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.ma.download.dto.GeneratedFile;
import de.ma.download.model.FileType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserActivityReportGenerator implements FileGenerator {

    private final ObjectMapper objectMapper;

    @Override
    public GeneratedFile generate(String jobId, String userId, Object parameters) {
        log.info("Generating User Activity Report for job: {}, user: {}", jobId, userId);

        try {
            Map<String, Object> reportParams = parameters instanceof Map ?
                    (Map<String, Object>) parameters :
                    Map.of();

            String startDate = reportParams.getOrDefault("startDate", "30").toString();
            int days = Integer.parseInt(startDate);

            ObjectNode rootNode = objectMapper.createObjectNode();
            addReportMetadata(rootNode, "User Activity Report", userId);

            ObjectNode dataNode = rootNode.putObject("data");
            dataNode.put("timespan", days + " days");

            ArrayNode activitiesNode = dataNode.putArray("activities");
            for (int i = 0; i < 10; i++) {
                ObjectNode activity = activitiesNode.addObject();
                activity.put("id", UUID.randomUUID().toString());
                activity.put("type", "FILE_DOWNLOAD");
                activity.put("timestamp", Instant.now().minusSeconds(i * 86400L).toString());
                activity.put("details", "Downloaded file #" + (i + 1));
            }

            byte[] fileData = objectMapper.writeValueAsBytes(rootNode);

            return GeneratedFile.builder()
                    .fileName("user-activity-report-" + System.currentTimeMillis() + ".json")
                    .contentType("application/json")
                    .fileData(fileData)
                    .build();

        } catch (Exception e) {
            log.error("Error generating user activity report", e);
            throw new RuntimeException("Failed to generate user activity report: " + e.getMessage(), e);
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
        return FileType.USER_ACTIVITY_REPORT;
    }
}

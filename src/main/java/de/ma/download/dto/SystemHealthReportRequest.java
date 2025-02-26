package de.ma.download.dto;

import de.ma.download.model.FileType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;


@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder
public class SystemHealthReportRequest extends ReportRequest {
    private Boolean includeDetailedMetrics;

    public SystemHealthReportRequest() {
        super(FileType.SYSTEM_HEALTH_REPORT);
        this.includeDetailedMetrics = false; // Default value
    }

    public SystemHealthReportRequest(Boolean includeDetailedMetrics) {
        super(FileType.SYSTEM_HEALTH_REPORT);
        this.includeDetailedMetrics = includeDetailedMetrics;
    }
}

package de.ma.download.dto;

import de.ma.download.model.FileType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;


@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder
public class CustomReportRequest extends ReportRequest {
    private String reportName;

    public CustomReportRequest() {
        super(FileType.CUSTOM_REPORT);
        this.reportName = "Custom"; // Default value
    }

    public CustomReportRequest(String reportName) {
        super(FileType.CUSTOM_REPORT);
        this.reportName = reportName;
    }
}

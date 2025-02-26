package de.ma.download.dto;

import de.ma.download.model.FileType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;


@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder
public class FileStatisticsReportRequest extends ReportRequest {
    private Boolean includeHistoricalData;

    public FileStatisticsReportRequest() {
        super(FileType.FILE_STATISTICS_REPORT);
        this.includeHistoricalData = false; // Default value
    }

    public FileStatisticsReportRequest(Boolean includeHistoricalData) {
        super(FileType.FILE_STATISTICS_REPORT);
        this.includeHistoricalData = includeHistoricalData;
    }
}

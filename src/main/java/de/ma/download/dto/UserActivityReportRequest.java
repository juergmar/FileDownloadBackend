package de.ma.download.dto;

import de.ma.download.model.FileType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;


@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder
public class UserActivityReportRequest extends ReportRequest {
    private Integer startDate;

    public UserActivityReportRequest() {
        super(FileType.USER_ACTIVITY_REPORT);
        this.startDate = 30;
    }

    public UserActivityReportRequest(Integer startDate) {
        super(FileType.USER_ACTIVITY_REPORT);
        this.startDate = startDate;
    }
}

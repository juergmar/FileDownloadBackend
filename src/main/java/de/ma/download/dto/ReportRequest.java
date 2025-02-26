package de.ma.download.dto;


import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import de.ma.download.model.FileType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        property = "type",
        visible = true
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = UserActivityReportRequest.class, name = "USER_ACTIVITY_REPORT"),
        @JsonSubTypes.Type(value = SystemHealthReportRequest.class, name = "SYSTEM_HEALTH_REPORT"),
        @JsonSubTypes.Type(value = FileStatisticsReportRequest.class, name = "FILE_STATISTICS_REPORT"),
        @JsonSubTypes.Type(value = CustomReportRequest.class, name = "CUSTOM_REPORT")
})
public abstract class ReportRequest {
    private FileType type;

    // Return the file type from the concrete subclass
    public FileType getFileType() {
        return type;
    }
}

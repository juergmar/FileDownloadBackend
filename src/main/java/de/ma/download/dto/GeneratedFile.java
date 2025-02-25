package de.ma.download.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GeneratedFile {
    private String fileName;
    private String contentType;
    private byte[] fileData;
}

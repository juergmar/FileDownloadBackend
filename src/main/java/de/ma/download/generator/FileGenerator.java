package de.ma.download.generator;

import de.ma.download.dto.GeneratedFile;
import de.ma.download.model.FileType;

public interface FileGenerator {
    GeneratedFile generate(String jobId, String userId, Object parameters);
    FileType getSupportedType();
}

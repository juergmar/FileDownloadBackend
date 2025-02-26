package de.ma.download.generator;

import de.ma.download.dto.GeneratedFile;
import de.ma.download.dto.ReportRequest;
import de.ma.download.model.FileType;

/**
 * Type-safe file generator interface that works with specific report request types
 * @param <T> The specific ReportRequest subclass this generator handles
 */
public interface FileGenerator<T extends ReportRequest> {

    /**
     * Generate a file based on the given report request
     * @param jobId The job ID
     * @param userId The user ID
     * @param request The typed request parameters
     * @return The generated file
     */
    GeneratedFile generate(String jobId, String userId, T request);

    /**
     * Get the file type this generator supports
     * @return The file type
     */
    FileType getSupportedType();

    /**
     * Get the request class this generator handles
     * @return The class of the request type
     */
    Class<T> getRequestType();
}

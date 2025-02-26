package de.ma.download.generator;

import de.ma.download.dto.ReportRequest;
import de.ma.download.model.FileType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Registry for storing and retrieving file generators based on file types
 * or request types.
 */
@Slf4j
@Component
public class GeneratorRegistry {

    private final Map<FileType, FileGenerator<?>> generatorsByType = new HashMap<>();
    private final Map<Class<? extends ReportRequest>, FileGenerator<?>> generatorsByClass = new HashMap<>();

    public GeneratorRegistry(Set<FileGenerator<?>> generators) {
        for (FileGenerator<?> generator : generators) {
            registerGenerator(generator);
        }
        log.info("Registered {} file generators", generators.size());
    }

    private void registerGenerator(FileGenerator<?> generator) {
        generatorsByType.put(generator.getSupportedType(), generator);
        generatorsByClass.put(generator.getRequestType(), generator);
    }

    /**
     * Get a generator for the specified file type
     */
    @SuppressWarnings("unchecked")
    public <T extends ReportRequest> FileGenerator<T> getGenerator(FileType fileType) {
        FileGenerator<?> generator = generatorsByType.get(fileType);
        if (generator == null) {
            throw new IllegalArgumentException("No generator registered for file type: " + fileType);
        }
        return (FileGenerator<T>) generator;
    }

    /**
     * Get a generator that can handle the specified request class
     */
    @SuppressWarnings("unchecked")
    public <T extends ReportRequest> FileGenerator<T> getGenerator(Class<T> requestClass) {
        FileGenerator<?> generator = generatorsByClass.get(requestClass);
        if (generator == null) {
            throw new IllegalArgumentException("No generator registered for request class: " + requestClass.getName());
        }
        return (FileGenerator<T>) generator;
    }

    /**
     * Get a generator for a specific report request instance
     */
    @SuppressWarnings("unchecked")
    public <T extends ReportRequest> FileGenerator<T> getGenerator(T request) {
        if (request == null) {
            throw new IllegalArgumentException("Request cannot be null");
        }

        FileType fileType = request.getFileType();
        FileGenerator<?> generator = generatorsByType.get(fileType);

        if (generator == null) {
            throw new IllegalArgumentException("No generator registered for file type: " + fileType);
        }

        return (FileGenerator<T>) generator;
    }
}

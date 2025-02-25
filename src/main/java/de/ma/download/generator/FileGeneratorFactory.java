package de.ma.download.generator;

import de.ma.download.model.FileType;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Component
public class FileGeneratorFactory {
    private final Map<FileType, FileGenerator> generators = new HashMap<>();

    public FileGeneratorFactory(Set<FileGenerator> availableGenerators) {
        for (FileGenerator generator : availableGenerators) {
            generators.put(generator.getSupportedType(), generator);
        }
    }

    public FileGenerator getGenerator(FileType fileType) {
        FileGenerator generator = generators.get(fileType);

        if (generator == null) {
            throw new IllegalArgumentException("No generator available for file type: " + fileType);
        }

        return generator;
    }
}

package de.ma.download.mapper;

import de.ma.download.dto.JobDTO;
import de.ma.download.dto.JobStatusDTO;
import de.ma.download.entity.JobEntity;
import org.mapstruct.*;

@Mapper(componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface JobMapper {

    @Mapping(target = "fileDataAvailable", expression = "java(entity.getFileData() != null && entity.getFileData().length > 0)")
    JobDTO toJobDTO(JobEntity entity);

    JobStatusDTO toJobStatusDTO(JobEntity entity);
}

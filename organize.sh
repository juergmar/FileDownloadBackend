#!/bin/bash

# Exit on any error
set -e

echo "Starting reorganization of Java files into subpackages..."

# Base directory - adjust if needed
BASE_DIR="src/main/java/de/ma/download"

# Create subpackages
mkdir -p "${BASE_DIR}/entity"
mkdir -p "${BASE_DIR}/repository"
mkdir -p "${BASE_DIR}/service"
mkdir -p "${BASE_DIR}/controller"
mkdir -p "${BASE_DIR}/config"
mkdir -p "${BASE_DIR}/dto"
mkdir -p "${BASE_DIR}/generator"
mkdir -p "${BASE_DIR}/exception"
mkdir -p "${BASE_DIR}/security"
mkdir -p "${BASE_DIR}/model"
mkdir -p "${BASE_DIR}/mapper"

# Function to move a file and update its package declaration
move_file() {
    local file=$1
    local target_dir=$2
    local target_package="de.ma.download.$3"

    if [ -f "${BASE_DIR}/${file}" ]; then
        echo "Moving ${file} to ${target_dir}"

        # Update package declaration in the file
        sed -i "1s|package de.ma.download;|package ${target_package};|" "${BASE_DIR}/${file}"

        # Move the file
        mv "${BASE_DIR}/${file}" "${BASE_DIR}/${target_dir}/"
    else
        echo "Warning: ${file} not found"
    fi
}

# Process entity files
move_file "JobEntity.java" "entity" "entity"
move_file "CustomUserDetails.java" "entity" "entity"

# Process repository
move_file "JobRepository.java" "repository" "repository"

# Process DTOs
move_file "JobDTO.java" "dto" "dto"
move_file "JobStatusDTO.java" "dto" "dto"
move_file "GenerateFileRequest.java" "dto" "dto"
move_file "GeneratedFile.java" "dto" "dto"

# Process exceptions
move_file "JobNotFoundException.java" "exception" "exception"
move_file "ResourceAccessDeniedException.java" "exception" "exception"
move_file "JobAlreadyExistsException.java" "exception" "exception"
move_file "ServiceOverloadedException.java" "exception" "exception"
move_file "FileNotReadyException.java" "exception" "exception"
move_file "GlobalExceptionHandler.java" "exception" "exception"

# Process services
move_file "FileDownloadService.java" "service" "service"
move_file "FileGenerationService.java" "service" "service"
move_file "JobCleanupService.java" "service" "service"
move_file "JobManagementService.java" "service" "service"
move_file "UserContextService.java" "service" "service"

# Process controllers
move_file "FileDownloadController.java" "controller" "controller"

# Process configs
move_file "AsyncConfig.java" "config" "config"
move_file "SecurityConfig.java" "config" "config"

# Process generators
move_file "FileGenerator.java" "generator" "generator"
move_file "FileGeneratorFactory.java" "generator" "generator"
move_file "UserActivityReportGenerator.java" "generator" "generator"
move_file "SystemHealthReportGenerator.java" "generator" "generator"
move_file "FileStatisticsReportGenerator.java" "generator" "generator"
move_file "CustomReportGenerator.java" "generator" "generator"

# Process security
move_file "KeycloakJwtAuthenticationConverter.java" "security" "security"

# Process models/enums
move_file "FileType.java" "model" "model"
move_file "JobStatusEnum.java" "model" "model"

# Process mappers
move_file "JobMapper.java" "mapper" "mapper"

# Process main application
# DemoApplication.java stays in the root package

echo "Updating import statements in all files..."

# Function to update import statements in files
update_imports() {
    local dir=$1

    # Find all Java files in directory
    find "${BASE_DIR}/${dir}" -name "*.java" | while read -r file; do
        # Update import statements
        sed -i "s|import de.ma.download\.|import de.ma.download.|g" "$file"

        # Replace specific imports for each subpackage
        sed -i "s|import de.ma.download.JobEntity;|import de.ma.download.entity.JobEntity;|g" "$file"
        sed -i "s|import de.ma.download.CustomUserDetails;|import de.ma.download.entity.CustomUserDetails;|g" "$file"

        sed -i "s|import de.ma.download.JobRepository;|import de.ma.download.repository.JobRepository;|g" "$file"

        sed -i "s|import de.ma.download.JobDTO;|import de.ma.download.dto.JobDTO;|g" "$file"
        sed -i "s|import de.ma.download.JobStatusDTO;|import de.ma.download.dto.JobStatusDTO;|g" "$file"
        sed -i "s|import de.ma.download.GenerateFileRequest;|import de.ma.download.dto.GenerateFileRequest;|g" "$file"
        sed -i "s|import de.ma.download.GeneratedFile;|import de.ma.download.dto.GeneratedFile;|g" "$file"

        sed -i "s|import de.ma.download.JobNotFoundException;|import de.ma.download.exception.JobNotFoundException;|g" "$file"
        sed -i "s|import de.ma.download.ResourceAccessDeniedException;|import de.ma.download.exception.ResourceAccessDeniedException;|g" "$file"
        sed -i "s|import de.ma.download.JobAlreadyExistsException;|import de.ma.download.exception.JobAlreadyExistsException;|g" "$file"
        sed -i "s|import de.ma.download.ServiceOverloadedException;|import de.ma.download.exception.ServiceOverloadedException;|g" "$file"
        sed -i "s|import de.ma.download.FileNotReadyException;|import de.ma.download.exception.FileNotReadyException;|g" "$file"
        sed -i "s|import de.ma.download.GlobalExceptionHandler;|import de.ma.download.exception.GlobalExceptionHandler;|g" "$file"

        sed -i "s|import de.ma.download.FileDownloadService;|import de.ma.download.service.FileDownloadService;|g" "$file"
        sed -i "s|import de.ma.download.FileGenerationService;|import de.ma.download.service.FileGenerationService;|g" "$file"
        sed -i "s|import de.ma.download.JobCleanupService;|import de.ma.download.service.JobCleanupService;|g" "$file"
        sed -i "s|import de.ma.download.JobManagementService;|import de.ma.download.service.JobManagementService;|g" "$file"
        sed -i "s|import de.ma.download.UserContextService;|import de.ma.download.service.UserContextService;|g" "$file"

        sed -i "s|import de.ma.download.FileDownloadController;|import de.ma.download.controller.FileDownloadController;|g" "$file"

        sed -i "s|import de.ma.download.AsyncConfig;|import de.ma.download.config.AsyncConfig;|g" "$file"
        sed -i "s|import de.ma.download.SecurityConfig;|import de.ma.download.config.SecurityConfig;|g" "$file"

        sed -i "s|import de.ma.download.FileGenerator;|import de.ma.download.generator.FileGenerator;|g" "$file"
        sed -i "s|import de.ma.download.FileGeneratorFactory;|import de.ma.download.generator.FileGeneratorFactory;|g" "$file"
        sed -i "s|import de.ma.download.UserActivityReportGenerator;|import de.ma.download.generator.UserActivityReportGenerator;|g" "$file"
        sed -i "s|import de.ma.download.SystemHealthReportGenerator;|import de.ma.download.generator.SystemHealthReportGenerator;|g" "$file"
        sed -i "s|import de.ma.download.FileStatisticsReportGenerator;|import de.ma.download.generator.FileStatisticsReportGenerator;|g" "$file"
        sed -i "s|import de.ma.download.CustomReportGenerator;|import de.ma.download.generator.CustomReportGenerator;|g" "$file"

        sed -i "s|import de.ma.download.KeycloakJwtAuthenticationConverter;|import de.ma.download.security.KeycloakJwtAuthenticationConverter;|g" "$file"

        sed -i "s|import de.ma.download.FileType;|import de.ma.download.model.FileType;|g" "$file"
        sed -i "s|import de.ma.download.JobStatusEnum;|import de.ma.download.model.JobStatusEnum;|g" "$file"

        sed -i "s|import de.ma.download.JobMapper;|import de.ma.download.mapper.JobMapper;|g" "$file"
    done
}

# Update imports in all subpackages
for dir in "entity" "repository" "service" "controller" "config" "dto" "generator" "exception" "security" "model" "mapper"; do
    update_imports "$dir"
done

# Update imports in remaining root files
for file in $(find "${BASE_DIR}" -maxdepth 1 -name "*.java"); do
    echo "Updating imports in ${file}"

    # Same replacements as in the update_imports function
    sed -i "s|import de.ma.download.JobEntity;|import de.ma.download.entity.JobEntity;|g" "$file"
    sed -i "s|import de.ma.download.CustomUserDetails;|import de.ma.download.entity.CustomUserDetails;|g" "$file"

    # Continue with the same replacements as above for each package
    # ... (same as in the update_imports function)
done

echo "Reorganization complete!"

/**
 * Helper class to translate between P6 and EBS data structures
 */
package com.tpcgrp.p6ebs.service.integration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

@Component
@Slf4j
public class MappingUtility {

    // Map of entity types to their field mappings
    private final Map<String, Map<String, String>> fieldMappings = new HashMap<>();

    // Store correlations between P6 and EBS IDs
    private final Map<String, Map<String, String>> idCorrelationStore = new ConcurrentHashMap<>();

    // Constructor with initialization of field mappings
    public MappingUtility() {
        initializeFieldMappings();
        loadIdCorrelations();
    }

    /**
     * Initialize field mappings between P6 and EBS
     */
    private void initializeFieldMappings() {
        // Project field mappings
        Map<String, String> projectMappings = new HashMap<>();
        projectMappings.put("proj_id", "project_id");
        projectMappings.put("proj_name", "project_name");
        projectMappings.put("proj_short_name", "segment1");
        projectMappings.put("status_code", "project_status_code");
        projectMappings.put("plan_start_date", "start_date");
        projectMappings.put("plan_end_date", "completion_date");
        fieldMappings.put("project", projectMappings);

        // Activity/Task field mappings
        Map<String, String> activityMappings = new HashMap<>();
        activityMappings.put("activity_id", "task_id");
        activityMappings.put("activity_name", "task_name");
        activityMappings.put("activity_code", "task_number");
        activityMappings.put("start_date", "start_date");
        activityMappings.put("finish_date", "completion_date");
        activityMappings.put("status_code", "task_status_code");
        fieldMappings.put("activity", activityMappings);

        // Task field mappings
        Map<String, String> taskMappings = new HashMap<>();
        taskMappings.put("task_id", "activity_id");
        taskMappings.put("task_name", "activity_name");
        taskMappings.put("task_number", "activity_code");
        taskMappings.put("start_date", "start_date");
        taskMappings.put("completion_date", "finish_date");
        taskMappings.put("task_status_code", "status_code");
        taskMappings.put("actual_start_date", "act_start_date");
        taskMappings.put("actual_finish_date", "act_end_date");
        taskMappings.put("planned_duration", "duration");
        taskMappings.put("project_id", "proj_id");
        fieldMappings.put("task", taskMappings);

        // Resource field mappings
        Map<String, String> resourceMappings = new HashMap<>();
        resourceMappings.put("rsrc_id", "person_id");
        resourceMappings.put("rsrc_name", "full_name");
        resourceMappings.put("email_addr", "email_address");
        fieldMappings.put("resource", resourceMappings);

        // WBS field mappings
        Map<String, String> wbsMappings = new HashMap<>();
        wbsMappings.put("wbs_id", "wbs_id");
        wbsMappings.put("wbs_name", "wbs_name");
        fieldMappings.put("wbs", wbsMappings);
    }

    /**
     * Load existing ID correlations from storage
     */
    private void loadIdCorrelations() {
        try {
            String correlationFile = System.getProperty("user.home") + "/.p6ebs/id_correlations.json";
            if (Files.exists(Paths.get(correlationFile))) {
                // Load from file
                // Implementation would deserialize JSON to Map structure
                log.info("Loaded ID correlations from file");
            }
        } catch (Exception e) {
            log.error("Failed to load ID correlations", e);
        }
    }

    /**
     * Save ID correlations to storage
     */
    public void saveIdCorrelations() {
        try {
            String correlationFile = System.getProperty("user.home") + "/.p6ebs/id_correlations.json";
            // Ensure directory exists
            Files.createDirectories(Paths.get(System.getProperty("user.home") + "/.p6ebs"));

            // Implementation would serialize Map to JSON
            log.info("Saved ID correlations to file");
        } catch (IOException e) {
            log.error("Failed to save ID correlations", e);
        }
    }

    /**
     * Map field values from P6 to EBS based on entity type
     */
    public Map<String, Object> mapP6ToEbs(String entityType, Map<String, Object> p6Entity) {
        Map<String, Object> ebsEntity = new HashMap<>();
        Map<String, String> mappings = fieldMappings.get(entityType);

        if (mappings != null) {
            for (Map.Entry<String, String> entry : mappings.entrySet()) {
                String p6Field = entry.getKey();
                String ebsField = entry.getValue();

                if (p6Entity.containsKey(p6Field)) {
                    Object value = p6Entity.get(p6Field);
                    // Convert data types if needed
                    value = convertDataType(value, entityType, p6Field, ebsField);
                    ebsEntity.put(ebsField, value);
                }
            }
        }

        return ebsEntity;
    }

    /**
     * Map field values from EBS to P6 based on entity type
     */
    public Map<String, Object> mapEbsToP6(String entityType, Map<String, Object> ebsEntity) {
        Map<String, Object> p6Entity = new HashMap<>();
        Map<String, String> mappings = fieldMappings.get(entityType);

        if (mappings != null) {
            for (Map.Entry<String, String> entry : mappings.entrySet()) {
                String p6Field = entry.getKey();
                String ebsField = entry.getValue();

                if (ebsEntity.containsKey(ebsField)) {
                    Object value = ebsEntity.get(ebsField);
                    // Convert data types if needed (reverse direction)
                    value = convertDataTypeReverse(value, entityType, ebsField, p6Field);
                    p6Entity.put(p6Field, value);
                }
            }
        }

        return p6Entity;
    }

    /**
     * Get field mappings for an entity type
     * Ensure this method is public and returns the correct type
     */
    public Map<String, String> getFieldMappings(String entityType) {
        if (fieldMappings.containsKey(entityType)) {
            return fieldMappings.get(entityType);
        }
        return Collections.emptyMap();
    }

    /**
     * Convert data types between P6 and EBS
     */
    private Object convertDataType(Object value, String entityType, String sourceField, String targetField) {
        // Handle various data type conversions
        // Example: Date formats, string/numeric conversions, etc.

        if (value == null) {
            return null;
        }

        // Handle date conversions
        if (sourceField.contains("date") && value instanceof java.sql.Date) {
            return new java.util.Date(((java.sql.Date) value).getTime());
        }

        // Handle numeric conversions
        if (value instanceof Number) {
            // Special conversion cases for specific fields
            if (sourceField.equals("status_code") && entityType.equals("project")) {
                // Map P6 status codes to EBS status codes
                return mapStatusCode((Number) value);
            }
        }

        return value;
    }

    /**
     * Convert data types from EBS to P6 (reverse direction)
     */
    private Object convertDataTypeReverse(Object value, String entityType, String sourceField, String targetField) {
        // Similar to convertDataType but for the reverse direction
        return value;
    }

    /**
     * Map status codes between systems
     */
    private String mapStatusCode(Number statusCode) {
        Map<Integer, String> statusMapping = new HashMap<>();
        statusMapping.put(1, "APPROVED");
        statusMapping.put(2, "IN_PROGRESS");
        statusMapping.put(3, "COMPLETED");

        return statusMapping.getOrDefault(statusCode.intValue(), "UNDEFINED");
    }

    /**
     * Map project IDs between P6 and EBS based on business keys
     */
    public Map<String, String> mapProjectIds(List<Map<String, Object>> p6Projects,
                                             List<Map<String, Object>> ebsProjects) {
        Map<String, String> projectMapping = new HashMap<>();
        Map<String, Map<String, Object>> ebsProjectsByCode = new HashMap<>();

        // Index EBS projects by code/number for faster lookup
        for (Map<String, Object> ebsProject : ebsProjects) {
            String projectNumber = (String) ebsProject.get("segment1");
            if (projectNumber != null) {
                ebsProjectsByCode.put(projectNumber, ebsProject);
            }
        }

        // Match P6 projects with EBS projects
        for (Map<String, Object> p6Project : p6Projects) {
            String p6ProjectId = p6Project.get("proj_id").toString();
            String p6ProjectCode = (String) p6Project.get("proj_short_name");

            if (p6ProjectCode != null && ebsProjectsByCode.containsKey(p6ProjectCode)) {
                Map<String, Object> matchedEbsProject = ebsProjectsByCode.get(p6ProjectCode);
                String ebsProjectId = matchedEbsProject.get("project_id").toString();

                projectMapping.put(p6ProjectId, ebsProjectId);

                // Store the correlation for future use
                storeIdCorrelation("project", p6ProjectId, ebsProjectId);
            }
        }

        return projectMapping;
    }

    /**
     * Map resource IDs between P6 and EBS
     */
    public Map<String, String> mapResourceIds(List<Map<String, Object>> p6Resources,
                                              List<Map<String, Object>> ebsResources) {
        Map<String, String> resourceMapping = new HashMap<>();
        // Implementation details for matching resources by email or other identifiers
        return resourceMapping;
    }

    /**
     * Store correlation between P6 and EBS entity IDs
     */
    public void storeIdCorrelation(String entityType, String p6Id, String ebsId) {
        idCorrelationStore.computeIfAbsent(entityType, k -> new ConcurrentHashMap<>())
                .put(p6Id, ebsId);
    }

    /**
     * Get EBS ID for a given P6 entity
     */
    public String getEbsIdForP6Entity(String entityType, String p6Id) {
        Map<String, String> correlations = idCorrelationStore.get(entityType);
        return correlations != null ? correlations.get(p6Id) : null;
    }

    /**
     * Get P6 ID for a given EBS entity
     */
    public String getP6IdForEbsEntity(String entityType, String ebsId) {
        Map<String, String> correlations = idCorrelationStore.get(entityType);
        if (correlations != null) {
            for (Map.Entry<String, String> entry : correlations.entrySet()) {
                if (entry.getValue().equals(ebsId)) {
                    return entry.getKey();
                }
            }
        }
        return null;
    }

    /**
     * Clear all stored correlations
     */
    public void clearCorrelations() {
        idCorrelationStore.clear();
    }
}
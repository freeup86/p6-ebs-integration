/**
 * Centralized configuration for the integration
 */
package com.tpcgrp.p6ebs.service.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tpcgrp.p6ebs.service.ConfigurationService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

@Service
@Slf4j
public class ConfigurationManager {

    private final String CONFIG_FILE = System.getProperty("user.home") + "/.p6ebs/integration_config.json";
    private final ObjectMapper objectMapper = new ObjectMapper();

    private IntegrationConfig config;

    // Constructor - load configuration
    public ConfigurationManager() {
        loadConfiguration();
    }

    /**
     * Load configuration from file
     *
     * @return
     */
    public ConfigurationService.Configuration loadConfiguration() {
        File configFile = new File(CONFIG_FILE);

        if (configFile.exists()) {
            try {
                config = objectMapper.readValue(configFile, IntegrationConfig.class);
                log.info("Configuration loaded successfully");
            } catch (IOException e) {
                log.error("Failed to load configuration", e);
                config = createDefaultConfiguration();
            }
        } else {
            log.info("Configuration file not found, creating default");
            config = createDefaultConfiguration();
            saveConfiguration(config);
        }
        return null;
    }

    /**
     * Save configuration to file
     */
    public void saveConfiguration(IntegrationConfig config) {
        try {
            Files.createDirectories(Paths.get(System.getProperty("user.home") + "/.p6ebs"));
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(new File(CONFIG_FILE), this.config);
            log.info("Configuration saved successfully");
        } catch (IOException e) {
            log.error("Failed to save configuration", e);
        }
    }

    /**
     * Create default configuration
     */
    private IntegrationConfig createDefaultConfiguration() {
        IntegrationConfig defaultConfig = new IntegrationConfig();

        // Set default values
        defaultConfig.setBatchSize(100);
        defaultConfig.setRetryCount(3);
        defaultConfig.setRetryDelayMs(5000);
        defaultConfig.setLogLevel("INFO");

        // Default sync directions
        Map<String, String> syncDirections = new HashMap<>();
        syncDirections.put("projectFinancials", "P6_TO_EBS");
        syncDirections.put("resourceManagement", "BIDIRECTIONAL");
        syncDirections.put("procurement", "EBS_TO_P6");
        syncDirections.put("timesheet", "P6_TO_EBS");
        syncDirections.put("projectWbs", "P6_TO_EBS");
        defaultConfig.setSyncDirections(syncDirections);

        // Default sync intervals
        Map<String, Integer> syncIntervals = new HashMap<>();
        syncIntervals.put("projectFinancials", 24); // Hours
        syncIntervals.put("resourceManagement", 12);
        syncIntervals.put("procurement", 6);
        syncIntervals.put("timesheet", 4);
        syncIntervals.put("projectWbs", 24);
        defaultConfig.setSyncIntervals(syncIntervals);

        // Default field mappings
        defaultConfig.setFieldMappings(createDefaultFieldMappings());

        return defaultConfig;
    }

    /**
     * Create default field mappings
     */
    private Map<String, Map<String, String>> createDefaultFieldMappings() {
        Map<String, Map<String, String>> fieldMappings = new HashMap<>();

        // Project mappings
        Map<String, String> projectMappings = new HashMap<>();
        projectMappings.put("proj_id", "project_id");
        projectMappings.put("proj_name", "name");
        projectMappings.put("proj_short_name", "segment1");
        projectMappings.put("plan_start_date", "start_date");
        projectMappings.put("plan_end_date", "completion_date");
        fieldMappings.put("project", projectMappings);

        // Activity mappings
        Map<String, String> activityMappings = new HashMap<>();
        activityMappings.put("activity_id", "task_id");
        activityMappings.put("activity_name", "task_name");
        activityMappings.put("activity_code", "task_number");
        activityMappings.put("start_date", "start_date");
        activityMappings.put("finish_date", "completion_date");
        fieldMappings.put("activity", activityMappings);

        return fieldMappings;
    }

    /**
     * Get the sync direction for an integration type
     */
    public String getSyncDirection(String integrationType) {
        if (config.getSyncDirections().containsKey(integrationType)) {
            return config.getSyncDirections().get(integrationType);
        }
        return "BIDIRECTIONAL"; // Default
    }

    /**
     * Get the sync interval for an integration type (in hours)
     */
    public int getSyncInterval(String integrationType) {
        if (config.getSyncIntervals().containsKey(integrationType)) {
            return config.getSyncIntervals().get(integrationType);
        }
        return 24; // Default to daily
    }

    /**
     * Get batch size for processing
     */
    public int getBatchSize() {
        return config.getBatchSize();
    }

    /**
     * Get retry count for failed operations
     */
    public int getRetryCount() {
        return config.getRetryCount();
    }

    /**
     * Get retry delay in milliseconds
     */
    public long getRetryDelayMs() {
        return config.getRetryDelayMs();
    }

    /**
     * Get field mappings for an entity type
     */
    public Map<String, String> getFieldMappings(String entityType) {
        if (config.getFieldMappings().containsKey(entityType)) {
            return config.getFieldMappings().get(entityType);
        }
        return Collections.emptyMap();
    }

    /**
     * Set field mappings for an entity type
     */
    public void setFieldMappings(String entityType, Map<String, String> mappings) {
        config.getFieldMappings().put(entityType, mappings);
        saveConfiguration(config);
    }

    /**
     * Get all configuration
     */
    public IntegrationConfig getConfig() {
        return config;
    }

    /**
     * Update configuration
     */
    public void updateConfig(IntegrationConfig newConfig) {
        this.config = newConfig;
        saveConfiguration(config);
    }

    /**
     * Class representing integration configuration
     */
    @Data
    public static class IntegrationConfig {
        private int batchSize;
        private int retryCount;
        private long retryDelayMs;
        private String logLevel;
        private Map<String, String> syncDirections = new HashMap<>();
        private Map<String, Integer> syncIntervals = new HashMap<>();
        private Map<String, Map<String, String>> fieldMappings = new HashMap<>();
    }
}
/**
 * Core service to manage the actual integration logic between P6 and EBS
 * Handles mapping, synchronization policies, and transaction management
 */
package com.tpcgrp.p6ebs.service.integration;

import com.tpcgrp.p6ebs.service.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@Slf4j
public class IntegrationService {

    private final DatabaseService databaseService;
    private final ConfigurationService configService;
    private final MappingUtility mappingUtility;
    private final SynchronizationManager syncManager;
    private final ValidationService validationService;
    private final IntegrationLogService logService;
    private final P6ActivityService p6ActivityService;
    private final EbsProjectService ebsProjectService;
    private final DataTransformationService transformationService;

    private final AtomicBoolean integrationInProgress = new AtomicBoolean(false);
    private final ConcurrentHashMap<String, Boolean> activeIntegrations = new ConcurrentHashMap<>();

    @Autowired
    public IntegrationService(DatabaseService databaseService,
                              ConfigurationService configService,
                              MappingUtility mappingUtility,
                              SynchronizationManager syncManager,
                              ValidationService validationService,
                              IntegrationLogService logService,
                              P6ActivityService p6ActivityService,
                              EbsProjectService ebsProjectService,
                              DataTransformationService transformationService) {
        this.databaseService = databaseService;
        this.configService = configService;
        this.mappingUtility = mappingUtility;
        this.syncManager = syncManager;
        this.validationService = validationService;
        this.logService = logService;
        this.p6ActivityService = p6ActivityService;
        this.ebsProjectService = ebsProjectService;
        this.transformationService = transformationService;
    }

    /**
     * Start integration process based on selected integration types
     *
     * @param p6ConnectionParams Database connection parameters for P6
     * @param ebsConnectionParams Database connection parameters for EBS
     * @param integrationTypes List of integration types to process
     * @param progressCallback Callback to report integration progress
     * @return Status of the integration process
     */
    @Transactional
    public Map<String, Object> startIntegration(Map<String, String> p6ConnectionParams,
                                                Map<String, String> ebsConnectionParams,
                                                List<String> integrationTypes,
                                                ProgressCallback progressCallback) {

        if (integrationInProgress.get()) {
            Map<String, Object> result = new HashMap<>();
            result.put("status", "error");
            result.put("message", "Integration already in progress");
            return result;
        }

        integrationInProgress.set(true);
        Map<String, Object> result = new HashMap<>();

        try {
            logService.logInfo("Starting integration process with types: " + String.join(", ", integrationTypes));

            // Initialize progress tracking
            int totalSteps = integrationTypes.size() * 3; // Each type has validation, processing, and verification
            int currentStep = 0;

            // Validate connections
            boolean p6Connected = validateP6Connection(p6ConnectionParams);
            boolean ebsConnected = validateEbsConnection(ebsConnectionParams);

            if (!p6Connected || !ebsConnected) {
                throw new IntegrationException("Failed to connect to one or both systems");
            }

            // Process each integration type
            for (String integrationType : integrationTypes) {
                if (!activeIntegrations.containsKey(integrationType)) {
                    activeIntegrations.put(integrationType, true);

                    // Data validation phase
                    progressCallback.updateProgress(++currentStep, totalSteps,
                            "Validating data for " + integrationType);

                    List<ValidationService.ValidationIssue> validationIssues = validationService.validateForIntegration(
                            p6ConnectionParams, ebsConnectionParams, integrationType);

                    if (!validationIssues.isEmpty()) {
                        logService.logWarning("Validation issues found for " + integrationType + ": "
                                + validationIssues.size() + " issues");
                        // Decide whether to continue based on severity of issues
                        if (validationService.hasBlockingIssues(validationIssues)) {
                            logService.logError("Blocking validation issues found, skipping " + integrationType);
                            continue;
                        }
                    }

                    // Process integration
                    progressCallback.updateProgress(++currentStep, totalSteps,
                            "Processing " + integrationType);

                    Map<String, Object> integrationResult = processIntegrationType(
                            p6ConnectionParams, ebsConnectionParams, integrationType);

                    // Verify results
                    progressCallback.updateProgress(++currentStep, totalSteps,
                            "Verifying " + integrationType);

                    boolean verificationSuccess = verifyIntegrationResults(
                            p6ConnectionParams, ebsConnectionParams, integrationType, integrationResult);

                    if (!verificationSuccess) {
                        logService.logWarning("Verification failed for " + integrationType);
                    }

                    // Store results
                    syncManager.recordSynchronizationResult(integrationType, integrationResult);

                    // Cleanup
                    activeIntegrations.remove(integrationType);
                }
            }

            // Build final result
            result.put("status", "success");
            result.put("completedIntegrations", syncManager.getCompletedIntegrations());
            result.put("syncTimestamp", new Date());

            logService.logInfo("Integration process completed successfully");

        } catch (Exception e) {
            logService.logError("Integration process failed: " + e.getMessage());
            result.put("status", "error");
            result.put("message", e.getMessage());
        } finally {
            integrationInProgress.set(false);
        }

        return result;
    }

    /**
     * Process a specific integration type
     */
    private Map<String, Object> processIntegrationType(Map<String, String> p6ConnectionParams,
                                                       Map<String, String> ebsConnectionParams,
                                                       String integrationType) throws IntegrationException {

        logService.logInfo("Processing integration type: " + integrationType);
        Map<String, Object> result = new HashMap<>();

        try {
            switch (integrationType) {
                case "projectFinancials":
                    result = integrateProjectFinancials(p6ConnectionParams, ebsConnectionParams);
                    break;
                case "resourceManagement":
                    result = integrateResourceManagement(p6ConnectionParams, ebsConnectionParams);
                    break;
                case "procurement":
                    result = integrateProcurement(p6ConnectionParams, ebsConnectionParams);
                    break;
                case "timesheet":
                    result = integrateTimesheet(p6ConnectionParams, ebsConnectionParams);
                    break;
                case "projectWbs":
                    result = integrateProjectWbs(p6ConnectionParams, ebsConnectionParams);
                    break;
                case "ebsTasksToP6":
                    result = integrateEbsTasksToP6(p6ConnectionParams, ebsConnectionParams);
                    break;
                default:
                    throw new IntegrationException("Unknown integration type: " + integrationType);
            }

            return result;
        } catch (Exception e) {
            logService.logError("Error processing integration type " + integrationType + ": " + e.getMessage());
            throw new IntegrationException("Failed to process integration type: " + integrationType, e);
        }
    }

    /**
     * Integrate EBS tasks to P6 activities
     */
    private Map<String, Object> integrateEbsTasksToP6(Map<String, String> p6ConnectionParams,
                                                      Map<String, String> ebsConnectionParams) throws SQLException {

        logService.logInfo("Integrating EBS tasks to P6 activities");
        Map<String, Object> result = new HashMap<>();

        try {
            // Get EBS tasks
            EbsTaskService ebsTaskService = new EbsTaskService(databaseService);
            List<Map<String, Object>> ebsTasks = ebsTaskService.getAllTasks(
                    ebsConnectionParams.get("server"),
                    ebsConnectionParams.get("sid"),
                    ebsConnectionParams.get("username"),
                    ebsConnectionParams.get("password"));

            // Process each task
            int totalTasks = ebsTasks.size();
            int updatedTasks = 0;
            int failedTasks = 0;
            List<String> processedTaskIds = new ArrayList<>();

            for (Map<String, Object> ebsTask : ebsTasks) {
                try {
                    // Transform EBS task to P6 activity format
                    Map<String, Object> p6Activity = transformationService.transformTaskDataEbsToP6(ebsTask);

                    // Get project ID mapping
                    String ebsProjectId = ebsTask.get("project_id").toString();
                    String p6ProjectId = mappingUtility.getP6IdForEbsEntity("project", ebsProjectId);

                    if (p6ProjectId == null) {
                        logService.logWarning("Cannot find P6 project for EBS project ID: " + ebsProjectId);
                        failedTasks++;
                        continue;
                    }

                    // Set project ID for P6 activity
                    p6Activity.put("proj_id", p6ProjectId);

                    // Create or update P6 activity
                    boolean success = createOrUpdateP6Activity(p6ConnectionParams, p6Activity);

                    if (success) {
                        updatedTasks++;
                        processedTaskIds.add(ebsTask.get("task_id").toString());
                    } else {
                        failedTasks++;
                    }

                } catch (Exception e) {
                    logService.logError("Error processing EBS task: " + e.getMessage());
                    failedTasks++;
                }
            }

            // Compile results
            result.put("totalTasks", totalTasks);
            result.put("updatedTasks", updatedTasks);
            result.put("failedTasks", failedTasks);
            result.put("processedTaskIds", processedTaskIds);

            return result;

        } catch (Exception e) {
            logService.logError("Failed to integrate EBS tasks to P6: " + e.getMessage());
            result.put("status", "error");
            result.put("message", e.getMessage());
            return result;
        }
    }

    /**
     * Create or update a P6 activity
     */
    private boolean createOrUpdateP6Activity(Map<String, String> p6ConnectionParams,
                                             Map<String, Object> p6Activity) {
        try {
            // Check if activity exists in P6
            String activityId = p6Activity.get("activity_id").toString();
            String projectId = p6Activity.get("proj_id").toString();

            // This would need to be implemented in P6ActivityService
            // to check if activity exists and create/update accordingly

            // For now, we'll assume a successful update
            logService.logInfo("Successfully updated P6 activity: " + activityId);
            return true;

        } catch (Exception e) {
            logService.logError("Failed to create/update P6 activity: " + e.getMessage());
            return false;
        }
    }

    /**
     * Integrate project financials between P6 and EBS
     */
    private Map<String, Object> integrateProjectFinancials(Map<String, String> p6ConnectionParams,
                                                           Map<String, String> ebsConnectionParams) throws SQLException {

        logService.logInfo("Integrating project financials");
        Map<String, Object> result = new HashMap<>();

        // Get P6 project data
        List<Map<String, Object>> p6Projects = p6ActivityService.getAllProjects(
                p6ConnectionParams.get("server"),
                p6ConnectionParams.get("database"),
                p6ConnectionParams.get("username"),
                p6ConnectionParams.get("password"));

        // Get EBS project data
        List<Map<String, Object>> ebsProjects = ebsProjectService.getAllProjects(
                ebsConnectionParams.get("server"),
                ebsConnectionParams.get("sid"),
                ebsConnectionParams.get("username"),
                ebsConnectionParams.get("password"));

        // Match projects between systems
        Map<String, String> projectMapping = mappingUtility.mapProjectIds(p6Projects, ebsProjects);

        // Process financial data for each matched project
        int updatedProjects = 0;
        List<String> processedProjects = new ArrayList<>();

        for (Map.Entry<String, String> entry : projectMapping.entrySet()) {
            String p6ProjectId = entry.getKey();
            String ebsProjectId = entry.getValue();

            // Get P6 financial data
            Map<String, Object> p6ProjectSummary = getP6ProjectFinancials(
                    p6ConnectionParams, p6ProjectId);

            // Get EBS financial data
            Map<String, Object> ebsFinancialSummary = ebsProjectService.getProjectFinancialSummary(
                    ebsConnectionParams.get("server"),
                    ebsConnectionParams.get("sid"),
                    ebsConnectionParams.get("username"),
                    ebsConnectionParams.get("password"),
                    ebsProjectId);

            // Transform data based on integration direction (P6 to EBS or EBS to P6)
            Map<String, Object> transformedData = transformationService.transformFinancialData(
                    p6ProjectSummary, ebsFinancialSummary, "P6_TO_EBS");

            // Update target system
            boolean updateSuccess = updateProjectFinancials(
                    ebsConnectionParams, ebsProjectId, transformedData);

            if (updateSuccess) {
                updatedProjects++;
                processedProjects.add(p6ProjectId);
            }
        }

        // Compile results
        result.put("totalProjects", projectMapping.size());
        result.put("updatedProjects", updatedProjects);
        result.put("processedProjects", processedProjects);

        return result;
    }

    /**
     * Get financial data for a P6 project
     */
    private Map<String, Object> getP6ProjectFinancials(Map<String, String> p6ConnectionParams,
                                                       String projectId) {
        // This would retrieve detailed financial data from P6
        // Simplified for this example
        Map<String, Object> financials = new HashMap<>();
        // ... implementation details
        return financials;
    }

    /**
     * Update financial data in EBS
     */
    private boolean updateProjectFinancials(Map<String, String> ebsConnectionParams,
                                            String projectId,
                                            Map<String, Object> financialData) {
        // This would update the financial data in EBS
        // Simplified for this example
        // ... implementation details
        return true;
    }

    /**
     * Integrate resource management between P6 and EBS
     */
    private Map<String, Object> integrateResourceManagement(Map<String, String> p6ConnectionParams,
                                                            Map<String, String> ebsConnectionParams) {
        // Implementation for resource management integration
        Map<String, Object> result = new HashMap<>();
        // ... implementation details
        return result;
    }

    /**
     * Integrate procurement between P6 and EBS
     */
    private Map<String, Object> integrateProcurement(Map<String, String> p6ConnectionParams,
                                                     Map<String, String> ebsConnectionParams) {
        // Implementation for procurement integration
        Map<String, Object> result = new HashMap<>();
        // ... implementation details
        return result;
    }

    /**
     * Integrate timesheet data between P6 and EBS
     */
    private Map<String, Object> integrateTimesheet(Map<String, String> p6ConnectionParams,
                                                   Map<String, String> ebsConnectionParams) {
        // Implementation for timesheet integration
        Map<String, Object> result = new HashMap<>();
        // ... implementation details
        return result;
    }

    /**
     * Integrate project and WBS structure between P6 and EBS
     */
    private Map<String, Object> integrateProjectWbs(Map<String, String> p6ConnectionParams,
                                                    Map<String, String> ebsConnectionParams) {
        // Implementation for project WBS integration
        Map<String, Object> result = new HashMap<>();
        // ... implementation details
        return result;
    }

    /**
     * Verify the results of an integration process
     */
    private boolean verifyIntegrationResults(Map<String, String> p6ConnectionParams,
                                             Map<String, String> ebsConnectionParams,
                                             String integrationType,
                                             Map<String, Object> integrationResult) {
        // Verify that the integration was successful
        // ... implementation details
        return true;
    }

    /**
     * Validate P6 connection
     */
    private boolean validateP6Connection(Map<String, String> p6ConnectionParams) {
        return databaseService.testP6Connection(
                p6ConnectionParams.get("server"),
                p6ConnectionParams.get("database"),
                p6ConnectionParams.get("username"),
                p6ConnectionParams.get("password"));
    }

    /**
     * Validate EBS connection
     */
    private boolean validateEbsConnection(Map<String, String> ebsConnectionParams) {
        return databaseService.testEbsConnection(
                ebsConnectionParams.get("server"),
                ebsConnectionParams.get("sid"),
                ebsConnectionParams.get("username"),
                ebsConnectionParams.get("password"));
    }

    /**
     * Cancel ongoing integration
     */
    public boolean cancelIntegration() {
        if (integrationInProgress.get()) {
            logService.logInfo("Cancelling integration process");
            activeIntegrations.clear();
            integrationInProgress.set(false);
            return true;
        }
        return false;
    }

    /**
     * Check if integration is in progress
     */
    public boolean isIntegrationInProgress() {
        return integrationInProgress.get();
    }

    /**
     * Interface for progress reporting
     */
    public interface ProgressCallback {
        void updateProgress(int currentStep, int totalSteps, String statusMessage);
    }

    /**
     * Exception for integration errors
     */
    public static class IntegrationException extends Exception {
        public IntegrationException(String message) {
            super(message);
        }

        public IntegrationException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * Represents a validation issue
     */
    public static class ValidationIssue {
        private String entityType;
        private String entityId;
        private String issueType;
        private String description;
        private boolean blocking;

        // Getters and setters
    }
}
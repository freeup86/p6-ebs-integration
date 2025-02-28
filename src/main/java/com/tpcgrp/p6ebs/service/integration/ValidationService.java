/**
 * Service for validating data before transfer between P6 and EBS
 */
package com.tpcgrp.p6ebs.service.integration;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.*;

@Service
@Slf4j
public class ValidationService {

    private final IntegrationLogService logService;

    public ValidationService(IntegrationLogService logService) {
        this.logService = logService;
    }

    /**
     * Validate data before integration
     */
    public List<ValidationIssue> validateForIntegration(Map<String, String> p6ConnectionParams,
                                                        Map<String, String> ebsConnectionParams,
                                                        String integrationType) {

        logService.logInfo("Validating data for integration type: " + integrationType);
        List<ValidationIssue> issues = new ArrayList<>();

        try {
            switch (integrationType) {
                case "projectFinancials":
                    issues.addAll(validateProjectFinancials(p6ConnectionParams, ebsConnectionParams));
                    break;
                case "resourceManagement":
                    issues.addAll(validateResourceManagement(p6ConnectionParams, ebsConnectionParams));
                    break;
                case "procurement":
                    issues.addAll(validateProcurement(p6ConnectionParams, ebsConnectionParams));
                    break;
                case "timesheet":
                    issues.addAll(validateTimesheet(p6ConnectionParams, ebsConnectionParams));
                    break;
                case "projectWbs":
                    issues.addAll(validateProjectWbs(p6ConnectionParams, ebsConnectionParams));
                    break;
                default:
                    throw new IllegalArgumentException("Unknown integration type: " + integrationType);
            }
        } catch (Exception e) {
            logService.logError("Validation error for " + integrationType + ": " + e.getMessage());
            ValidationIssue issue = new ValidationIssue();
            issue.setEntityType(integrationType);
            issue.setIssueType("VALIDATION_ERROR");
            issue.setDescription("Error during validation: " + e.getMessage());
            issue.setBlocking(true);
            issues.add(issue);
        }

        return issues;
    }

    /**
     * Validate project financials data
     */
    private List<ValidationIssue> validateProjectFinancials(Map<String, String> p6ConnectionParams,
                                                            Map<String, String> ebsConnectionParams) throws SQLException {
        List<ValidationIssue> issues = new ArrayList<>();

        // Check for projects with missing financial data
        // Implementation details

        // Check for currency mismatches
        // Implementation details

        // Check for negative budget values
        // Implementation details

        return issues;
    }

    /**
     * Validate resource management data
     */
    private List<ValidationIssue> validateResourceManagement(Map<String, String> p6ConnectionParams,
                                                             Map<String, String> ebsConnectionParams) {
        List<ValidationIssue> issues = new ArrayList<>();
        // Implementation details
        return issues;
    }

    /**
     * Validate procurement data
     */
    private List<ValidationIssue> validateProcurement(Map<String, String> p6ConnectionParams,
                                                      Map<String, String> ebsConnectionParams) {
        List<ValidationIssue> issues = new ArrayList<>();
        // Implementation details
        return issues;
    }

    /**
     * Validate timesheet data
     */
    private List<ValidationIssue> validateTimesheet(Map<String, String> p6ConnectionParams,
                                                    Map<String, String> ebsConnectionParams) {
        List<ValidationIssue> issues = new ArrayList<>();
        // Implementation details
        return issues;
    }

    /**
     * Validate project WBS data
     */
    private List<ValidationIssue> validateProjectWbs(Map<String, String> p6ConnectionParams,
                                                     Map<String, String> ebsConnectionParams) {
        List<ValidationIssue> issues = new ArrayList<>();
        // Implementation details
        return issues;
    }

    /**
     * Check if there are any blocking validation issues
     */
    public boolean hasBlockingIssues(List<ValidationIssue> issues) {
        return issues.stream().anyMatch(ValidationIssue::isBlocking);
    }

    /**
     * Generate a validation report
     */
    public ValidationReport generateValidationReport(List<ValidationIssue> issues) {
        ValidationReport report = new ValidationReport();
        report.setTimestamp(new Date());
        report.setTotalIssues(issues.size());
        report.setBlockingIssues((int) issues.stream().filter(ValidationIssue::isBlocking).count());
        report.setWarningIssues((int) issues.stream().filter(i -> !i.isBlocking()).count());
        report.setIssues(issues);

        return report;
    }

    /**
     * Class representing a validation issue
     */
    @Setter
    @Getter
    public static class ValidationIssue {
        // Getters and setters
        private String entityType;
        private String entityId;
        private String issueType;
        private String description;
        private boolean blocking;

    }

    /**
     * Class representing a validation report
     */
    public static class ValidationReport {
        private Date timestamp;
        private int totalIssues;
        private int blockingIssues;
        private int warningIssues;
        private List<ValidationIssue> issues;

        // Getters and setters
        public Date getTimestamp() { return timestamp; }
        public void setTimestamp(Date timestamp) { this.timestamp = timestamp; }
        public int getTotalIssues() { return totalIssues; }
        public void setTotalIssues(int totalIssues) { this.totalIssues = totalIssues; }
        public int getBlockingIssues() { return blockingIssues; }
        public void setBlockingIssues(int blockingIssues) { this.blockingIssues = blockingIssues; }
        public int getWarningIssues() { return warningIssues; }
        public void setWarningIssues(int warningIssues) { this.warningIssues = warningIssues; }
        public List<ValidationIssue> getIssues() { return issues; }
        public void setIssues(List<ValidationIssue> issues) { this.issues = issues; }
    }
}
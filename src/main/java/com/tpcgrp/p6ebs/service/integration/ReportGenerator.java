/**
 * Service for generating integration reports
 */
package com.tpcgrp.p6ebs.service.integration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
@Slf4j
public class ReportGenerator {

    private final IntegrationLogService logService;
    private final SynchronizationManager syncManager;
    private final String REPORTS_DIRECTORY = System.getProperty("user.home") + "/.p6ebs/reports";

    public ReportGenerator(IntegrationLogService logService, SynchronizationManager syncManager) {
        this.logService = logService;
        this.syncManager = syncManager;

        // Create reports directory if it doesn't exist
        File reportsDir = new File(REPORTS_DIRECTORY);
        if (!reportsDir.exists()) {
            reportsDir.mkdirs();
        }
    }

    /**
     * Generate an integration summary report
     */
    public File generateSummaryReport() {
        try {
            // Create file name with timestamp
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String fileName = REPORTS_DIRECTORY + "/integration_summary_" + timestamp + ".txt";

            File reportFile = new File(fileName);

            try (FileWriter writer = new FileWriter(reportFile)) {
                // Report header
                writer.write("P6-EBS INTEGRATION SUMMARY REPORT\n");
                writer.write("Generated: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "\n");
                writer.write("----------------------------------------------\n\n");

                // Last synchronization times
                Map<String, Date> completedIntegrations = syncManager.getCompletedIntegrations();
                writer.write("LAST SYNCHRONIZATION TIMES:\n");

                if (completedIntegrations.isEmpty()) {
                    writer.write("No synchronization records found.\n");
                } else {
                    for (Map.Entry<String, Date> entry : completedIntegrations.entrySet()) {
                        writer.write(entry.getKey() + ": " +
                                (entry.getValue() != null ?
                                        new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(entry.getValue()) :
                                        "Never") + "\n");
                    }
                }

                writer.write("\n");

                // Synchronization history
                writer.write("SYNCHRONIZATION HISTORY:\n");
                List<SynchronizationManager.SyncRecord> history = syncManager.getSyncHistory(null);

                if (history.isEmpty()) {
                    writer.write("No synchronization history found.\n");
                } else {
                    // Sort by start time (descending)
                    history.sort((r1, r2) -> r2.getStartTime().compareTo(r1.getStartTime()));

                    // Write last 10 records
                    int count = 0;
                    for (SynchronizationManager.SyncRecord record : history) {
                        if (count++ >= 10) break;

                        writer.write("Session: " + record.getSessionId() + "\n");
                        writer.write("  Type: " + record.getSyncType() + "\n");
                        writer.write("  Status: " + record.getStatus() + "\n");
                        writer.write("  Started: " +
                                new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(record.getStartTime()) + "\n");
                        writer.write("  Duration: " + (record.getDurationMs() / 1000) + " seconds\n");
                        writer.write("  Entities processed: " + record.getEntitiesProcessed() + "\n");
                        writer.write("  Entities updated: " + record.getEntitiesUpdated() + "\n");

                        if (record.getErrorMessage() != null) {
                            writer.write("  Error: " + record.getErrorMessage() + "\n");
                        }

                        writer.write("\n");
                    }
                }

                // Append log summary
                writer.write("RECENT LOG ENTRIES:\n");
                List<IntegrationLogService.LogEntry> logs = logService.getRecentLogs(IntegrationLogService.LogLevel.WARNING);

                if (logs.isEmpty()) {
                    writer.write("No relevant log entries found.\n");
                } else {
                    // Sort by timestamp (descending)
                    logs.sort((l1, l2) -> l2.getTimestamp().compareTo(l1.getTimestamp()));

                    // Write last 20 warning/error logs
                    int count = 0;
                    for (IntegrationLogService.LogEntry entry : logs) {
                        if (count++ >= 20) break;

                        writer.write(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(entry.getTimestamp()) +
                                " [" + entry.getLevel() + "] " + entry.getMessage() + "\n");
                    }
                }
            }

            logService.logInfo("Generated integration summary report: " + fileName);
            return reportFile;
        } catch (Exception e) {
            logService.logError("Failed to generate summary report");
            return null;
        }
    }

    /**
     * Generate a detailed report for a specific integration type
     */
    public File generateDetailedReport(String integrationType) {
        try {
            // Create file name with timestamp
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String fileName = REPORTS_DIRECTORY + "/" + integrationType + "_report_" + timestamp + ".txt";

            File reportFile = new File(fileName);

            try (FileWriter writer = new FileWriter(reportFile)) {
                // Report header
                writer.write(integrationType.toUpperCase() + " INTEGRATION DETAILED REPORT\n");
                writer.write("Generated: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "\n");
                writer.write("----------------------------------------------\n\n");

                // Synchronization history for this type
                writer.write("SYNCHRONIZATION HISTORY:\n");
                List<SynchronizationManager.SyncRecord> history = syncManager.getSyncHistory(integrationType);

                if (history.isEmpty()) {
                    writer.write("No synchronization history found for " + integrationType + ".\n");
                } else {
                    // Sort by start time (descending)
                    history.sort((r1, r2) -> r2.getStartTime().compareTo(r1.getStartTime()));

                    // Write all records for this type
                    for (SynchronizationManager.SyncRecord record : history) {
                        writer.write("Session: " + record.getSessionId() + "\n");
                        writer.write("  Started: " +
                                new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(record.getStartTime()) + "\n");
                        writer.write("  Ended: " +
                                new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(record.getEndTime()) + "\n");
                        writer.write("  Direction: " + record.getDirection() + "\n");
                        writer.write("  Status: " + record.getStatus() + "\n");
                        writer.write("  Duration: " + (record.getDurationMs() / 1000) + " seconds\n");
                        writer.write("  Entities processed: " + record.getEntitiesProcessed() + "\n");
                        writer.write("  Entities updated: " + record.getEntitiesUpdated() + "\n");
                        writer.write("  Entities failed: " + record.getEntitiesFailed() + "\n");

                        if (record.getErrorMessage() != null) {
                            writer.write("  Error: " + record.getErrorMessage() + "\n");
                        }

                        writer.write("\n");
                    }
                }

                // Statistics section
                writer.write("STATISTICS:\n");

                // Calculate statistics from history
                int totalRuns = history.size();
                int successfulRuns = 0;
                int failedRuns = 0;
                int totalEntitiesProcessed = 0;
                int totalEntitiesUpdated = 0;
                long totalDuration = 0;

                for (SynchronizationManager.SyncRecord record : history) {
                    if ("COMPLETED".equals(record.getStatus())) {
                        successfulRuns++;
                    } else {
                        failedRuns++;
                    }

                    totalEntitiesProcessed += record.getEntitiesProcessed();
                    totalEntitiesUpdated += record.getEntitiesUpdated();
                    totalDuration += record.getDurationMs();
                }

                writer.write("Total runs: " + totalRuns + "\n");
                writer.write("Successful runs: " + successfulRuns + "\n");
                writer.write("Failed runs: " + failedRuns + "\n");
                writer.write("Success rate: " + (totalRuns > 0 ? (successfulRuns * 100 / totalRuns) : 0) + "%\n");
                writer.write("Total entities processed: " + totalEntitiesProcessed + "\n");
                writer.write("Total entities updated: " + totalEntitiesUpdated + "\n");
                if (totalRuns > 0) {
                    writer.write("Average duration: " + (totalDuration / totalRuns / 1000) + " seconds\n");
                }

                writer.write("\n");

                // Recent log entries for this type
                writer.write("RECENT LOG ENTRIES:\n");
                List<IntegrationLogService.LogEntry> logs = logService.getRecentLogs();

                if (logs.isEmpty()) {
                    writer.write("No log entries found.\n");
                } else {
                    // Sort by timestamp (descending)
                    logs.sort((l1, l2) -> l2.getTimestamp().compareTo(l1.getTimestamp()));

                    // Filter and write logs related to this integration type
                    int count = 0;
                    for (IntegrationLogService.LogEntry entry : logs) {
                        if (count >= 50) break; // Limit to 50 entries

                        if (entry.getMessage().contains(integrationType)) {
                            writer.write(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(entry.getTimestamp()) +
                                    " [" + entry.getLevel() + "] " + entry.getMessage() + "\n");
                            count++;
                        }
                    }
                }
            }

            logService.logInfo("Generated detailed report for " + integrationType + ": " + fileName);
            return reportFile;
        } catch (Exception e) {
            logService.logError("Failed to generate detailed report for " + integrationType);
            return null;
        }
    }

    /**
     * Generate a data reconciliation report
     */
    public File generateReconciliationReport(String integrationType,
                                             Map<String, Object> p6Data,
                                             Map<String, Object> ebsData) {
        try {
            // Create file name with timestamp
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String fileName = REPORTS_DIRECTORY + "/" + integrationType + "_reconciliation_" + timestamp + ".txt";

            File reportFile = new File(fileName);

            try (FileWriter writer = new FileWriter(reportFile)) {
                // Report header
                writer.write(integrationType.toUpperCase() + " DATA RECONCILIATION REPORT\n");
                writer.write("Generated: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "\n");
                writer.write("----------------------------------------------\n\n");

                // Compare data between systems
                writer.write("DATA COMPARISON:\n\n");

                // Get keys from both data sets
                Set<String> allKeys = new HashSet<>();
                if (p6Data != null) allKeys.addAll(p6Data.keySet());
                if (ebsData != null) allKeys.addAll(ebsData.keySet());

                // Write comparison table header
                writer.write(String.format("%-30s | %-30s | %-30s | %s\n", "Field", "P6 Value", "EBS Value", "Match"));
                writer.write(String.format("%-30s-|-%-30s-|-%-30s-|-%s\n",
                        "------------------------------",
                        "------------------------------",
                        "------------------------------",
                        "-----"));

                // Write comparison for each field
                for (String key : allKeys) {
                    Object p6Value = p6Data != null ? p6Data.get(key) : null;
                    Object ebsValue = ebsData != null ? ebsData.get(key) : null;

                    boolean match = Objects.equals(p6Value, ebsValue);

                    writer.write(String.format("%-30s | %-30s | %-30s | %s\n",
                            key,
                            p6Value != null ? p6Value.toString() : "N/A",
                            ebsValue != null ? ebsValue.toString() : "N/A",
                            match ? "Yes" : "No"));
                }

                writer.write("\n");

                // Summary section
                writer.write("SUMMARY:\n");
                int totalFields = allKeys.size();
                int matchingFields = 0;
                int mismatchedFields = 0;
                int p6OnlyFields = 0;
                int ebsOnlyFields = 0;

                for (String key : allKeys) {
                    Object p6Value = p6Data != null ? p6Data.get(key) : null;
                    Object ebsValue = ebsData != null ? ebsData.get(key) : null;

                    if (p6Value != null && ebsValue != null) {
                        if (Objects.equals(p6Value, ebsValue)) {
                            matchingFields++;
                        } else {
                            mismatchedFields++;
                        }
                    } else if (p6Value != null) {
                        p6OnlyFields++;
                    } else if (ebsValue != null) {
                        ebsOnlyFields++;
                    }
                }

                writer.write("Total fields: " + totalFields + "\n");
                writer.write("Matching fields: " + matchingFields + "\n");
                writer.write("Mismatched fields: " + mismatchedFields + "\n");
                writer.write("P6 only fields: " + p6OnlyFields + "\n");
                writer.write("EBS only fields: " + ebsOnlyFields + "\n");
                writer.write("Match percentage: " + (totalFields > 0 ? (matchingFields * 100 / totalFields) : 0) + "%\n");
            }

            logService.logInfo("Generated reconciliation report for " + integrationType + ": " + fileName);
            return reportFile;
        } catch (Exception e) {
            logService.logError("Failed to generate reconciliation report for " + integrationType);
            return null;
        }
    }

    /**
     * Get list of available reports
     */
    public List<ReportInfo> getAvailableReports() {
        List<ReportInfo> reports = new ArrayList<>();

        File reportsDir = new File(REPORTS_DIRECTORY);
        File[] files = reportsDir.listFiles((dir, name) -> name.endsWith(".txt"));

        if (files != null) {
            for (File file : files) {
                ReportInfo info = new ReportInfo();
                info.setFileName(file.getName());
                info.setFilePath(file.getAbsolutePath());
                info.setFileSize(file.length());
                info.setCreationDate(new Date(file.lastModified()));

                // Determine report type from filename
                if (file.getName().contains("summary")) {
                    info.setReportType("Summary");
                } else if (file.getName().contains("reconciliation")) {
                    info.setReportType("Reconciliation");
                } else {
                    info.setReportType("Detailed");
                }

                reports.add(info);
            }

            // Sort by creation date (descending)
            reports.sort((r1, r2) -> r2.getCreationDate().compareTo(r1.getCreationDate()));
        }

        return reports;
    }

    /**
     * Class to hold report information
     */
    public static class ReportInfo {
        private String fileName;
        private String filePath;
        private long fileSize;
        private Date creationDate;
        private String reportType;

        // Getters and setters
        public String getFileName() { return fileName; }
        public void setFileName(String fileName) { this.fileName = fileName; }
        public String getFilePath() { return filePath; }
        public void setFilePath(String filePath) { this.filePath = filePath; }
        public long getFileSize() { return fileSize; }
        public void setFileSize(long fileSize) { this.fileSize = fileSize; }
        public Date getCreationDate() { return creationDate; }
        public void setCreationDate(Date creationDate) { this.creationDate = creationDate; }
        public String getReportType() { return reportType; }
        public void setReportType(String reportType) { this.reportType = reportType; }
    }
}
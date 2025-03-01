package com.tpcgrp.p6ebs.controller;

import com.tpcgrp.p6ebs.service.ConfigurationService;
import com.tpcgrp.p6ebs.service.integration.*;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.springframework.stereotype.Controller;

import java.io.File;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.*;

@Controller
public class IntegrationController {

    private final IntegrationService integrationService;
    private final ValidationService validationService;
    private final SynchronizationManager syncManager;
    private final ReportGenerator reportGenerator;
    private final SchedulerService schedulerService;
    private final NotificationService notificationService;
    private final ConfigurationService configService;
    public TabPane integrationTabs;


    @FXML
    private TableView<IntegrationHistoryItem> historyTable;

    @FXML
    private ComboBox<String> integrationTypeCombo;

    @FXML
    private TextArea integrationLogArea;

    @FXML
    private ProgressBar integrationProgressBar;

    @FXML
    private Label statusLabel;

    @FXML
    private Button startIntegrationButton;

    @FXML
    private Button cancelIntegrationButton;

    @FXML
    private Button schedulerConfigButton;

    @FXML
    private TabPane settingsTabs;

    @FXML
    private CheckBox enableScheduling;

    @FXML
    private Spinner<Integer> intervalHoursSpinner;

    @FXML
    private ComboBox<String> syncDirectionCombo;

    @FXML
    private TableView<ScheduledTaskItem> scheduledTasksTable;

    public IntegrationController(IntegrationService integrationService,
                                 ValidationService validationService,
                                 SynchronizationManager syncManager,
                                 ReportGenerator reportGenerator,
                                 SchedulerService schedulerService,
                                 NotificationService notificationService,
                                 ConfigurationService configService) {
        this.integrationService = integrationService;
        this.validationService = validationService;
        this.syncManager = syncManager;
        this.reportGenerator = reportGenerator;
        this.schedulerService = schedulerService;
        this.notificationService = notificationService;
        this.configService = configService;
    }

    @FXML
    public void initialize() {
        // Initialize integration type combo
        integrationTypeCombo.setItems(FXCollections.observableArrayList(
                "projectFinancials", "resourceManagement", "procurement", "timesheet", "projectWbs"
        ));
        integrationTypeCombo.getSelectionModel().selectFirst();

        // Initialize sync direction combo
        syncDirectionCombo.setItems(FXCollections.observableArrayList(
                "P6_TO_EBS", "EBS_TO_P6", "BIDIRECTIONAL"
        ));
        syncDirectionCombo.getSelectionModel().selectFirst();

        // Setup interval spinner
        intervalHoursSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 168, 24));

        // Initialize history table
        setupHistoryTable();

        // Initialize scheduled tasks table
        setupScheduledTasksTable();

        // Initial UI state
        cancelIntegrationButton.setDisable(true);
        integrationProgressBar.setProgress(0);
        integrationProgressBar.setVisible(false);

        integrationTypeCombo.setItems(FXCollections.observableArrayList(
                "projectFinancials", "resourceManagement", "procurement",
                "timesheet", "projectWbs", "ebsTasksToP6"
        ));

        // Update the history and scheduler tables
        refreshHistoryTable();
        refreshScheduledTasksTable();

        integrationLogArea.appendText("Integration panel initialized\n");
    }

    private void setupHistoryTable() {
        // Set up history table columns
        TableColumn<IntegrationHistoryItem, String> typeCol = new TableColumn<>("Type");
        typeCol.setCellValueFactory(new PropertyValueFactory<>("integrationType"));

        TableColumn<IntegrationHistoryItem, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));

        TableColumn<IntegrationHistoryItem, String> startTimeCol = new TableColumn<>("Start Time");
        startTimeCol.setCellValueFactory(new PropertyValueFactory<>("startTime"));

        TableColumn<IntegrationHistoryItem, String> durationCol = new TableColumn<>("Duration");
        durationCol.setCellValueFactory(new PropertyValueFactory<>("duration"));

        TableColumn<IntegrationHistoryItem, Integer> entitiesCol = new TableColumn<>("Entities");
        entitiesCol.setCellValueFactory(new PropertyValueFactory<>("entitiesProcessed"));

        historyTable.getColumns().addAll(typeCol, statusCol, startTimeCol, durationCol, entitiesCol);
    }

    private void setupScheduledTasksTable() {
        // Set up scheduled tasks table columns
        TableColumn<ScheduledTaskItem, String> typeCol = new TableColumn<>("Type");
        typeCol.setCellValueFactory(new PropertyValueFactory<>("integrationType"));

        TableColumn<ScheduledTaskItem, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));

        TableColumn<ScheduledTaskItem, String> intervalCol = new TableColumn<>("Interval");
        intervalCol.setCellValueFactory(new PropertyValueFactory<>("interval"));

        TableColumn<ScheduledTaskItem, String> lastRunCol = new TableColumn<>("Last Run");
        lastRunCol.setCellValueFactory(new PropertyValueFactory<>("lastRun"));

        TableColumn<ScheduledTaskItem, String> nextRunCol = new TableColumn<>("Next Run");
        nextRunCol.setCellValueFactory(new PropertyValueFactory<>("nextRun"));

        scheduledTasksTable.getColumns().addAll(typeCol, statusCol, intervalCol, lastRunCol, nextRunCol);
    }

    private void refreshHistoryTable() {
        // Get synchronization history from manager
        List<SynchronizationManager.SyncRecord> history = syncManager.getSyncHistory(null);

        // Convert to table items
        List<IntegrationHistoryItem> items = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        for (SynchronizationManager.SyncRecord record : history) {
            IntegrationHistoryItem item = new IntegrationHistoryItem();
            item.setIntegrationType(record.getSyncType());
            item.setStatus(record.getStatus());
            item.setStartTime(sdf.format(record.getStartTime()));

            // Calculate duration in seconds
            long durationSec = record.getDurationMs() / 1000;
            item.setDuration(durationSec + " sec");

            item.setEntitiesProcessed(record.getEntitiesProcessed());
            items.add(item);
        }

        // Update table
        historyTable.setItems(FXCollections.observableArrayList(items));
    }

    private void refreshScheduledTasksTable() {
        // Get scheduled tasks info
        Map<String, SchedulerService.ScheduleInfo> scheduledTasks = schedulerService.getAllScheduledTasks();

        // Convert to table items
        List<ScheduledTaskItem> items = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        for (Map.Entry<String, SchedulerService.ScheduleInfo> entry : scheduledTasks.entrySet()) {
            SchedulerService.ScheduleInfo info = entry.getValue();
            ScheduledTaskItem item = new ScheduledTaskItem();

            item.setIntegrationType(info.getIntegrationType());
            item.setStatus(info.isActive() ? "Active" : "Inactive");
            item.setInterval("Every " + info.getIntervalHours() + " hours");

            item.setLastRun(info.getLastRun() != null ? sdf.format(info.getLastRun()) : "Never");
            item.setNextRun(info.getNextRun() != null ? sdf.format(info.getNextRun()) : "N/A");

            items.add(item);
        }

        // Update table
        scheduledTasksTable.setItems(FXCollections.observableArrayList(items));
    }

    @FXML
    public void startIntegration() {
        String integrationType = integrationTypeCombo.getValue();

        if (integrationType == null || integrationType.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Error", "Please select an integration type");
            return;
        }

        // Get connection parameters from configuration
        try {
            ConfigurationService.Configuration config = configService.loadConfiguration();

            // Prepare connection parameters
            Map<String, String> p6Params = new HashMap<>();
            p6Params.put("server", config.getP6Server());
            p6Params.put("database", config.getP6Database());
            p6Params.put("username", config.getP6Username());
            p6Params.put("password", config.getP6Password());

            Map<String, String> ebsParams = new HashMap<>();
            ebsParams.put("server", config.getEbsServer());
            ebsParams.put("sid", config.getEbsSid());
            ebsParams.put("username", config.getEbsUsername());
            ebsParams.put("password", config.getEbsPassword());

            // Update UI state
            startIntegrationButton.setDisable(true);
            cancelIntegrationButton.setDisable(false);
            integrationProgressBar.setVisible(true);
            statusLabel.setText("Running integration: " + integrationType);
            integrationLogArea.appendText("Starting integration for: " + integrationType + "\n");

            // Run integration in background thread
            new Thread(() -> {
                try {
                    Map<String, Object> result = integrationService.startIntegration(
                            p6Params,
                            ebsParams,
                            Collections.singletonList(integrationType),
                            this::updateProgress
                    );

                    // Handle the result
                    Platform.runLater(() -> {
                        String status = (String) result.get("status");
                        if ("success".equals(status)) {
                            integrationLogArea.appendText("Integration completed successfully\n");
                            showAlert(Alert.AlertType.INFORMATION, "Success", "Integration completed successfully");

                            // Send success notification
                            notificationService.sendSuccessNotification(integrationType, result);
                        } else {
                            String errorMsg = (String) result.get("message");
                            integrationLogArea.appendText("Integration failed: " + errorMsg + "\n");
                            showAlert(Alert.AlertType.ERROR, "Error", "Integration failed: " + errorMsg);

                            // Send failure notification
                            notificationService.sendFailureNotification(integrationType, errorMsg);
                        }

                        // Reset UI state
                        startIntegrationButton.setDisable(false);
                        cancelIntegrationButton.setDisable(true);
                        integrationProgressBar.setProgress(0);
                        integrationProgressBar.setVisible(false);
                        statusLabel.setText("Ready");

                        // Refresh history table
                        refreshHistoryTable();
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> {
                        integrationLogArea.appendText("Integration error: " + e.getMessage() + "\n");
                        showAlert(Alert.AlertType.ERROR, "Error", "Integration error: " + e.getMessage());

                        // Reset UI state
                        startIntegrationButton.setDisable(false);
                        cancelIntegrationButton.setDisable(true);
                        integrationProgressBar.setProgress(0);
                        integrationProgressBar.setVisible(false);
                        statusLabel.setText("Ready");
                    });
                }
            }).start();

        } catch (Exception e) {
            integrationLogArea.appendText("Error loading configuration: " + e.getMessage() + "\n");
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to load configuration: " + e.getMessage());
        }
    }

    @FXML
    public void cancelIntegration() {
        if (integrationService.cancelIntegration()) {
            integrationLogArea.appendText("Integration cancelled by user\n");

            // Reset UI state
            startIntegrationButton.setDisable(false);
            cancelIntegrationButton.setDisable(true);
            integrationProgressBar.setProgress(0);
            integrationProgressBar.setVisible(false);
            statusLabel.setText("Cancelled");
        }
    }

    @FXML
    public void runValidation() {
        String integrationType = integrationTypeCombo.getValue();

        if (integrationType == null || integrationType.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Error", "Please select an integration type");
            return;
        }

        // Get connection parameters from configuration
        try {
            ConfigurationService.Configuration config = configService.loadConfiguration();

            // Prepare connection parameters
            Map<String, String> p6Params = new HashMap<>();
            p6Params.put("server", config.getP6Server());
            p6Params.put("database", config.getP6Database());
            p6Params.put("username", config.getP6Username());
            p6Params.put("password", config.getP6Password());

            Map<String, String> ebsParams = new HashMap<>();
            ebsParams.put("server", config.getEbsServer());
            ebsParams.put("sid", config.getEbsSid());
            ebsParams.put("username", config.getEbsUsername());
            ebsParams.put("password", config.getEbsPassword());

            integrationLogArea.appendText("Running validation for: " + integrationType + "\n");
            statusLabel.setText("Validating: " + integrationType);

            // Run validation in background thread
            new Thread(() -> {
                try {
                    List<ValidationService.ValidationIssue> issues = validationService.validateForIntegration(
                            p6Params, ebsParams, integrationType);

                    ValidationService.ValidationReport report = validationService.generateValidationReport(issues);

                    // Send notification with validation report
                    notificationService.sendValidationNotification(integrationType, report);

                    // Update UI on JavaFX thread
                    Platform.runLater(() -> {
                        integrationLogArea.appendText("Validation completed. Found " +
                                report.getTotalIssues() + " issues (" +
                                report.getBlockingIssues() + " blocking, " +
                                report.getWarningIssues() + " warnings)\n");

                        statusLabel.setText("Validation complete");

                        // Show validation results
                        showValidationResults(report);
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> {
                        integrationLogArea.appendText("Validation error: " + e.getMessage() + "\n");
                        showAlert(Alert.AlertType.ERROR, "Error", "Validation error: " + e.getMessage());
                        statusLabel.setText("Validation failed");
                    });
                }
            }).start();

        } catch (Exception e) {
            integrationLogArea.appendText("Error loading configuration: " + e.getMessage() + "\n");
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to load configuration: " + e.getMessage());
        }
    }

    private void showValidationResults(ValidationService.ValidationReport report) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Validation Results");
        alert.setHeaderText("Validation Report");

        StringBuilder content = new StringBuilder();
        content.append("Total issues: ").append(report.getTotalIssues()).append("\n");
        content.append("Blocking issues: ").append(report.getBlockingIssues()).append("\n");
        content.append("Warning issues: ").append(report.getWarningIssues()).append("\n\n");

        if (report.getIssues() != null && !report.getIssues().isEmpty()) {
            content.append("Issues:\n");

            for (ValidationService.ValidationIssue issue : report.getIssues()) {
                content.append("- ").append(issue.isBlocking() ? "[BLOCKING] " : "[WARNING] ")
                        .append(issue.getEntityType()).append(": ")
                        .append(issue.getDescription()).append("\n");
            }
        } else {
            content.append("No issues found.");
        }

        // Use a TextArea for scrollable content
        TextArea textArea = new TextArea(content.toString());
        textArea.setEditable(false);
        textArea.setWrapText(true);
        textArea.setPrefHeight(300);
        textArea.setPrefWidth(500);

        alert.getDialogPane().setContent(textArea);
        alert.showAndWait();
    }

    @FXML
    public void generateReport() {
        String integrationType = integrationTypeCombo.getValue();

        if (integrationType == null || integrationType.isEmpty()) {
            // Generate summary report if no specific type is selected
            integrationLogArea.appendText("Generating summary report...\n");

            new Thread(() -> {
                File report = reportGenerator.generateSummaryReport();

                Platform.runLater(() -> {
                    if (report != null) {
                        integrationLogArea.appendText("Report generated: " + report.getPath() + "\n");
                        askToOpenReport(report);
                    } else {
                        integrationLogArea.appendText("Failed to generate report\n");
                        showAlert(Alert.AlertType.ERROR, "Error", "Failed to generate report");
                    }
                });
            }).start();
        } else {
            // Generate detailed report for selected type
            integrationLogArea.appendText("Generating detailed report for " + integrationType + "...\n");

            new Thread(() -> {
                File report = reportGenerator.generateDetailedReport(integrationType);

                Platform.runLater(() -> {
                    if (report != null) {
                        integrationLogArea.appendText("Report generated: " + report.getPath() + "\n");
                        askToOpenReport(report);
                    } else {
                        integrationLogArea.appendText("Failed to generate report\n");
                        showAlert(Alert.AlertType.ERROR, "Error", "Failed to generate report");
                    }
                });
            }).start();
        }
    }

    private void askToOpenReport(File report) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Report Generated");
        alert.setHeaderText("Report generated successfully");
        alert.setContentText("Would you like to save it to a different location?");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Save Report");
            fileChooser.setInitialFileName(report.getName());
            fileChooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("Text Files", "*.txt")
            );

            File saveFile = fileChooser.showSaveDialog(new Stage());
            if (saveFile != null) {
                try {
                    Files.copy(report.toPath(), saveFile.toPath(),
                            java.nio.file.StandardCopyOption.REPLACE_EXISTING);

                    integrationLogArea.appendText("Report saved to: " + saveFile.getPath() + "\n");
                } catch (Exception e) {
                    integrationLogArea.appendText("Error saving report: " + e.getMessage() + "\n");
                    showAlert(Alert.AlertType.ERROR, "Error", "Failed to save report: " + e.getMessage());
                }
            }
        }
    }

    @FXML
    public void saveSchedulerConfig() {
        String integrationType = integrationTypeCombo.getValue();

        if (integrationType == null || integrationType.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Error", "Please select an integration type");
            return;
        }

        int intervalHours = intervalHoursSpinner.getValue();
        boolean enabled = enableScheduling.isSelected();

        // Update scheduler
        if (enabled) {
            schedulerService.scheduleIntegration(integrationType, intervalHours);
            integrationLogArea.appendText("Scheduled integration for " + integrationType +
                    " every " + intervalHours + " hours\n");
        } else {
            schedulerService.cancelScheduledIntegration(integrationType);
            integrationLogArea.appendText("Cancelled scheduled integration for " + integrationType + "\n");
        }

        // Refresh table
        refreshScheduledTasksTable();
    }

    @FXML
    public void runNow() {
        String integrationType = integrationTypeCombo.getValue();

        if (integrationType == null || integrationType.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Error", "Please select an integration type");
            return;
        }

        integrationLogArea.appendText("Manually triggering integration for " + integrationType + "\n");
        schedulerService.runIntegrationNow(integrationType);
    }

    /**
     * Callback for integration progress updates
     */
    private void updateProgress(int currentStep, int totalSteps, String statusMessage) {
        Platform.runLater(() -> {
            double progress = (double) currentStep / totalSteps;
            integrationProgressBar.setProgress(progress);
            statusLabel.setText(statusMessage);
            integrationLogArea.appendText(statusMessage + "\n");
        });
    }

    @FXML
    public void selectSchedulerTab() {
        // Select the scheduler tab
        integrationTabs.getSelectionModel().select(1); // 1 is the index of the Scheduler tab
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    /**
     * Data class for history table
     */
    public static class IntegrationHistoryItem {
        private String integrationType;
        private String status;
        private String startTime;
        private String duration;
        private int entitiesProcessed;

        // Getters and setters
        public String getIntegrationType() { return integrationType; }
        public void setIntegrationType(String integrationType) { this.integrationType = integrationType; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getStartTime() { return startTime; }
        public void setStartTime(String startTime) { this.startTime = startTime; }
        public String getDuration() { return duration; }
        public void setDuration(String duration) { this.duration = duration; }
        public int getEntitiesProcessed() { return entitiesProcessed; }
        public void setEntitiesProcessed(int entitiesProcessed) { this.entitiesProcessed = entitiesProcessed; }
    }

    /**
     * Data class for scheduled tasks table
     */
    public static class ScheduledTaskItem {
        private String integrationType;
        private String status;
        private String interval;
        private String lastRun;
        private String nextRun;

        // Getters and setters
        public String getIntegrationType() { return integrationType; }
        public void setIntegrationType(String integrationType) { this.integrationType = integrationType; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getInterval() { return interval; }
        public void setInterval(String interval) { this.interval = interval; }
        public String getLastRun() { return lastRun; }
        public void setLastRun(String lastRun) { this.lastRun = lastRun; }
        public String getNextRun() { return nextRun; }
        public void setNextRun(String nextRun) { this.nextRun = nextRun; }
    }
}
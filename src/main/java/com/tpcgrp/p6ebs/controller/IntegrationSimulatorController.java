package com.tpcgrp.p6ebs.controller;

import com.tpcgrp.p6ebs.service.ConfigurationService;
import com.tpcgrp.p6ebs.service.DatabaseService;
import com.tpcgrp.p6ebs.service.integration.*;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.springframework.stereotype.Controller;

import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Controller
public class IntegrationSimulatorController {

    private final DatabaseService databaseService;
    private final ConfigurationService configService;
    private final IntegrationLogService logService;
    private final SynchronizationManager syncManager;
    private final MappingUtility mappingUtility;
    private final DataTransformationService transformationService;

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    @FXML private ComboBox<String> integrationTypeCombo;
    @FXML private ComboBox<String> syncDirectionCombo;
    @FXML private CheckBox useSampleDataCheck;
    @FXML private Spinner<Integer> entityCountSpinner;
    @FXML private TextField filterTextField;
    @FXML private CheckBox simulateErrorsCheck;
    @FXML private Slider errorRateSlider;
    @FXML private Label errorRateLabel;

    @FXML private TableView<EntityRecord> dataPreviewTable;
    @FXML private TextArea simulationLogArea;
    @FXML private ProgressBar simulationProgressBar;
    @FXML private Label statusLabel;

    @FXML private Button startSimulationButton;
    @FXML private Button cancelSimulationButton;
    @FXML private Button saveLogsButton;

    // Simulation state
    private List<EntityRecord> sampleEntities = new ArrayList<>();
    private boolean simulationRunning = false;
    private Random random = new Random();

    public IntegrationSimulatorController(DatabaseService databaseService,
                                          ConfigurationService configService,
                                          IntegrationLogService logService,
                                          SynchronizationManager syncManager,
                                          MappingUtility mappingUtility,
                                          DataTransformationService transformationService) {
        this.databaseService = databaseService;
        this.configService = configService;
        this.logService = logService;
        this.syncManager = syncManager;
        this.mappingUtility = mappingUtility;
        this.transformationService = transformationService;
    }

    @FXML
    public void initialize() {
        setupIntegrationTypeCombo();
        setupSyncDirectionCombo();
        setupEntityCountSpinner();
        setupErrorControls();
        setupDataTable();

        // Initial UI state
        simulationProgressBar.setProgress(0);
        simulationProgressBar.setVisible(false);
        cancelSimulationButton.setDisable(true);
        saveLogsButton.setDisable(true);

        // Initialize with sample data
        useSampleDataCheck.setSelected(true);
        generateSampleData();

        logMessage("Simulator initialized");
    }

    private void setupIntegrationTypeCombo() {
        ObservableList<String> integrationTypes = FXCollections.observableArrayList(
                "Project Financials", "Resource Management", "Procurement", "Timesheet", "Project/WBS"
        );
        integrationTypeCombo.setItems(integrationTypes);
        integrationTypeCombo.getSelectionModel().selectFirst();

        // When integration type changes, update sample data
        integrationTypeCombo.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldVal, newVal) -> {
                    if (useSampleDataCheck.isSelected()) {
                        generateSampleData();
                    }
                }
        );
    }

    private void setupSyncDirectionCombo() {
        ObservableList<String> directions = FXCollections.observableArrayList(
                "P6 to EBS", "EBS to P6", "Bidirectional"
        );
        syncDirectionCombo.setItems(directions);
        syncDirectionCombo.getSelectionModel().selectFirst();
    }

    private void setupEntityCountSpinner() {
        entityCountSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 1000, 10));

        // Update sample data when count changes
        entityCountSpinner.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (useSampleDataCheck.isSelected()) {
                generateSampleData();
            }
        });

        // Handle checkbox for sample data
        useSampleDataCheck.selectedProperty().addListener((obs, oldVal, newVal) -> {
            entityCountSpinner.setDisable(!newVal);
            if (newVal) {
                generateSampleData();
            } else {
                loadRealData();
            }
        });
    }

    private void setupErrorControls() {
        // Setup error controls
        errorRateSlider.setMin(0);
        errorRateSlider.setMax(100);
        errorRateSlider.setValue(10);

        // Update label when slider value changes
        errorRateSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            errorRateLabel.setText(String.format("%.0f%%", newVal));
        });

        // Set initial value
        errorRateLabel.setText("10%");

        // Enable/disable error rate slider based on checkbox
        simulateErrorsCheck.selectedProperty().addListener((obs, oldVal, newVal) -> {
            errorRateSlider.setDisable(!newVal);
            errorRateLabel.setDisable(!newVal);
        });

        // Initially disable error rate slider
        errorRateSlider.setDisable(true);
        errorRateLabel.setDisable(true);
    }

    private void setupDataTable() {
        // Setup data preview table
        TableColumn<EntityRecord, String> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));

        TableColumn<EntityRecord, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));

        TableColumn<EntityRecord, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));

        TableColumn<EntityRecord, String> p6ValueCol = new TableColumn<>("P6 Value");
        p6ValueCol.setCellValueFactory(new PropertyValueFactory<>("p6Value"));

        TableColumn<EntityRecord, String> ebsValueCol = new TableColumn<>("EBS Value");
        ebsValueCol.setCellValueFactory(new PropertyValueFactory<>("ebsValue"));

        dataPreviewTable.getColumns().addAll(idCol, nameCol, statusCol, p6ValueCol, ebsValueCol);
    }

    private void generateSampleData() {
        int count = entityCountSpinner.getValue();
        String type = integrationTypeCombo.getValue();
        sampleEntities.clear();

        // Generate sample entities
        for (int i = 1; i <= count; i++) {
            EntityRecord entity = new EntityRecord();
            entity.setId("ENT-" + i);
            entity.setName(type + " " + i);

            // Random status
            String[] statuses = {"Active", "Completed", "On Hold", "Cancelled"};
            entity.setStatus(statuses[random.nextInt(statuses.length)]);

            // Sample values specific to the integration type
            switch (type) {
                case "Project Financials":
                    double budget = 10000 + random.nextInt(90000);
                    double spend = budget * (0.5 + (random.nextDouble() * 0.5));
                    entity.setP6Value(String.format("$%.2f", budget));
                    entity.setEbsValue(String.format("$%.2f", spend));
                    break;
                case "Resource Management":
                    int hours = 20 + random.nextInt(80);
                    entity.setP6Value(hours + " hours");
                    entity.setEbsValue((hours + (random.nextBoolean() ? 2 : -2)) + " hours");
                    break;
                case "Procurement":
                    int items = 1 + random.nextInt(10);
                    entity.setP6Value(items + " items");
                    entity.setEbsValue(items + " items");
                    break;
                case "Timesheet":
                    double p6Hours = 40 + (random.nextDouble() * 20);
                    double ebsHours = p6Hours + (random.nextDouble() * 4 - 2);
                    entity.setP6Value(String.format("%.1f hours", p6Hours));
                    entity.setEbsValue(String.format("%.1f hours", ebsHours));
                    break;
                case "Project/WBS":
                    int tasks = 5 + random.nextInt(15);
                    int ebsTasks = tasks + (random.nextBoolean() ? 1 : -1);
                    entity.setP6Value(tasks + " tasks");
                    entity.setEbsValue(ebsTasks + " tasks");
                    break;
            }

            sampleEntities.add(entity);
        }

        // Apply filter if specified
        applyDataFilter();
    }

    private void loadRealData() {
        // This would load actual data from the systems
        // For now, we'll just clear the data
        sampleEntities.clear();
        dataPreviewTable.setItems(FXCollections.observableArrayList(sampleEntities));
        logMessage("Real data connection would be implemented here");
        statusLabel.setText("No data loaded");
    }

    @FXML
    public void filterData() {
        applyDataFilter();
    }

    private void applyDataFilter() {
        String filter = filterTextField.getText().toLowerCase();

        if (filter.isEmpty()) {
            // No filter, show all data
            dataPreviewTable.setItems(FXCollections.observableArrayList(sampleEntities));
            return;
        }

        // Apply filter to sample entities
        List<EntityRecord> filtered = sampleEntities.stream()
                .filter(e -> e.getId().toLowerCase().contains(filter) ||
                        e.getName().toLowerCase().contains(filter) ||
                        e.getStatus().toLowerCase().contains(filter) ||
                        e.getP6Value().toLowerCase().contains(filter) ||
                        e.getEbsValue().toLowerCase().contains(filter))
                .collect(Collectors.toList());

        dataPreviewTable.setItems(FXCollections.observableArrayList(filtered));
    }

    @FXML
    public void startSimulation() {
        if (simulationRunning) {
            return;
        }

        // Get simulation parameters
        String integrationType = integrationTypeCombo.getValue();
        String direction = syncDirectionCombo.getValue();
        boolean simulateErrors = simulateErrorsCheck.isSelected();
        double errorRate = simulateErrors ? errorRateSlider.getValue() / 100.0 : 0;

        // Update UI state
        simulationRunning = true;
        startSimulationButton.setDisable(true);
        cancelSimulationButton.setDisable(false);
        simulationProgressBar.setVisible(true);
        simulationProgressBar.setProgress(0);
        simulationLogArea.clear();
        saveLogsButton.setDisable(true);

        logMessage("Starting " + integrationType + " simulation (" + direction + ")");
        statusLabel.setText("Simulation running...");

        // Run simulation in background
        executorService.submit(() -> {
            try {
                // Simulate the integration process
                simulateIntegration(integrationType, direction, errorRate);

                // Update UI when complete
                Platform.runLater(() -> {
                    simulationRunning = false;
                    startSimulationButton.setDisable(false);
                    cancelSimulationButton.setDisable(true);
                    saveLogsButton.setDisable(false);
                    simulationProgressBar.setProgress(1.0);
                    statusLabel.setText("Simulation completed");
                });

            } catch (Exception e) {
                // Handle errors
                Platform.runLater(() -> {
                    logMessage("Error: " + e.getMessage());
                    simulationRunning = false;
                    startSimulationButton.setDisable(false);
                    cancelSimulationButton.setDisable(true);
                    saveLogsButton.setDisable(true);
                    simulationProgressBar.setProgress(0);
                    statusLabel.setText("Simulation failed");
                });
            }
        });
    }

    private void simulateIntegration(String integrationType, String direction, double errorRate) {
        try {
            // Calculate total steps
            int totalSteps = 5; // Initialize, validate, process, verify, finalize
            int currentStep = 0;

            // Step 1: Initialize
            updateProgress(++currentStep, totalSteps, "Initializing simulation...");
            Thread.sleep(500);

            logMessage("Simulation parameters:");
            logMessage("- Type: " + integrationType);
            logMessage("- Direction: " + direction);
            logMessage("- Error Rate: " + (errorRate * 100) + "%");
            logMessage("- Entity Count: " + sampleEntities.size());

            // Step 2: Validation phase
            updateProgress(++currentStep, totalSteps, "Validating entities...");
            Thread.sleep(1000);

            int validationIssues = simulateValidation(errorRate);
            if (validationIssues > 0) {
                logMessage("Found " + validationIssues + " validation issues");
                if (validationIssues > sampleEntities.size() / 2) {
                    logMessage("WARNING: High number of validation issues detected");
                }
            } else {
                logMessage("Validation successful - no issues found");
            }

            // Step 3: Processing phase
            updateProgress(++currentStep, totalSteps, "Processing entities...");

            int totalEntities = sampleEntities.size();
            int processedCount = 0;
            int successCount = 0;
            int errorCount = 0;

            // Process each entity
            for (EntityRecord entity : sampleEntities) {
                processedCount++;

                // Update progress more frequently during this step
                double subProgress = (double) processedCount / totalEntities;
                double overallProgress = (currentStep - 1 + subProgress) / totalSteps;
                updateProgressNoMessage(overallProgress);

                // Process the entity
                boolean success = processEntity(entity, errorRate);

                if (success) {
                    successCount++;
                } else {
                    errorCount++;
                }

                // Simulate processing time
                Thread.sleep(100);
            }

            // Step 4: Verification phase
            updateProgress(++currentStep, totalSteps, "Verifying results...");
            Thread.sleep(800);

            // Calculate verification statistics
            int mismatches = simulateVerification(errorRate);

            logMessage("Verification complete");
            if (mismatches > 0) {
                logMessage("- " + mismatches + " entities require manual review");
            } else {
                logMessage("- All entities synchronized successfully");
            }

            // Step 5: Finalization phase
            updateProgress(++currentStep, totalSteps, "Finalizing simulation...");
            Thread.sleep(500);

            // Summarize results
            logMessage("Simulation completed");
            logMessage("- Processed entities: " + processedCount);
            logMessage("- Successful: " + successCount);
            logMessage("- Errors: " + errorCount);
            logMessage("- Matched Entities: " + (processedCount - errorCount - mismatches));
            logMessage("- Updated Entities: " + (processedCount - errorCount));

            // Update status
            updateProgress(totalSteps, totalSteps, "Simulation completed");

        } catch (InterruptedException e) {
            logMessage("Simulation interrupted");
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            logMessage("Simulation error: " + e.getMessage());
            throw new RuntimeException("Simulation failed", e);
        }
    }

    private int simulateValidation(double errorRate) {
        // Simulate validation issues based on error rate
        int totalEntities = sampleEntities.size();
        int issueCount = (int) Math.round(totalEntities * errorRate * 0.5); // Half the error rate for validation

        for (int i = 0; i < Math.min(issueCount, 5); i++) {
            // Log some sample validation issues
            logMessage("Validation: Entity " + sampleEntities.get(random.nextInt(totalEntities)).getId() +
                    " has incomplete data");
        }

        return issueCount;
    }

    private boolean processEntity(EntityRecord entity, double errorRate) {
        // Simulate processing success/failure based on error rate
        boolean success = random.nextDouble() > errorRate;

        if (success) {
            // Processing successful
            logMessage("Processed: " + entity.getId() + " - " + entity.getName());

            // Sometimes log detailed operations
            if (random.nextDouble() < 0.3) {
                String operation = random.nextBoolean() ? "Updated" : "Synchronized";
                logMessage("  - " + operation + " " + entity.getId() + " values between systems");
            }
        } else {
            // Processing failed
            String errorType = random.nextInt(3) == 0 ? "Connection error" :
                    (random.nextInt(2) == 0 ? "Data validation error" : "Permission denied");

            logMessage("ERROR: Failed to process " + entity.getId() + " - " + errorType);
        }

        return success;
    }

    private int simulateVerification(double errorRate) {
        // Simulate verification mismatches
        int totalEntities = sampleEntities.size();
        int mismatchCount = (int) Math.round(totalEntities * errorRate * 0.3); // 30% of error rate for verification

        for (int i = 0; i < Math.min(mismatchCount, 3); i++) {
            // Log some sample verification issues
            EntityRecord entity = sampleEntities.get(random.nextInt(totalEntities));
            logMessage("Verification: Mismatch detected in " + entity.getId() + " - values don't match between systems");
        }

        return mismatchCount;
    }

    @FXML
    public void cancelSimulation() {
        if (!simulationRunning) {
            return;
        }

        logMessage("Simulation cancelled by user");
        simulationRunning = false;
        startSimulationButton.setDisable(false);
        cancelSimulationButton.setDisable(true);
        simulationProgressBar.setProgress(0);
        statusLabel.setText("Simulation cancelled");
    }

    @FXML
    public void saveLogs() {
        if (simulationLogArea.getText().isEmpty()) {
            showAlert("No logs to save", "There are no simulation logs to save.");
            return;
        }

        // Create a file chooser
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Simulation Logs");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Text Files", "*.txt")
        );

        // Set default file name
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        fileChooser.setInitialFileName("simulation_log_" + timestamp + ".txt");

        // Show save dialog
        File file = fileChooser.showSaveDialog(new Stage());

        if (file != null) {
            try {
                // Write log content to file
                try (FileWriter writer = new FileWriter(file)) {
                    writer.write("P6-EBS INTEGRATION SIMULATION LOG\n");
                    writer.write("Generated: " + new Date() + "\n");
                    writer.write("Integration Type: " + integrationTypeCombo.getValue() + "\n");
                    writer.write("Direction: " + syncDirectionCombo.getValue() + "\n");
                    writer.write("=================================================\n\n");
                    writer.write(simulationLogArea.getText());
                }

                showAlert("Logs Saved", "Simulation logs saved to:\n" + file.getAbsolutePath());
            } catch (Exception e) {
                showAlert("Error", "Failed to save logs: " + e.getMessage());
            }
        }
    }

    @FXML
    public void showHelp() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Integration Simulator Help");
        alert.setHeaderText("Integration Simulator");

        String helpText = "The Integration Simulator allows you to test P6-EBS integration scenarios " +
                "without affecting production data.\n\n" +

                "Key Features:\n" +
                "- Test different integration types and directions\n" +
                "- Use sample data or connect to real systems in read-only mode\n" +
                "- Simulate errors at specified rates to test error handling\n" +
                "- View detailed logs of the integration process\n" +
                "- Export logs for analysis or documentation\n\n" +

                "How to Use:\n" +
                "1. Select integration type and direction\n" +
                "2. Choose between sample data or real data\n" +
                "3. Configure error simulation if desired\n" +
                "4. Click 'Start Simulation' to begin\n" +
                "5. Review logs and results\n" +
                "6. Save logs if needed\n\n" +

                "This simulator is a safe environment for testing integration scenarios " +
                "and training users without risk to production systems.";

        // Use a TextArea for scrollable content
        TextArea textArea = new TextArea(helpText);
        textArea.setEditable(false);
        textArea.setWrapText(true);
        textArea.setPrefHeight(300);
        textArea.setPrefWidth(500);

        alert.getDialogPane().setContent(textArea);
        alert.showAndWait();
    }

    private void logMessage(String message) {
        String timestamp = dateFormat.format(new Date());
        Platform.runLater(() -> {
            simulationLogArea.appendText(timestamp + " - " + message + "\n");
            // Scroll to bottom
            simulationLogArea.setScrollTop(Double.MAX_VALUE);
        });
    }

    private void updateProgress(int currentStep, int totalSteps, String message) {
        double progress = (double) currentStep / totalSteps;
        Platform.runLater(() -> {
            simulationProgressBar.setProgress(progress);
            statusLabel.setText(message);
            logMessage(message);
        });
    }

    private void updateProgressNoMessage(double progress) {
        Platform.runLater(() -> {
            simulationProgressBar.setProgress(progress);
        });
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Data class for entity records in the simulation
     */
    public static class EntityRecord {
        private String id;
        private String name;
        private String status;
        private String p6Value;
        private String ebsValue;

        // Getters and setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public String getP6Value() { return p6Value; }
        public void setP6Value(String p6Value) { this.p6Value = p6Value; }

        public String getEbsValue() { return ebsValue; }
        public void setEbsValue(String ebsValue) { this.ebsValue = ebsValue; }
    }
}
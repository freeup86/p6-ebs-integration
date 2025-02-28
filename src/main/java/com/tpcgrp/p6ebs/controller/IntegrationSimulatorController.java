package com.tpcgrp.p6ebs.controller;

import com.tpcgrp.p6ebs.service.simulation.IntegrationSimulatorService;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.MapValueFactory;
import javafx.scene.control.cell.PropertyValueFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@Controller
public class IntegrationSimulatorController {

    // Logger definition
    private static final Logger logger = LoggerFactory.getLogger(IntegrationSimulatorController.class);

    private final IntegrationSimulatorService simulatorService;

    @FXML private ComboBox<String> entityTypeCombo;
    @FXML private ComboBox<String> directionCombo;
    @FXML private Button simulateButton;
    @FXML private TextArea simulationLogArea;
    @FXML private TableView<Map<String, Object>> resultsTable;
    @FXML private TableView<Map<String, Object>> p6DataTable;
    @FXML private TableView<Map<String, Object>> ebsDataTable;
    @FXML private ProgressBar simulationProgressBar;

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public IntegrationSimulatorController(IntegrationSimulatorService simulatorService) {
        this.simulatorService = simulatorService;
    }

    @FXML
    public void initialize() {
        logger.info("Initializing Integration Simulator Controller");

        // Initialize combos
        entityTypeCombo.setItems(FXCollections.observableArrayList(
                "projects", "activities", "resources"
        ));
        entityTypeCombo.getSelectionModel().selectFirst();

        directionCombo.setItems(FXCollections.observableArrayList(
                "P6_TO_EBS", "EBS_TO_P6"
        ));
        directionCombo.getSelectionModel().selectFirst();

        // Setup tables
        setupResultsTable();
        setupP6DataTable();
        setupEbsDataTable();

        // Generate initial sample data
        //generateSampleData();
    }

    private void setupResultsTable() {
        // Clear existing columns
        resultsTable.getColumns().clear();
    }

    private void setupP6DataTable() {
        // Clear existing columns
        p6DataTable.getColumns().clear();
    }

    private void setupEbsDataTable() {
        // Clear existing columns
        ebsDataTable.getColumns().clear();
    }

    @FXML
    public void generateSampleData() {
        logger.info("Generating sample data triggered from UI");

        simulatorService.generateSampleData();

        // Populate data tables
        updateDataTables();

        showAlert(Alert.AlertType.INFORMATION, "Sample Data",
                "Sample integration data has been generated.");
    }

    private void updateDataTables() {
        Platform.runLater(() -> {
            // Update P6 Data Table
            updateDataTable(p6DataTable, "projects", true);
            updateDataTable(p6DataTable, "activities", true);
            updateDataTable(p6DataTable, "resources", true);

            // Update EBS Data Table
            updateDataTable(ebsDataTable, "projects", false);
            updateDataTable(ebsDataTable, "activities", false);
            updateDataTable(ebsDataTable, "resources", false);
        });
    }

    private void updateDataTable(TableView<Map<String, Object>> tableView, String entityType, boolean isP6) {
        // Clear existing columns
        tableView.getColumns().clear();

        // Get data from simulator service
        List<IntegrationSimulatorService.SimulatedEntity> entities =
                simulatorService.getSampleData(entityType, isP6);

        if (entities.isEmpty()) {
            return;
        }

        // Collect all unique keys from all entities
        Set<String> allKeys = new LinkedHashSet<>();
        allKeys.add("id");
        allKeys.add("name");
        entities.forEach(entity -> {
            allKeys.add("id");
            allKeys.add("name");
            allKeys.addAll(entity.getData().keySet());
        });

        // Create columns for each unique key
        for (String key : allKeys) {
            TableColumn<Map<String, Object>, Object> column = new TableColumn<>(key);
            column.setCellValueFactory(cellData -> {
                Map<String, Object> rowData = cellData.getValue();
                return new SimpleObjectProperty<>(rowData.get(key));
            });
            tableView.getColumns().add(column);
        }

        // Convert entities to observable list of maps
        ObservableList<Map<String, Object>> dataList = FXCollections.observableArrayList();
        for (IntegrationSimulatorService.SimulatedEntity entity : entities) {
            Map<String, Object> dataMap = new HashMap<>(entity.getData());
            dataMap.put("id", entity.getId());
            dataMap.put("name", entity.getName());
            dataList.add(dataMap);
        }

        // Set data to table
        tableView.setItems(dataList);
    }

    @FXML
    public void runSimulation() {
        // Validate inputs
        String entityType = entityTypeCombo.getValue();
        String direction = directionCombo.getValue();

        if (entityType == null || direction == null) {
            showAlert(Alert.AlertType.WARNING, "Validation Error",
                    "Please select an entity type and direction.");
            return;
        }

        // Disable UI during simulation
        simulateButton.setDisable(true);
        simulationProgressBar.setVisible(true);
        simulationProgressBar.setProgress(-1); // Indeterminate progress

        // Run simulation in background thread
        new Thread(() -> {
            try {
                // Log start of simulation
                logSimulationMessage("Starting simulation for " + entityType +
                        " in " + direction + " direction");

                // Run simulation
                IntegrationSimulatorService.SimulationResult result =
                        simulatorService.simulateIntegration(entityType, direction);

                // Update UI on JavaFX thread
                Platform.runLater(() -> {
                    // Add result to table
                    resultsTable.getItems().add((Map<String, Object>) result);

                    // Log simulation details
                    logSimulationMessage("Simulation completed:");
                    logSimulationMessage("- Matched Entities: " + result.getMatchedEntities());
                    logSimulationMessage("- Updated Entities: " + result.getUpdatedEntities());
                    logSimulationMessage("- Status: " + result.getStatus());

                    // Reset UI
                    simulateButton.setDisable(false);
                    simulationProgressBar.setVisible(false);
                });
            } catch (Exception e) {
                // Handle any exceptions
                Platform.runLater(() -> {
                    logSimulationMessage("Simulation failed: " + e.getMessage());
                    showAlert(Alert.AlertType.ERROR, "Simulation Error",
                            "An error occurred during simulation: " + e.getMessage());

                    // Reset UI
                    simulateButton.setDisable(false);
                    simulationProgressBar.setVisible(false);
                });
            }
        }).start();
    }

    // Helper method to log simulation messages
    private void logSimulationMessage(String message) {
        Platform.runLater(() -> {
            simulationLogArea.appendText(
                    dateFormat.format(new Date()) + " - " + message + "\n"
            );
        });
    }

    // Helper method to show alerts
    private void showAlert(Alert.AlertType type, String title, String content) {
        Platform.runLater(() -> {
            Alert alert = new Alert(type);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(content);
            alert.showAndWait();
        });
    }
}
package com.tpcgrp.p6ebs.controller;

import com.tpcgrp.p6ebs.service.ConfigurationService;
import com.tpcgrp.p6ebs.service.DatabaseService;
import com.tpcgrp.p6ebs.service.P6ActivityService;
import com.tpcgrp.p6ebs.service.integration.*;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.springframework.stereotype.Controller;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Controller
public class ReconciliationController {

    private final DatabaseService databaseService;
    private final ConfigurationService configService;
    private final P6ActivityService p6ActivityService;
    private final ReportGenerator reportGenerator;
    private final IntegrationLogService logService;
    private final SchedulerService schedulerService;
    private final MappingUtility mappingUtility;

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    // UI Components
    @FXML private ComboBox<String> entityTypeCombo;
    @FXML private ComboBox<String> filterFieldCombo;
    @FXML private TextField filterValueField;
    @FXML private ComboBox<String> discrepancyTypeCombo;

    @FXML private Label discrepancyCountLabel;
    @FXML private ProgressIndicator loadingIndicator;
    @FXML private TableView<DiscrepancyRecord> discrepancyTable;

    @FXML private TableView<Map<String, Object>> p6DataTable;
    @FXML private TableView<Map<String, Object>> ebsDataTable;

    @FXML private Label selectedEntityLabel;
    @FXML private ComboBox<String> resolutionActionCombo;
    @FXML private TableView<FieldDiscrepancy> resolutionTable;

    @FXML private ProgressBar progressBar;
    @FXML private Label statusLabel;

    // Data storage
    private List<Map<String, Object>> p6Data = new ArrayList<>();
    private List<Map<String, Object>> ebsData = new ArrayList<>();
    private List<DiscrepancyRecord> discrepancies = new ArrayList<>();
    private DiscrepancyRecord selectedDiscrepancy = null;

    public ReconciliationController(DatabaseService databaseService,
                                    ConfigurationService configService,
                                    P6ActivityService p6ActivityService,
                                    ReportGenerator reportGenerator,
                                    IntegrationLogService logService,
                                    SchedulerService schedulerService,
                                    MappingUtility mappingUtility) {
        this.databaseService = databaseService;
        this.configService = configService;
        this.p6ActivityService = p6ActivityService;
        this.reportGenerator = reportGenerator;
        this.logService = logService;
        this.schedulerService = schedulerService;
        this.mappingUtility = mappingUtility;
    }

    @FXML
    public void initialize() {
        setupEntityTypeCombo();
        setupDiscrepancyTypeCombo();
        setupResolutionActionCombo();
        setupTables();

        // Add listener for entity selection in discrepancy table
        discrepancyTable.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldSelection, newSelection) -> {
                    if (newSelection != null) {
                        handleDiscrepancySelection(newSelection);
                    }
                }
        );
    }

    private void setupEntityTypeCombo() {
        // Set up entity type options
        ObservableList<String> entityTypes = FXCollections.observableArrayList(
                "Project", "Activity", "Resource", "WBS"
        );
        entityTypeCombo.setItems(entityTypes);
        entityTypeCombo.getSelectionModel().selectFirst();

        // When entity type changes, update the filter field options
        entityTypeCombo.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldVal, newVal) -> updateFilterFields(newVal)
        );

        // Initialize filter fields
        updateFilterFields(entityTypeCombo.getValue());
    }

    private void updateFilterFields(String entityType) {
        List<String> filterFields = new ArrayList<>();

        // Add appropriate filter fields based on entity type
        switch (entityType) {
            case "Project":
                filterFields.addAll(Arrays.asList("ID", "Code", "Name", "Status"));
                break;
            case "Activity":
                filterFields.addAll(Arrays.asList("ID", "Project ID", "Code", "Name", "Status"));
                break;
            case "Resource":
                filterFields.addAll(Arrays.asList("ID", "Name", "Email", "Department"));
                break;
            case "WBS":
                filterFields.addAll(Arrays.asList("ID", "Project ID", "Code", "Name"));
                break;
        }

        filterFieldCombo.setItems(FXCollections.observableArrayList(filterFields));
        filterFieldCombo.getSelectionModel().selectFirst();
    }

    private void setupDiscrepancyTypeCombo() {
        ObservableList<String> discrepancyTypes = FXCollections.observableArrayList(
                "All Discrepancies", "Missing in P6", "Missing in EBS", "Value Mismatch"
        );
        discrepancyTypeCombo.setItems(discrepancyTypes);
        discrepancyTypeCombo.getSelectionModel().selectFirst();
    }

    private void setupResolutionActionCombo() {
        ObservableList<String> resolutionActions = FXCollections.observableArrayList(
                "Use P6 Value", "Use EBS Value", "Ignore", "Custom Value"
        );
        resolutionActionCombo.setItems(resolutionActions);
    }

    private void setupTables() {
        // Setup discrepancy table
        TableColumn<DiscrepancyRecord, String> entityIdCol = new TableColumn<>("Entity ID");
        entityIdCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getEntityId()));

        TableColumn<DiscrepancyRecord, String> entityNameCol = new TableColumn<>("Entity Name");
        entityNameCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getEntityName()));

        TableColumn<DiscrepancyRecord, String> discrepancyTypeCol = new TableColumn<>("Discrepancy Type");
        discrepancyTypeCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getDiscrepancyType()));

        TableColumn<DiscrepancyRecord, Integer> fieldCountCol = new TableColumn<>("Field Count");
        fieldCountCol.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().getFieldDiscrepancies().size()));

        TableColumn<DiscrepancyRecord, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getStatus()));

        discrepancyTable.getColumns().addAll(entityIdCol, entityNameCol, discrepancyTypeCol, fieldCountCol, statusCol);

        // Setup resolution table
        TableColumn<FieldDiscrepancy, String> fieldNameCol = new TableColumn<>("Field");
        fieldNameCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getFieldName()));

        TableColumn<FieldDiscrepancy, String> p6ValueCol = new TableColumn<>("P6 Value");
        p6ValueCol.setCellValueFactory(cellData -> new SimpleStringProperty(
                cellData.getValue().getP6Value() != null ? cellData.getValue().getP6Value().toString() : "N/A"));

        TableColumn<FieldDiscrepancy, String> ebsValueCol = new TableColumn<>("EBS Value");
        ebsValueCol.setCellValueFactory(cellData -> new SimpleStringProperty(
                cellData.getValue().getEbsValue() != null ? cellData.getValue().getEbsValue().toString() : "N/A"));

        TableColumn<FieldDiscrepancy, String> resolutionCol = new TableColumn<>("Resolution");
        resolutionCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getResolution()));

        TableColumn<FieldDiscrepancy, CheckBox> selectCol = new TableColumn<>("Select");
        selectCol.setCellValueFactory(cellData -> {
            CheckBox checkBox = new CheckBox();
            checkBox.setSelected(cellData.getValue().isSelected());
            checkBox.selectedProperty().addListener((obs, oldVal, newVal) ->
                    cellData.getValue().setSelected(newVal));
            return new SimpleObjectProperty<>(checkBox);
        });

        resolutionTable.getColumns().addAll(selectCol, fieldNameCol, p6ValueCol, ebsValueCol, resolutionCol);
    }

    @FXML
    public void compareData() {
        String entityType = entityTypeCombo.getValue();
        String filterField = filterFieldCombo.getValue();
        String filterValue = filterValueField.getText();
        String discrepancyType = discrepancyTypeCombo.getValue();

        // Show loading indicators
        loadingIndicator.setVisible(true);
        progressBar.setVisible(true);
        progressBar.setProgress(-1); // Indeterminate
        statusLabel.setText("Loading data...");

        executorService.submit(() -> {
            try {
                // Load configuration
                ConfigurationService.Configuration config = configService.loadConfiguration();

                // Prepare P6 connection parameters
                Map<String, String> p6Params = new HashMap<>();
                p6Params.put("server", config.getP6Server());
                p6Params.put("database", config.getP6Database());
                p6Params.put("username", config.getP6Username());
                p6Params.put("password", config.getP6Password());

                // Prepare EBS connection parameters
                Map<String, String> ebsParams = new HashMap<>();
                ebsParams.put("server", config.getEbsServer());
                ebsParams.put("sid", config.getEbsSid());
                ebsParams.put("username", config.getEbsUsername());
                ebsParams.put("password", config.getEbsPassword());

                // Update status
                Platform.runLater(() -> statusLabel.setText("Fetching P6 data..."));

                // Fetch data from P6
                p6Data = fetchP6Data(p6Params, entityType, filterField, filterValue);

                // Update status
                Platform.runLater(() -> statusLabel.setText("Fetching EBS data..."));

                // Fetch data from EBS
                ebsData = fetchEbsData(ebsParams, entityType, filterField, filterValue);

                // Update status
                Platform.runLater(() -> statusLabel.setText("Analyzing discrepancies..."));

                // Find discrepancies
                discrepancies = findDiscrepancies(p6Data, ebsData, entityType);

                // Filter discrepancies based on type if needed
                if (!"All Discrepancies".equals(discrepancyType)) {
                    discrepancies = discrepancies.stream()
                            .filter(d -> d.getDiscrepancyType().equals(discrepancyType))
                            .collect(Collectors.toList());
                }

                // Update UI on JavaFX thread
                Platform.runLater(() -> {
                    // Update discrepancy table
                    discrepancyTable.setItems(FXCollections.observableArrayList(discrepancies));
                    discrepancyCountLabel.setText(discrepancies.size() + " discrepancies found");

                    // Set P6 data table
                    updateDataTable(p6DataTable, p6Data);

                    // Set EBS data table
                    updateDataTable(ebsDataTable, ebsData);

                    // Hide loading indicators
                    loadingIndicator.setVisible(false);
                    progressBar.setVisible(false);
                    statusLabel.setText("Ready");
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    logService.logInfo("Error comparing data: " + e.getMessage());
                    statusLabel.setText("Error: " + e.getMessage());
                    loadingIndicator.setVisible(false);
                    progressBar.setVisible(false);

                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Error");
                    alert.setHeaderText("Data Comparison Error");
                    alert.setContentText("Failed to compare data: " + e.getMessage());
                    alert.showAndWait();
                });
            }
        });
    }

    private List<Map<String, Object>> fetchP6Data(Map<String, String> p6Params,
                                                  String entityType,
                                                  String filterField,
                                                  String filterValue) throws Exception {
        // This would call the appropriate P6 service based on entity type
        // For this example, we'll simulate data
        List<Map<String, Object>> data = new ArrayList<>();

        // Simulated data for testing
        switch (entityType) {
            case "Project":
                for (int i = 1; i <= 10; i++) {
                    Map<String, Object> project = new HashMap<>();
                    project.put("proj_id", "P" + i);
                    project.put("proj_name", "Project " + i);
                    project.put("proj_short_name", "PROJ-" + i);
                    project.put("status_code", i % 3 == 0 ? "COMPLETED" : "IN_PROGRESS");
                    project.put("plan_start_date", new Date());
                    project.put("plan_end_date", new Date());
                    data.add(project);
                }
                break;
            case "Activity":
                // Similar simulated data for activities
                break;
            // Handle other entity types
        }

        // Apply filter if provided
        if (filterField != null && !filterField.isEmpty() && filterValue != null && !filterValue.isEmpty()) {
            // This is a simplified filter - real implementation would be more complex
            String fieldKey = mapUiFieldToApiField(entityType, filterField);
            data = data.stream()
                    .filter(m -> m.containsKey(fieldKey) &&
                            String.valueOf(m.get(fieldKey)).toLowerCase().contains(filterValue.toLowerCase()))
                    .collect(Collectors.toList());
        }

        return data;
    }

    private List<Map<String, Object>> fetchEbsData(Map<String, String> ebsParams,
                                                   String entityType,
                                                   String filterField,
                                                   String filterValue) throws Exception {
        // This would call the appropriate EBS service based on entity type
        // For this example, we'll simulate data
        List<Map<String, Object>> data = new ArrayList<>();

        // Simulated data for testing
        switch (entityType) {
            case "Project":
                for (int i = 1; i <= 12; i++) {
                    // Skip some records to simulate missing data
                    if (i == 2 || i == 8) continue;

                    Map<String, Object> project = new HashMap<>();
                    project.put("project_id", "P" + i);
                    // Introduce some differences from P6 data
                    project.put("project_name", i == 5 ? "EBS Project " + i : "Project " + i);
                    project.put("segment1", "PROJ-" + i);
                    project.put("project_status_code", i % 3 == 0 ? "COMPLETED" : "ACTIVE");
                    project.put("start_date", new Date());
                    project.put("completion_date", new Date());
                    data.add(project);
                }
                break;
            case "Activity":
                // Similar simulated data for activities
                break;
            // Handle other entity types
        }

        // Apply filter if provided
        if (filterField != null && !filterField.isEmpty() && filterValue != null && !filterValue.isEmpty()) {
            // This is a simplified filter - real implementation would be more complex
            String fieldKey = mapUiFieldToApiField(entityType, filterField);
            data = data.stream()
                    .filter(m -> m.containsKey(fieldKey) &&
                            String.valueOf(m.get(fieldKey)).toLowerCase().contains(filterValue.toLowerCase()))
                    .collect(Collectors.toList());
        }

        return data;
    }

    private String mapUiFieldToApiField(String entityType, String uiField) {
        // Map UI field names to API field names
        switch (entityType) {
            case "Project":
                switch (uiField) {
                    case "ID": return entityType.equals("Project") ? "proj_id" : "project_id";
                    case "Code": return entityType.equals("Project") ? "proj_short_name" : "segment1";
                    case "Name": return entityType.equals("Project") ? "proj_name" : "project_name";
                    case "Status": return entityType.equals("Project") ? "status_code" : "project_status_code";
                    default: return uiField.toLowerCase().replace(" ", "_");
                }
                // Handle other entity types
            default:
                return uiField.toLowerCase().replace(" ", "_");
        }
    }

    private List<DiscrepancyRecord> findDiscrepancies(List<Map<String, Object>> p6Data,
                                                      List<Map<String, Object>> ebsData,
                                                      String entityType) {
        List<DiscrepancyRecord> discrepancies = new ArrayList<>();

        // Create a map of field mappings between P6 and EBS
        Map<String, String> fieldMappings = mappingUtility.getFieldMappings(entityType.toLowerCase());

        // Get the key fields for the entity type
        String p6IdField = getEntityIdField(entityType, true);
        String ebsIdField = getEntityIdField(entityType, false);
        String p6NameField = getEntityNameField(entityType, true);
        String ebsNameField = getEntityNameField(entityType, false);

        // Create maps for easier lookup
        Map<String, Map<String, Object>> p6Map = new HashMap<>();
        for (Map<String, Object> p6Entity : p6Data) {
            if (p6Entity.containsKey(p6IdField)) {
                p6Map.put(String.valueOf(p6Entity.get(p6IdField)), p6Entity);
            }
        }

        Map<String, Map<String, Object>> ebsMap = new HashMap<>();
        for (Map<String, Object> ebsEntity : ebsData) {
            if (ebsEntity.containsKey(ebsIdField)) {
                ebsMap.put(String.valueOf(ebsEntity.get(ebsIdField)), ebsEntity);
            }
        }

        // Find entities in P6 that are missing in EBS
        for (String p6Id : p6Map.keySet()) {
            if (!ebsMap.containsKey(p6Id)) {
                Map<String, Object> p6Entity = p6Map.get(p6Id);

                DiscrepancyRecord record = new DiscrepancyRecord();
                record.setEntityId(p6Id);
                record.setEntityName(p6Entity.containsKey(p6NameField) ?
                        String.valueOf(p6Entity.get(p6NameField)) : "Unknown");
                record.setDiscrepancyType("Missing in EBS");
                record.setStatus("Unresolved");

                List<FieldDiscrepancy> fieldDiscrepancies = new ArrayList<>();
                for (Map.Entry<String, Object> entry : p6Entity.entrySet()) {
                    String p6Field = entry.getKey();

                    FieldDiscrepancy fieldDiscrepancy = new FieldDiscrepancy();
                    fieldDiscrepancy.setFieldName(p6Field);
                    fieldDiscrepancy.setP6Value(entry.getValue());
                    fieldDiscrepancy.setEbsValue(null);
                    fieldDiscrepancy.setResolution("Pending");

                    fieldDiscrepancies.add(fieldDiscrepancy);
                }

                record.setFieldDiscrepancies(fieldDiscrepancies);
                discrepancies.add(record);
            }
        }

        // Find entities in EBS that are missing in P6
        for (String ebsId : ebsMap.keySet()) {
            if (!p6Map.containsKey(ebsId)) {
                Map<String, Object> ebsEntity = ebsMap.get(ebsId);

                DiscrepancyRecord record = new DiscrepancyRecord();
                record.setEntityId(ebsId);
                record.setEntityName(ebsEntity.containsKey(ebsNameField) ?
                        String.valueOf(ebsEntity.get(ebsNameField)) : "Unknown");
                record.setDiscrepancyType("Missing in P6");
                record.setStatus("Unresolved");

                List<FieldDiscrepancy> fieldDiscrepancies = new ArrayList<>();
                for (Map.Entry<String, Object> entry : ebsEntity.entrySet()) {
                    String ebsField = entry.getKey();

                    FieldDiscrepancy fieldDiscrepancy = new FieldDiscrepancy();
                    fieldDiscrepancy.setFieldName(ebsField);
                    fieldDiscrepancy.setP6Value(null);
                    fieldDiscrepancy.setEbsValue(entry.getValue());
                    fieldDiscrepancy.setResolution("Pending");

                    fieldDiscrepancies.add(fieldDiscrepancy);
                }

                record.setFieldDiscrepancies(fieldDiscrepancies);
                discrepancies.add(record);
            }
        }

        // Find entities with value mismatches
        for (String id : p6Map.keySet()) {
            if (ebsMap.containsKey(id)) {
                Map<String, Object> p6Entity = p6Map.get(id);
                Map<String, Object> ebsEntity = ebsMap.get(id);

                List<FieldDiscrepancy> fieldDiscrepancies = new ArrayList<>();

                // Compare fields that exist in field mappings
                for (Map.Entry<String, String> mapping : fieldMappings.entrySet()) {
                    String p6Field = mapping.getKey();
                    String ebsField = mapping.getValue();

                    Object p6Value = p6Entity.get(p6Field);
                    Object ebsValue = ebsEntity.get(ebsField);

                    // Check for differences
                    if (!Objects.equals(p6Value, ebsValue)) {
                        FieldDiscrepancy fieldDiscrepancy = new FieldDiscrepancy();
                        fieldDiscrepancy.setFieldName(p6Field + " / " + ebsField);
                        fieldDiscrepancy.setP6Value(p6Value);
                        fieldDiscrepancy.setEbsValue(ebsValue);
                        fieldDiscrepancy.setResolution("Pending");

                        fieldDiscrepancies.add(fieldDiscrepancy);
                    }
                }

                // Only add if there are discrepancies
                if (!fieldDiscrepancies.isEmpty()) {
                    DiscrepancyRecord record = new DiscrepancyRecord();
                    record.setEntityId(id);
                    record.setEntityName(p6Entity.containsKey(p6NameField) ?
                            String.valueOf(p6Entity.get(p6NameField)) : "Unknown");
                    record.setDiscrepancyType("Value Mismatch");
                    record.setStatus("Unresolved");
                    record.setFieldDiscrepancies(fieldDiscrepancies);

                    discrepancies.add(record);
                }
            }
        }

        return discrepancies;
    }

    private String getEntityIdField(String entityType, boolean isP6) {
        switch (entityType) {
            case "Project":
                return isP6 ? "proj_id" : "project_id";
            case "Activity":
                return isP6 ? "activity_id" : "task_id";
            case "Resource":
                return isP6 ? "rsrc_id" : "person_id";
            case "WBS":
                return isP6 ? "wbs_id" : "wbs_id";
            default:
                return "id";
        }
    }

    private String getEntityNameField(String entityType, boolean isP6) {
        switch (entityType) {
            case "Project":
                return isP6 ? "proj_name" : "project_name";
            case "Activity":
                return isP6 ? "activity_name" : "task_name";
            case "Resource":
                return isP6 ? "rsrc_name" : "full_name";
            case "WBS":
                return isP6 ? "wbs_name" : "wbs_name";
            default:
                return "name";
        }
    }

    private void updateDataTable(TableView<Map<String, Object>> tableView, List<Map<String, Object>> data) {
        // Clear existing columns
        tableView.getColumns().clear();

        if (data.isEmpty()) {
            return;
        }

        // Create columns based on the first record
        Map<String, Object> firstRecord = data.get(0);
        for (String key : firstRecord.keySet()) {
            TableColumn<Map<String, Object>, String> column = new TableColumn<>(key);
            column.setCellValueFactory(cellData -> {
                Object value = cellData.getValue().get(key);
                return new SimpleStringProperty(value != null ? value.toString() : "");
            });
            tableView.getColumns().add(column);
        }

        // Set the data
        tableView.setItems(FXCollections.observableArrayList(data));
    }

    private void handleDiscrepancySelection(DiscrepancyRecord discrepancy) {
        selectedDiscrepancy = discrepancy;
        selectedEntityLabel.setText(discrepancy.getEntityId() + " - " + discrepancy.getEntityName());

        // Update resolution table
        resolutionTable.setItems(FXCollections.observableArrayList(discrepancy.getFieldDiscrepancies()));
    }

    @FXML
    public void clearFilters() {
        filterValueField.clear();
        entityTypeCombo.getSelectionModel().selectFirst();
        filterFieldCombo.getSelectionModel().selectFirst();
        discrepancyTypeCombo.getSelectionModel().selectFirst();
    }

    @FXML
    public void applyResolution() {
        if (selectedDiscrepancy == null) {
            showAlert("No discrepancy selected", "Please select a discrepancy first.");
            return;
        }

        String action = resolutionActionCombo.getValue();
        if (action == null || action.isEmpty()) {
            showAlert("No action selected", "Please select a resolution action.");
            return;
        }

        // Get selected field discrepancies
        List<FieldDiscrepancy> selectedFields = resolutionTable.getItems().stream()
                .filter(FieldDiscrepancy::isSelected)
                .collect(Collectors.toList());

        if (selectedFields.isEmpty()) {
            showAlert("No fields selected", "Please select at least one field to resolve.");
            return;
        }

        // Apply the resolution action to all selected fields
        for (FieldDiscrepancy field : selectedFields) {
            switch (action) {
                case "Use P6 Value":
                    field.setResolution("Use P6 Value");
                    break;
                case "Use EBS Value":
                    field.setResolution("Use EBS Value");
                    break;
                case "Ignore":
                    field.setResolution("Ignore");
                    break;
                case "Custom Value":
                    // This would show a dialog to enter a custom value
                    showCustomValueDialog(field);
                    break;
            }
        }

        // Refresh the resolution table
        resolutionTable.refresh();

        // Check if all fields are resolved
        boolean allResolved = selectedDiscrepancy.getFieldDiscrepancies().stream()
                .noneMatch(fd -> "Pending".equals(fd.getResolution()));

        if (allResolved) {
            selectedDiscrepancy.setStatus("Resolved");
            discrepancyTable.refresh();
        }
    }

    private void showCustomValueDialog(FieldDiscrepancy field) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Custom Value");
        dialog.setHeaderText("Enter custom value for field: " + field.getFieldName());
        dialog.setContentText("Value:");

        dialog.showAndWait().ifPresent(value -> {
            field.setResolution("Custom: " + value);
        });
    }

    @FXML
    public void resolveSelected() {
        if (selectedDiscrepancy == null) {
            showAlert("No discrepancy selected", "Please select a discrepancy first.");
            return;
        }

        // Apply resolutions to the backend
        applyResolutionsToBackend(Collections.singletonList(selectedDiscrepancy));
    }

    @FXML
    public void resolveAll() {
        // Get all resolved discrepancies
        List<DiscrepancyRecord> resolvedDiscrepancies = discrepancies.stream()
                .filter(d -> "Resolved".equals(d.getStatus()))
                .collect(Collectors.toList());

        if (resolvedDiscrepancies.isEmpty()) {
            showAlert("No resolved discrepancies", "There are no resolved discrepancies to apply.");
            return;
        }

        // Apply resolutions to the backend
        applyResolutionsToBackend(resolvedDiscrepancies);
    }

    private void applyResolutionsToBackend(List<DiscrepancyRecord> discrepancies) {
        // Show loading indicators
        progressBar.setVisible(true);
        progressBar.setProgress(-1); // Indeterminate
        statusLabel.setText("Applying resolutions...");

        executorService.submit(() -> {
            try {
                // Load configuration
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

                // For each discrepancy, apply resolutions
                int progressCount = 0;
                int totalCount = discrepancies.size();

                for (DiscrepancyRecord discrepancy : discrepancies) {
                    progressCount++;
                    final int currentProgress = progressCount;

                    // Update progress on UI thread
                    Platform.runLater(() -> {
                        progressBar.setProgress((double) currentProgress / totalCount);
                        statusLabel.setText("Processing " + currentProgress + " of " + totalCount + "...");
                    });

                    // Apply the resolution based on discrepancy type
                    switch (discrepancy.getDiscrepancyType()) {
                        case "Missing in P6":
                            // Create entity in P6
                            // This would call the appropriate P6 service
                            break;

                        case "Missing in EBS":
                            // Create entity in EBS
                            // This would call the appropriate EBS service
                            break;

                        case "Value Mismatch":
                            // Update values based on field resolutions
                            applyFieldResolutions(discrepancy, p6Params, ebsParams);
                            break;
                    }

                    // Mark as applied
                    discrepancy.setStatus("Applied");
                }

                // Update UI on completion
                Platform.runLater(() -> {
                    progressBar.setVisible(false);
                    statusLabel.setText("Resolutions applied successfully");
                    discrepancyTable.refresh();

                    showAlert("Resolutions Applied",
                            "Successfully applied resolutions for " + discrepancies.size() + " discrepancies.");
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    progressBar.setVisible(false);
                    statusLabel.setText("Error: " + e.getMessage());

                    showAlert("Error", "Failed to apply resolutions: " + e.getMessage());
                });
            }
        });
    }

    private void applyFieldResolutions(DiscrepancyRecord discrepancy,
                                       Map<String, String> p6Params,
                                       Map<String, String> ebsParams) {
        // This would update the appropriate system based on field resolutions
        String entityType = entityTypeCombo.getValue();
        String entityId = discrepancy.getEntityId();

        // Collect updates for P6 and EBS
        Map<String, Object> p6Updates = new HashMap<>();
        Map<String, Object> ebsUpdates = new HashMap<>();

        for (FieldDiscrepancy field : discrepancy.getFieldDiscrepancies()) {
            String resolution = field.getResolution();

            if (resolution.equals("Use P6 Value")) {
                // Update EBS with P6 value
                ebsUpdates.put(field.getFieldName(), field.getP6Value());
            } else if (resolution.equals("Use EBS Value")) {
                // Update P6 with EBS value
                p6Updates.put(field.getFieldName(), field.getEbsValue());
            } else if (resolution.startsWith("Custom: ")) {
                // Update both with custom value
                String customValue = resolution.substring("Custom: ".length());
                p6Updates.put(field.getFieldName(), customValue);
                ebsUpdates.put(field.getFieldName(), customValue);
            }
            // No action for "Ignore"
        }

        // Apply updates to P6 if needed
        if (!p6Updates.isEmpty()) {
            // This would call the appropriate P6 service to update the entity
            // updateP6Entity(entityType, entityId, p6Updates, p6Params);
        }

        // Apply updates to EBS if needed
        if (!ebsUpdates.isEmpty()) {
            // This would call the appropriate EBS service to update the entity
            // updateEbsEntity(entityType, entityId, ebsUpdates, ebsParams);
        }
    }

    @FXML
    public void exportReport() {
        if (discrepancies.isEmpty()) {
            showAlert("No Data", "There are no discrepancies to export.");
            return;
        }

        // Create a file chooser
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Reconciliation Report");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Text Files", "*.txt")
        );

        // Show save dialog
        File file = fileChooser.showSaveDialog(new Stage());

        if (file != null) {
            // Generate report in background
            executorService.submit(() -> {
                try {
                    // Show progress
                    Platform.runLater(() -> {
                        progressBar.setVisible(true);
                        progressBar.setProgress(-1);
                        statusLabel.setText("Generating report...");
                    });

                    // Generate reconciliation report
                    File report = generateReconciliationReport(file);

                    // Update UI on completion
                    Platform.runLater(() -> {
                        progressBar.setVisible(false);
                        statusLabel.setText("Report generated successfully");

                        showAlert("Report Generated",
                                "Reconciliation report saved to: " + report.getAbsolutePath());
                    });

                } catch (Exception e) {
                    Platform.runLater(() -> {
                        progressBar.setVisible(false);
                        statusLabel.setText("Error: " + e.getMessage());

                        showAlert("Error", "Failed to generate report: " + e.getMessage());
                    });
                }
            });
        }
    }

    private File generateReconciliationReport(File outputFile) throws IOException {
        // This would generate a detailed report of all discrepancies
        StringBuilder report = new StringBuilder();

        // Report header
        report.append("DATA RECONCILIATION REPORT\n");
        report.append("Generated: ").append(new Date()).append("\n");
        report.append("Entity Type: ").append(entityTypeCombo.getValue()).append("\n");
        report.append("Total Discrepancies: ").append(discrepancies.size()).append("\n");
        report.append("==================================================\n\n");

        // Group discrepancies by type
        Map<String, List<DiscrepancyRecord>> groupedDiscrepancies = discrepancies.stream()
                .collect(Collectors.groupingBy(DiscrepancyRecord::getDiscrepancyType));

        // For each type, list discrepancies
        for (Map.Entry<String, List<DiscrepancyRecord>> entry : groupedDiscrepancies.entrySet()) {
            String type = entry.getKey();
            List<DiscrepancyRecord> records = entry.getValue();

            report.append(type.toUpperCase()).append(" (").append(records.size()).append(")\n");
            report.append("--------------------------------------------------\n");

            for (DiscrepancyRecord record : records) {
                report.append("Entity: ").append(record.getEntityId())
                        .append(" - ").append(record.getEntityName()).append("\n");
                report.append("Status: ").append(record.getStatus()).append("\n");
                report.append("Field Discrepancies:\n");

                for (FieldDiscrepancy field : record.getFieldDiscrepancies()) {
                    report.append("  * ").append(field.getFieldName()).append("\n");
                    report.append("    P6: ").append(field.getP6Value()).append("\n");
                    report.append("    EBS: ").append(field.getEbsValue()).append("\n");
                    report.append("    Resolution: ").append(field.getResolution()).append("\n");
                }

                report.append("\n");
            }

            report.append("\n");
        }

        // Write to file
        java.nio.file.Files.write(outputFile.toPath(), report.toString().getBytes());

        return outputFile;
    }

    @FXML
    public void scheduleReconciliation() {
        TextInputDialog dialog = new TextInputDialog("24");
        dialog.setTitle("Schedule Reconciliation");
        dialog.setHeaderText("Schedule Regular Reconciliation");
        dialog.setContentText("Enter interval (hours):");

        dialog.showAndWait().ifPresent(intervalStr -> {
            try {
                int interval = Integer.parseInt(intervalStr);

                // Schedule reconciliation
                schedulerService.scheduleReconciliation(entityTypeCombo.getValue(), interval);

                showAlert("Reconciliation Scheduled",
                        "Reconciliation for " + entityTypeCombo.getValue() +
                                " entities scheduled every " + interval + " hours.");

            } catch (NumberFormatException e) {
                showAlert("Invalid Input", "Please enter a valid number for the interval.");
            }
        });
    }

    @FXML
    public void showHelp() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Reconciliation Tool Help");
        alert.setHeaderText("Data Reconciliation Tool");

        String helpText =
                "This tool helps identify and resolve discrepancies between P6 and EBS data.\n\n" +

                        "Usage Instructions:\n" +
                        "1. Select the entity type to reconcile (Project, Activity, etc.)\n" +
                        "2. Apply filters if needed\n" +
                        "3. Click 'Compare Data' to find discrepancies\n" +
                        "4. Review discrepancies in the table\n" +
                        "5. Select a discrepancy to see detailed field differences\n" +
                        "6. Choose how to resolve each field discrepancy\n" +
                        "7. Apply resolutions\n\n" +

                        "Discrepancy Types:\n" +
                        "- Missing in P6: Entity exists in EBS but not in P6\n" +
                        "- Missing in EBS: Entity exists in P6 but not in EBS\n" +
                        "- Value Mismatch: Entity exists in both systems but has different values\n\n" +

                        "Resolution Options:\n" +
                        "- Use P6 Value: Update EBS with the value from P6\n" +
                        "- Use EBS Value: Update P6 with the value from EBS\n" +
                        "- Ignore: Do not update either system\n" +
                        "- Custom Value: Enter a new value to update both systems";

        // Use a TextArea for scrollable content
        TextArea textArea = new TextArea(helpText);
        textArea.setEditable(false);
        textArea.setWrapText(true);
        textArea.setPrefHeight(300);
        textArea.setPrefWidth(500);

        alert.getDialogPane().setContent(textArea);
        alert.showAndWait();
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Data class representing a discrepancy between systems
     */
    public static class DiscrepancyRecord {
        private String entityId;
        private String entityName;
        private String discrepancyType;
        private String status;
        private List<FieldDiscrepancy> fieldDiscrepancies = new ArrayList<>();

        // Getters and setters
        public String getEntityId() { return entityId; }
        public void setEntityId(String entityId) { this.entityId = entityId; }

        public String getEntityName() { return entityName; }
        public void setEntityName(String entityName) { this.entityName = entityName; }

        public String getDiscrepancyType() { return discrepancyType; }
        public void setDiscrepancyType(String discrepancyType) { this.discrepancyType = discrepancyType; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public List<FieldDiscrepancy> getFieldDiscrepancies() { return fieldDiscrepancies; }
        public void setFieldDiscrepancies(List<FieldDiscrepancy> fieldDiscrepancies) {
            this.fieldDiscrepancies = fieldDiscrepancies;
        }
    }

    /**
     * Data class representing a field discrepancy
     */
    public static class FieldDiscrepancy {
        private String fieldName;
        private Object p6Value;
        private Object ebsValue;
        private String resolution;
        private boolean selected;

        // Getters and setters
        public String getFieldName() { return fieldName; }
        public void setFieldName(String fieldName) { this.fieldName = fieldName; }

        public Object getP6Value() { return p6Value; }
        public void setP6Value(Object p6Value) { this.p6Value = p6Value; }

        public Object getEbsValue() { return ebsValue; }
        public void setEbsValue(Object ebsValue) { this.ebsValue = ebsValue; }

        public String getResolution() { return resolution; }
        public void setResolution(String resolution) { this.resolution = resolution; }

        public boolean isSelected() { return selected; }
        public void setSelected(boolean selected) { this.selected = selected; }
    }
}
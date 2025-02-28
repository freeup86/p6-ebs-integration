package com.tpcgrp.p6ebs.controller;

import com.tpcgrp.p6ebs.service.ConfigurationService;
import com.tpcgrp.p6ebs.service.DatabaseService;
import com.tpcgrp.p6ebs.service.FileImportExportService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.context.ApplicationContext;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;

import java.io.File;
import java.net.URL;


@Controller
public class MainController {

    private DatabaseService databaseService;
    private final ConfigurationService configService;
    private ApplicationContext applicationContext;
    private IntegrationController integrationController;
    private FileImportExportService fileImportExportService;


    //@FXML
    //private IntegrationController integrationController;

    // FXML Field Declarations
    @FXML
    private VBox mainContainer;

    @FXML
    private MenuBar menuBar;

    @FXML
    private TabPane mainTabPane;

    @FXML
    private TextField p6Server;

    @FXML
    private TextField p6Database;

    @FXML
    private TextField p6Username;

    @FXML
    private PasswordField p6Password;

    @FXML
    private TextField ebsServer;

    @FXML
    private TextField ebsSid;

    @FXML
    private TextField ebsUsername;

    @FXML
    private PasswordField ebsPassword;

    @FXML
    private CheckBox projectFinancialsCheck;

    @FXML
    private CheckBox resourceManagementCheck;

    @FXML
    private CheckBox procurementCheck;

    @FXML
    private CheckBox timesheetCheck;

    @FXML
    private CheckBox projectWbsCheck;

    @FXML
    private TextArea logArea;

    @FXML
    private ProgressBar progressBar;

    @FXML
    private Label statusLabel;

    @FXML
    private Button startButton;

    @FXML
    private Button cancelButton;

    @FXML
    private ComboBox<String> logLevelComboBox;

    @FXML
    private DashboardController dashboardController;

    @FXML
    private ReconciliationController reconciliationController;

    // Constructor
    public MainController(DatabaseService databaseService, ConfigurationService configService, IntegrationController integrationController) {
        this.databaseService = databaseService;
        this.configService = configService;
        this.applicationContext = applicationContext;
        this.integrationController = integrationController;
    }

    @Autowired
    public MainController(
            FileImportExportService fileImportExportService,
            ConfigurationService configService,
            DatabaseService databaseService
    ) {
        this.fileImportExportService = fileImportExportService;
        this.configService = configService;
        this.databaseService = databaseService;
    }

    @FXML
    public void initialize() {
        // Add integration tab
        // Add this to your initialize method where you're loading the integration tab
        try {
            URL integrationFxmlUrl = getClass().getResource("/fxml/integration.fxml");
            if (integrationFxmlUrl == null) {
                logArea.appendText("Could not find integration.fxml resource\n");
                return; // Skip loading the integration tab if resource is missing
            }

            FXMLLoader loader = new FXMLLoader(integrationFxmlUrl);
            loader.setControllerFactory(applicationContext::getBean);

            Parent integrationView = loader.load();
            // Rest of your code...
        } catch (Exception e) {
            logArea.appendText("Error loading integration module: " + e.getMessage() + "\n");
            e.printStackTrace();
        }

        // Load dashboard view
        try {
            FXMLLoader dashboardLoader = new FXMLLoader(getClass().getResource("/fxml/dashboard.fxml"));
            dashboardLoader.setControllerFactory(applicationContext::getBean);

            Parent dashboardView = dashboardLoader.load();
            dashboardController = dashboardLoader.getController();

            // Create and add dashboard tab - make it the first tab
            Tab dashboardTab = new Tab("Dashboard");
            dashboardTab.setContent(dashboardView);
            dashboardTab.setClosable(false);

            mainTabPane.getTabs().add(0, dashboardTab);
            mainTabPane.getSelectionModel().select(dashboardTab);

            logArea.appendText("Dashboard loaded\n");
        } catch (Exception e) {
            logArea.appendText("Failed to load dashboard: " + e.getMessage() + "\n");
            e.printStackTrace();
        }

        //Load Reconciliation Service
        try {
            // Load reconciliation view
            FXMLLoader reconciliationLoader = new FXMLLoader(getClass().getResource("/fxml/reconciliation.fxml"));
            reconciliationLoader.setControllerFactory(applicationContext::getBean);

            Parent reconciliationView = reconciliationLoader.load();
            reconciliationController = reconciliationLoader.getController();

            // Create and add reconciliation tab
            Tab reconciliationTab = new Tab("Data Reconciliation");
            reconciliationTab.setContent(reconciliationView);
            reconciliationTab.setClosable(false);

            mainTabPane.getTabs().add(reconciliationTab);

            logArea.appendText("Data Reconciliation tool loaded\n");
        } catch (Exception e) {
            logArea.appendText("Failed to load reconciliation tool: " + e.getMessage() + "\n");
            e.printStackTrace();
        }

        initializeLogLevels();
        initializeButtons();
        setupProgressBar();
        loadConfiguration();
        logArea.appendText("Application initialized\n");
    }

    private void initializeLogLevels() {
        if (logLevelComboBox != null) {
            logLevelComboBox.getItems().addAll("DEBUG", "INFO", "WARN", "ERROR");
            logLevelComboBox.setValue("INFO");
        }
    }

    private void initializeButtons() {
        if (cancelButton != null) {
            cancelButton.setDisable(true);
        }
        if (startButton != null) {
            startButton.setDisable(false);
        }
    }

    private void setupProgressBar() {
        if (progressBar != null) {
            progressBar.setProgress(0);
            progressBar.setVisible(false);
        }
    }

    @FXML
    public void showSettings() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Settings");
        alert.setHeaderText(null);
        alert.setContentText("Settings dialog will be implemented soon.");
        alert.showAndWait();
    }

    @FXML
    public void exitApplication() {
        Platform.exit();
    }

    @FXML
    public void showAbout() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("About");
        alert.setHeaderText("P6-EBS Integration Tool");
        alert.setContentText("Version 1.0\nDeveloped by LIT Consulting");
        alert.showAndWait();
    }

    @FXML
    public void clearLog() {
        if (logArea != null) {
            logArea.clear();
            logArea.appendText("Log cleared\n");
        }
    }

    @FXML
    public void cancelOperation() {
        logArea.appendText("Operation cancelled\n");
        if (cancelButton != null) {
            cancelButton.setDisable(true);
        }
        if (startButton != null) {
            startButton.setDisable(false);
        }
        if (progressBar != null) {
            progressBar.setProgress(0);
            progressBar.setVisible(false);
        }
        if (statusLabel != null) {
            statusLabel.setText("Ready");
        }
    }

    @FXML
    public void exportLog() {
        logArea.appendText("Exporting log...\n");
        showAlert(Alert.AlertType.INFORMATION, "Export", "Log export will be implemented soon");
    }

    @FXML
    private P6ActivitiesController p6ActivitiesController;

    @FXML
    public void testP6Connection() {
        try {
            boolean success = databaseService.testP6Connection(
                    p6Server.getText(),
                    p6Database.getText(),
                    p6Username.getText(),
                    p6Password.getText()
            );

            if (success) {
                showAlert(Alert.AlertType.INFORMATION, "Success", "Successfully connected to P6 database");
                logArea.appendText("P6 connection test successful\n");

                // Pass connection parameters to P6ActivitiesController
                if (p6ActivitiesController != null) {
                    p6ActivitiesController.setConnectionParams(
                            p6Server.getText(),
                            p6Database.getText(),
                            p6Username.getText(),
                            p6Password.getText()
                    );
                }
            } else {
                showAlert(Alert.AlertType.ERROR, "Error", "Failed to connect to P6 database");
                logArea.appendText("P6 connection test failed\n");
            }
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Connection error: " + e.getMessage());
            logArea.appendText("P6 connection error: " + e.getMessage() + "\n");
        }
    }

    @FXML
    public void testEbsConnection() {
        try {
            boolean success = databaseService.testEbsConnection(
                    ebsServer.getText(),
                    ebsSid.getText(),
                    ebsUsername.getText(),
                    ebsPassword.getText()
            );

            if (success) {
                showAlert(Alert.AlertType.INFORMATION, "Success", "Successfully connected to EBS database");
                logArea.appendText("EBS connection test successful\n");
            } else {
                showAlert(Alert.AlertType.ERROR, "Error", "Failed to connect to EBS database");
                logArea.appendText("EBS connection test failed\n");
            }
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Connection error: " + e.getMessage());
            logArea.appendText("EBS connection error: " + e.getMessage() + "\n");
        }
    }

    @FXML
    public void loadConfiguration() {
        try {
            ConfigurationService.Configuration config = configService.loadConfiguration();

            // Load P6 settings
            p6Server.setText(config.getP6Server());
            p6Database.setText(config.getP6Database());
            p6Username.setText(config.getP6Username());
            p6Password.setText(config.getP6Password());

            // Load EBS settings
            ebsServer.setText(config.getEbsServer());
            ebsSid.setText(config.getEbsSid());
            ebsUsername.setText(config.getEbsUsername());
            ebsPassword.setText(config.getEbsPassword());

            // Load integration settings
            projectFinancialsCheck.setSelected(config.isProjectFinancialsEnabled());
            resourceManagementCheck.setSelected(config.isResourceManagementEnabled());
            procurementCheck.setSelected(config.isProcurementEnabled());
            timesheetCheck.setSelected(config.isTimesheetEnabled());
            projectWbsCheck.setSelected(config.isProjectWbsEnabled());

            logArea.appendText("Configuration loaded successfully\n");
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to load configuration: " + e.getMessage());
            logArea.appendText("Failed to load configuration: " + e.getMessage() + "\n");
        }
    }

    @FXML
    public void saveConfiguration() {
        try {
            ConfigurationService.Configuration config = new ConfigurationService.Configuration();

            // Set P6 settings
            config.setP6Server(p6Server.getText());
            config.setP6Database(p6Database.getText());
            config.setP6Username(p6Username.getText());
            config.setP6Password(p6Password.getText());

            // Set EBS settings
            config.setEbsServer(ebsServer.getText());
            config.setEbsSid(ebsSid.getText());
            config.setEbsUsername(ebsUsername.getText());
            config.setEbsPassword(ebsPassword.getText());

            // Set integration settings
            config.setProjectFinancialsEnabled(projectFinancialsCheck.isSelected());
            config.setResourceManagementEnabled(resourceManagementCheck.isSelected());
            config.setProcurementEnabled(procurementCheck.isSelected());
            config.setTimesheetEnabled(timesheetCheck.isSelected());
            config.setProjectWbsEnabled(projectWbsCheck.isSelected());

            configService.saveConfiguration(config);
            showAlert(Alert.AlertType.INFORMATION, "Success", "Configuration saved successfully");
            logArea.appendText("Configuration saved successfully\n");
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to save configuration: " + e.getMessage());
            logArea.appendText("Failed to save configuration: " + e.getMessage() + "\n");
        }
    }

    @FXML
    public void startIntegration() {
        if (!validateIntegrationSettings()) {
            return;
        }

        logArea.appendText("Starting integration process...\n");

        if (projectFinancialsCheck.isSelected()) {
            logArea.appendText("Processing Project Financials...\n");
        }
        if (resourceManagementCheck.isSelected()) {
            logArea.appendText("Processing Resource Management...\n");
        }
        if (procurementCheck.isSelected()) {
            logArea.appendText("Processing Procurement...\n");
        }
        if (timesheetCheck.isSelected()) {
            logArea.appendText("Processing Timesheet...\n");
        }
        if (projectWbsCheck.isSelected()) {
            logArea.appendText("Processing Project/WBS...\n");
        }

        showAlert(Alert.AlertType.INFORMATION, "Integration", "Integration process completed");
        logArea.appendText("Integration process completed\n");
    }

    private boolean validateIntegrationSettings() {
        if (!projectFinancialsCheck.isSelected() &&
                !resourceManagementCheck.isSelected() &&
                !procurementCheck.isSelected() &&
                !timesheetCheck.isSelected() &&
                !projectWbsCheck.isSelected()) {

            showAlert(Alert.AlertType.ERROR, "Error", "Please select at least one integration type");
            return false;
        }
        return true;
    }

    // Add Integration menu item
    @FXML
    public void showIntegrationSettings() {
        // Switch to the integration tab
        for (Tab tab : mainTabPane.getTabs()) {
            if (tab.getText().equals("Integration")) {
                mainTabPane.getSelectionModel().select(tab);
                break;
            }
        }
    }

    // Add to the menu handling methods to include integration functionality
    @FXML
    public void showMenu() {
        Menu menu = new Menu("Integration");

        MenuItem settingsItem = new MenuItem("Integration Settings");
        settingsItem.setOnAction(event -> showIntegrationSettings());

        MenuItem scheduleItem = new MenuItem("Scheduling");
        scheduleItem.setOnAction(event -> {
            showIntegrationSettings();
            // Select scheduler tab
            integrationController.selectSchedulerTab();
        });

        MenuItem reportItem = new MenuItem("Generate Reports");
        reportItem.setOnAction(event -> {
            showIntegrationSettings();
            integrationController.generateReport();
        });

        menu.getItems().addAll(settingsItem, scheduleItem, reportItem);
        menuBar.getMenus().add(menu);
    }

    @FXML
    public void importConfiguration() {
        try {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Import Configuration");
            fileChooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("JSON Files", "*.json")
            );
            File selectedFile = fileChooser.showOpenDialog(new Stage());

            if (selectedFile != null) {
                ConfigurationService.Configuration importedConfig =
                        fileImportExportService.importFromJson(
                                selectedFile.getAbsolutePath(),
                                ConfigurationService.Configuration.class
                        );

                configService.saveConfiguration(importedConfig);
                showAlert(Alert.AlertType.INFORMATION, "Import Successful",
                        "Configuration imported from " + selectedFile.getAbsolutePath());
            }
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Import Failed",
                    "Could not import configuration: " + e.getMessage());
        }
    }

    @FXML
    public void exportConfiguration() {
        try {
            ConfigurationService.Configuration config = configService.loadConfiguration();

            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Export Configuration");
            fileChooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("JSON Files", "*.json")
            );
            File selectedFile = fileChooser.showSaveDialog(new Stage());

            if (selectedFile != null) {
                fileImportExportService.exportToJson(config, selectedFile.getAbsolutePath());
                showAlert(Alert.AlertType.INFORMATION, "Export Successful",
                        "Configuration exported to " + selectedFile.getAbsolutePath());
            }
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Export Failed",
                    "Could not export configuration: " + e.getMessage());
        }
    }

    @FXML
    public void importData() {
        try {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Import Data");
            fileChooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("CSV Files", "*.csv"),
                    new FileChooser.ExtensionFilter("JSON Files", "*.json")
            );
            File selectedFile = fileChooser.showOpenDialog(new Stage());

            if (selectedFile != null) {
                String fileName = selectedFile.getName().toLowerCase();
                if (fileName.endsWith(".csv")) {
                    // Import from CSV
                    fileImportExportService.importFromCsv(selectedFile.getAbsolutePath());
                } else if (fileName.endsWith(".json")) {
                    // Import from JSON (generic import)
                    fileImportExportService.importFromJson(selectedFile.getAbsolutePath(), Object.class);
                }

                showAlert(Alert.AlertType.INFORMATION, "Import Successful",
                        "Data imported from " + selectedFile.getAbsolutePath());
            }
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Import Failed",
                    "Could not import data: " + e.getMessage());
        }
    }

    @FXML
    public void exportData() {
        try {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Export Data");
            fileChooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("CSV Files", "*.csv"),
                    new FileChooser.ExtensionFilter("Excel Files", "*.xlsx"),
                    new FileChooser.ExtensionFilter("JSON Files", "*.json")
            );
            File selectedFile = fileChooser.showSaveDialog(new Stage());

            if (selectedFile != null) {
                String fileName = selectedFile.getName().toLowerCase();
                // Get your data from appropriate services
                // This is a placeholder - replace with actual data retrieval
                java.util.List<java.util.Map<String, Object>> data = getDataForExport();

                if (fileName.endsWith(".csv")) {
                    fileImportExportService.exportToCsv(data, selectedFile.getAbsolutePath());
                } else if (fileName.endsWith(".xlsx")) {
                    fileImportExportService.exportToExcel(data, selectedFile.getAbsolutePath());
                } else if (fileName.endsWith(".json")) {
                    fileImportExportService.exportToJson(data, selectedFile.getAbsolutePath());
                }

                showAlert(Alert.AlertType.INFORMATION, "Export Successful",
                        "Data exported to " + selectedFile.getAbsolutePath());
            }
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Export Failed",
                    "Could not export data: " + e.getMessage());
        }
    }

    // Placeholder method to get data for export
    private java.util.List<java.util.Map<String, Object>> getDataForExport() {
        java.util.List<java.util.Map<String, Object>> data = new java.util.ArrayList<>();

        // Example data - replace with actual data retrieval from your services
        java.util.Map<String, Object> item1 = new java.util.HashMap<>();
        item1.put("ID", "001");
        item1.put("Name", "Sample Item 1");
        item1.put("Value", 100.50);

        java.util.Map<String, Object> item2 = new java.util.HashMap<>();
        item2.put("ID", "002");
        item2.put("Name", "Sample Item 2");
        item2.put("Value", 200.75);

        data.add(item1);
        data.add(item2);

        return data;
    }


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
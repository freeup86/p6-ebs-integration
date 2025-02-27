package com.tpcgrp.p6ebs.controller;

import com.tpcgrp.p6ebs.service.ConfigurationService;
import com.tpcgrp.p6ebs.service.DatabaseService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import org.springframework.stereotype.Controller;

@Controller
public class MainController {

    private final DatabaseService databaseService;
    private final ConfigurationService configService;

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

    // Constructor
    public MainController(DatabaseService databaseService, ConfigurationService configService) {
        this.databaseService = databaseService;
        this.configService = configService;
    }

    @FXML
    public void initialize() {
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
        alert.setContentText("Version 1.0\nDeveloped by TPC Group");
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
package com.tpcgrp.p6ebs.controller;

import com.tpcgrp.p6ebs.service.ConfigurationService;
import com.tpcgrp.p6ebs.service.FileImportExportService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.springframework.stereotype.Controller;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class FileImportExportController {

    private final FileImportExportService fileImportExportService;
    private final ConfigurationService configService;

    public FileImportExportController(
            FileImportExportService fileImportExportService,
            ConfigurationService configService) {
        this.fileImportExportService = fileImportExportService;
        this.configService = configService;
    }

    /**
     * Export configuration to JSON
     */
    @FXML
    public void exportConfigToJson() {
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

    /**
     * Import configuration from JSON
     */
    @FXML
    public void importConfigFromJson() {
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

    /**
     * Export data to CSV
     */
    @FXML
    public void exportDataToCsv() {
        try {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Export Data");
            fileChooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("CSV Files", "*.csv")
            );
            File selectedFile = fileChooser.showSaveDialog(new Stage());

            if (selectedFile != null) {
                // Example: Exporting projects - replace with actual data retrieval method
                List<Map<String, Object>> projectData = getProjectData();

                fileImportExportService.exportToCsv(projectData, selectedFile.getAbsolutePath());
                showAlert(Alert.AlertType.INFORMATION, "Export Successful",
                        "Data exported to " + selectedFile.getAbsolutePath());
            }
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Export Failed",
                    "Could not export data: " + e.getMessage());
        }
    }

    /**
     * Export data to Excel
     */
    @FXML
    public void exportDataToExcel() {
        try {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Export Data");
            fileChooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("Excel Files", "*.xlsx")
            );
            File selectedFile = fileChooser.showSaveDialog(new Stage());

            if (selectedFile != null) {
                // Example: Exporting projects - replace with actual data retrieval method
                List<Map<String, Object>> projectData = getProjectData();

                fileImportExportService.exportToExcel(projectData, selectedFile.getAbsolutePath());
                showAlert(Alert.AlertType.INFORMATION, "Export Successful",
                        "Data exported to " + selectedFile.getAbsolutePath());
            }
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Export Failed",
                    "Could not export data: " + e.getMessage());
        }
    }

    /**
     * Helper method to show alerts
     */
    private void showAlert(Alert.AlertType type, String title, String content) {
        Platform.runLater(() -> {
            Alert alert = new Alert(type);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(content);
            alert.showAndWait();
        });
    }

    /**
     * Placeholder method to get project data
     * Replace with actual data retrieval from your services
     */
    private List<Map<String, Object>> getProjectData() {
        // Placeholder implementation
        // In a real application, this would come from a service like P6ProjectService or EbsProjectService
        List<Map<String, Object>> projectData = new ArrayList<>();

        Map<String, Object> project1 = new HashMap<>();
        project1.put("Project ID", "P001");
        project1.put("Project Name", "Sample Project 1");
        project1.put("Start Date", "2024-01-01");
        project1.put("End Date", "2024-12-31");

        Map<String, Object> project2 = new HashMap<>();
        project2.put("Project ID", "P002");
        project2.put("Project Name", "Sample Project 2");
        project2.put("Start Date", "2024-02-15");
        project2.put("End Date", "2024-11-30");

        projectData.add(project1);
        projectData.add(project2);

        return projectData;
    }
}
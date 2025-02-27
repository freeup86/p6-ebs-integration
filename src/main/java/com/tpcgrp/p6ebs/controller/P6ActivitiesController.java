package com.tpcgrp.p6ebs.controller;

import com.tpcgrp.p6ebs.service.P6ActivityService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import org.springframework.stereotype.Controller;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
public class P6ActivitiesController {

    private final P6ActivityService p6ActivityService;

    @FXML
    private ComboBox<String> projectSelector;

    @FXML
    private TableView<ActivityData> activitiesTable;

    @FXML
    private TextArea logArea;

    @FXML
    private Button loadActivitiesBtn;

    @FXML
    private ProgressIndicator loadingIndicator;

    // Connection parameters - these could be passed from the main controller
    private String server;
    private String database;
    private String username;
    private String password;

    // Project ID to project name mapping
    private Map<String, String> projectMap;

    public P6ActivitiesController(P6ActivityService p6ActivityService) {
        this.p6ActivityService = p6ActivityService;
    }

    @FXML
    public void initialize() {
        setupTable();
        loadingIndicator.setVisible(false);
    }

    private void setupTable() {
        // Define table columns
        TableColumn<ActivityData, String> idCol = new TableColumn<>("Activity ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("activityId"));

        TableColumn<ActivityData, String> nameCol = new TableColumn<>("Activity Name");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("activityName"));

        TableColumn<ActivityData, String> codeCol = new TableColumn<>("Activity Code");
        codeCol.setCellValueFactory(new PropertyValueFactory<>("activityCode"));

        TableColumn<ActivityData, String> startCol = new TableColumn<>("Start Date");
        startCol.setCellValueFactory(new PropertyValueFactory<>("startDate"));

        TableColumn<ActivityData, String> finishCol = new TableColumn<>("Finish Date");
        finishCol.setCellValueFactory(new PropertyValueFactory<>("finishDate"));

        TableColumn<ActivityData, String> durationCol = new TableColumn<>("Duration");
        durationCol.setCellValueFactory(new PropertyValueFactory<>("duration"));

        TableColumn<ActivityData, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("statusCode"));

        TableColumn<ActivityData, String> wbsCol = new TableColumn<>("WBS");
        wbsCol.setCellValueFactory(new PropertyValueFactory<>("wbsName"));

        activitiesTable.getColumns().addAll(idCol, nameCol, codeCol, startCol, finishCol,
                durationCol, statusCol, wbsCol);
    }

    public void setConnectionParams(String server, String database, String username, String password) {
        this.server = server;
        this.database = database;
        this.username = username;
        this.password = password;

        // Load projects after setting connection parameters
        loadProjects();
    }

    private void loadProjects() {
        loadingIndicator.setVisible(true);

        new Thread(() -> {
            try {
                List<Map<String, Object>> projects = p6ActivityService.getAllProjects(
                        server, database, username, password);

                // Create mapping from project ID to name
                projectMap = projects.stream()
                        .collect(Collectors.toMap(
                                p -> p.get("proj_id").toString(),
                                p -> p.get("proj_name").toString()
                        ));

                // Update UI on JavaFX thread
                Platform.runLater(() -> {
                    projectSelector.setItems(FXCollections.observableArrayList(projectMap.values()));
                    loadingIndicator.setVisible(false);
                    logArea.appendText("Loaded " + projects.size() + " projects\n");
                });
            } catch (SQLException e) {
                Platform.runLater(() -> {
                    loadingIndicator.setVisible(false);
                    logArea.appendText("Error loading projects: " + e.getMessage() + "\n");
                    showAlert(Alert.AlertType.ERROR, "Error", "Failed to load projects: " + e.getMessage());
                });
            }
        }).start();
    }

    @FXML
    public void loadActivities() {
        String selectedProject = projectSelector.getValue();
        if (selectedProject == null) {
            showAlert(Alert.AlertType.WARNING, "Warning", "Please select a project first");
            return;
        }

        // Find project ID from name
        String projectId = projectMap.entrySet().stream()
                .filter(entry -> entry.getValue().equals(selectedProject))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);

        if (projectId == null) {
            logArea.appendText("Could not find project ID for: " + selectedProject + "\n");
            return;
        }

        loadingIndicator.setVisible(true);
        loadActivitiesBtn.setDisable(true);

        new Thread(() -> {
            try {
                List<Map<String, Object>> activities = p6ActivityService.getActivitiesByProject(
                        server, database, username, password, projectId);

                // Convert to ActivityData objects
                ObservableList<ActivityData> activityData = FXCollections.observableArrayList(
                        activities.stream()
                                .map(this::mapToActivityData)
                                .collect(Collectors.toList())
                );

                // Update UI on JavaFX thread
                Platform.runLater(() -> {
                    activitiesTable.setItems(activityData);
                    loadingIndicator.setVisible(false);
                    loadActivitiesBtn.setDisable(false);
                    logArea.appendText("Loaded " + activities.size() + " activities for project: " + selectedProject + "\n");
                });
            } catch (SQLException e) {
                Platform.runLater(() -> {
                    loadingIndicator.setVisible(false);
                    loadActivitiesBtn.setDisable(false);
                    logArea.appendText("Error loading activities: " + e.getMessage() + "\n");
                    showAlert(Alert.AlertType.ERROR, "Error", "Failed to load activities: " + e.getMessage());
                });
            }
        }).start();
    }

    private ActivityData mapToActivityData(Map<String, Object> activityMap) {
        ActivityData data = new ActivityData();

        data.setActivityId(getStringValue(activityMap, "activity_id"));
        data.setActivityName(getStringValue(activityMap, "activity_name"));
        data.setActivityCode(getStringValue(activityMap, "activity_code"));
        data.setStartDate(getStringValue(activityMap, "start_date"));
        data.setFinishDate(getStringValue(activityMap, "finish_date"));
        data.setDuration(getStringValue(activityMap, "duration"));
        data.setStatusCode(getStringValue(activityMap, "status_code"));
        data.setWbsName(getStringValue(activityMap, "wbs_name"));

        return data;
    }

    private String getStringValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString() : "";
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    // Inner class for TableView data
    public static class ActivityData {
        private String activityId;
        private String activityName;
        private String activityCode;
        private String startDate;
        private String finishDate;
        private String duration;
        private String statusCode;
        private String wbsName;

        // Getters and setters
        public String getActivityId() { return activityId; }
        public void setActivityId(String activityId) { this.activityId = activityId; }

        public String getActivityName() { return activityName; }
        public void setActivityName(String activityName) { this.activityName = activityName; }

        public String getActivityCode() { return activityCode; }
        public void setActivityCode(String activityCode) { this.activityCode = activityCode; }

        public String getStartDate() { return startDate; }
        public void setStartDate(String startDate) { this.startDate = startDate; }

        public String getFinishDate() { return finishDate; }
        public void setFinishDate(String finishDate) { this.finishDate = finishDate; }

        public String getDuration() { return duration; }
        public void setDuration(String duration) { this.duration = duration; }

        public String getStatusCode() { return statusCode; }
        public void setStatusCode(String statusCode) { this.statusCode = statusCode; }

        public String getWbsName() { return wbsName; }
        public void setWbsName(String wbsName) { this.wbsName = wbsName; }
    }
}
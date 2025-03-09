package com.tpcgrp.p6ebs.controller;

import com.tpcgrp.p6ebs.service.P6ProjectService;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import org.springframework.stereotype.Controller;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Controller
public class P6ProjectSearchController {

    private final P6ProjectService p6ProjectService;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

    // Connection parameters - to be passed from the main controller
    private String server;
    private String database;
    private String username;
    private String password;

    // Cache of projects for search
    private List<Map<String, Object>> projectCache = new ArrayList<>();

    // Minimum search length before performing search
    private static final int MIN_SEARCH_LENGTH = 3;

    // Timer for delayed search (to avoid excessive DB queries while typing)
    private Timer searchTimer;

    @FXML private TextField searchField;
    @FXML private ListView<String> searchResultsList;
    @FXML private ProgressIndicator searchProgressIndicator;
    @FXML private VBox projectDetailsPane;
    @FXML private Label projectNameLabel;
    @FXML private Label projectIdLabel;
    @FXML private Label projectCodeLabel;
    @FXML private Label startDateLabel;
    @FXML private Label finishDateLabel;
    @FXML private Label statusLabel;
    @FXML private Label lastUpdatedLabel;
    @FXML private Label activitiesCountLabel;
    @FXML private Label resourcesCountLabel;
    @FXML private TextArea projectDescriptionArea;
    @FXML private TextArea logArea;

    // Hold the mapping from display string to project ID
    private Map<String, String> displayToIdMap = new HashMap<>();

    public P6ProjectSearchController(P6ProjectService p6ProjectService) {
        this.p6ProjectService = p6ProjectService;
    }

    @FXML
    public void initialize() {
        // Set up UI elements
        setupSearchField();
        setupResultsList();

        // Initially hide project details and show a message
        projectDetailsPane.setVisible(false);

        searchProgressIndicator.setVisible(false);

        // Log initialization
        logMessage("Project search component initialized");
    }

    public void setConnectionParams(String server, String database, String username, String password) {
        this.server = server;
        this.database = database;
        this.username = username;
        this.password = password;

        // Load projects after setting connection parameters
        loadProjects();
    }

    private void setupSearchField() {
        // Set up search field listener with delayed execution
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            // Cancel existing timer if there is one
            if (searchTimer != null) {
                searchTimer.cancel();
            }

            if (newValue.length() >= MIN_SEARCH_LENGTH) {
                // Schedule a new search after 500ms delay
                searchTimer = new Timer();
                searchTimer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        Platform.runLater(() -> performSearch(newValue));
                    }
                }, 500);
            } else {
                // Clear results if search is too short
                searchResultsList.getItems().clear();
                displayToIdMap.clear();
            }
        });
    }

    private void setupResultsList() {
        // Set up selection listener for search results
        searchResultsList.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) -> {
                    if (newValue != null) {
                        String projectId = displayToIdMap.get(newValue);
                        if (projectId != null) {
                            loadProjectDetails(projectId);
                        }
                    }
                }
        );
    }

    private void loadProjects() {
        searchProgressIndicator.setVisible(true);

        executorService.submit(() -> {
            try {
                // Load all projects to cache for quick search
                projectCache = p6ProjectService.getAllProjects(
                        server, database, username, password
                );

                Platform.runLater(() -> {
                    searchProgressIndicator.setVisible(false);
                    logMessage("Loaded " + projectCache.size() + " projects from P6");
                });
            } catch (SQLException e) {
                Platform.runLater(() -> {
                    searchProgressIndicator.setVisible(false);
                    logMessage("Error loading projects: " + e.getMessage());
                    showAlert(Alert.AlertType.ERROR, "Error", "Failed to load projects: " + e.getMessage());
                });
            }
        });
    }

    private void performSearch(String searchText) {
        if (projectCache.isEmpty()) {
            logMessage("Project cache is empty. Cannot search.");
            return;
        }

        searchProgressIndicator.setVisible(true);

        // Clear previous results
        searchResultsList.getItems().clear();
        displayToIdMap.clear();

        // Convert search text to lowercase for case-insensitive search
        String searchLower = searchText.toLowerCase();

        // Filter projects that match the search text in ID, name, or code
        List<Map<String, Object>> matchingProjects = projectCache.stream()
                .filter(project -> {
                    String id = getStringValue(project, "proj_id");
                    String name = getStringValue(project, "proj_name");
                    String code = getStringValue(project, "proj_short_name");

                    return id.toLowerCase().contains(searchLower) ||
                            name.toLowerCase().contains(searchLower) ||
                            code.toLowerCase().contains(searchLower);
                })
                .collect(Collectors.toList());

        // Create list items with formatted display text
        ObservableList<String> displayItems = FXCollections.observableArrayList();
        for (Map<String, Object> project : matchingProjects) {
            String id = getStringValue(project, "proj_id");
            String name = getStringValue(project, "proj_name");
            String code = getStringValue(project, "proj_short_name");

            String displayText = name + " (" + code + ")";
            displayItems.add(displayText);
            displayToIdMap.put(displayText, id);
        }

        searchResultsList.setItems(displayItems);
        searchProgressIndicator.setVisible(false);

        logMessage("Found " + matchingProjects.size() + " matching projects");
    }

    private void loadProjectDetails(String projectId) {
        searchProgressIndicator.setVisible(true);

        executorService.submit(() -> {
            try {
                // Load detailed project information
                Map<String, Object> projectDetails = p6ProjectService.getProjectById(
                        server, database, username, password, projectId
                );

                // Load project summary with activity counts, etc.
                Map<String, Object> projectSummary = p6ProjectService.getProjectSummary(
                        server, database, username, password, projectId
                );

                Platform.runLater(() -> {
                    displayProjectDetails(projectDetails, projectSummary);
                    searchProgressIndicator.setVisible(false);
                });
            } catch (SQLException e) {
                Platform.runLater(() -> {
                    logMessage("Error loading project details: " + e.getMessage());
                    searchProgressIndicator.setVisible(false);
                    showAlert(Alert.AlertType.ERROR, "Error", "Failed to load project details: " + e.getMessage());
                });
            }
        });
    }

    private void displayProjectDetails(Map<String, Object> projectDetails, Map<String, Object> projectSummary) {
        // Make details pane visible
        projectDetailsPane.setVisible(true);

        // Display basic project information
        projectNameLabel.setText(getStringValue(projectDetails, "proj_name"));
        projectIdLabel.setText(getStringValue(projectDetails, "proj_id"));
        projectCodeLabel.setText(getStringValue(projectDetails, "proj_short_name"));

        // Format and display dates
        startDateLabel.setText(formatDate(projectDetails.get("plan_start_date")));
        finishDateLabel.setText(formatDate(projectDetails.get("plan_end_date")));

        // Display status and last updated
        statusLabel.setText(getStringValue(projectDetails, "status_code"));
        lastUpdatedLabel.setText(formatDate(projectDetails.get("update_date")));

        // Display counts from summary
        activitiesCountLabel.setText(projectSummary.get("total_activities") != null ?
                projectSummary.get("total_activities").toString() : "0");
        resourcesCountLabel.setText(projectSummary.get("total_resources") != null ?
                projectSummary.get("total_resources").toString() : "0");

        // Display description if available
        String description = getStringValue(projectDetails, "description");
        projectDescriptionArea.setText(description.isEmpty() ? "No description available" : description);

        logMessage("Displayed details for project: " + projectIdLabel.getText());
    }

    private String getStringValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString() : "";
    }

    private String formatDate(Object dateObj) {
        if (dateObj == null) {
            return "N/A";
        }

        try {
            if (dateObj instanceof Date) {
                return dateFormat.format((Date) dateObj);
            } else if (dateObj instanceof java.sql.Date) {
                return dateFormat.format(new Date(((java.sql.Date) dateObj).getTime()));
            } else {
                return dateObj.toString();
            }
        } catch (Exception e) {
            return "Invalid date";
        }
    }

    private void logMessage(String message) {
        if (logArea != null) {
            Platform.runLater(() -> {
                logArea.appendText(message + "\n");
                // Scroll to bottom
                logArea.setScrollTop(Double.MAX_VALUE);
            });
        }
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    /**
     * Clean up resources when controller is no longer needed
     */
    public void shutdown() {
        if (searchTimer != null) {
            searchTimer.cancel();
        }
        executorService.shutdown();
    }
}
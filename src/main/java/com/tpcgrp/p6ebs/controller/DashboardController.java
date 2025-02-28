package com.tpcgrp.p6ebs.controller;

import com.tpcgrp.p6ebs.service.ConfigurationService;
import com.tpcgrp.p6ebs.service.DatabaseService;
import com.tpcgrp.p6ebs.service.integration.*;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.Region;
import org.springframework.stereotype.Controller;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Controller
public class DashboardController {

    private final SynchronizationManager syncManager;
    private final SchedulerService schedulerService;
    private final DatabaseService databaseService;
    private final ConfigurationService configService;
    private final IntegrationLogService logService;

    private final ScheduledExecutorService refreshExecutor = Executors.newSingleThreadScheduledExecutor();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    @FXML private Text lastUpdatedText;
    @FXML private Text successCountText;
    @FXML private Text successRateText;
    @FXML private Text failedCountText;
    @FXML private Text failureRateText;
    @FXML private Text scheduledCountText;
    @FXML private Text nextScheduledText;
    @FXML private Text entitiesCountText;
    @FXML private Text avgTimeText;

    @FXML private PieChart integrationStatusChart;
    @FXML private BarChart<String, Number> integrationTypesChart;
    @FXML private TableView<IntegrationRecord> recentIntegrationsTable;

    @FXML private Region p6StatusIndicator;
    @FXML private Text p6StatusText;
    @FXML private Text p6DetailsText;

    @FXML private Region ebsStatusIndicator;
    @FXML private Text ebsStatusText;
    @FXML private Text ebsDetailsText;

    @FXML private ProgressBar memoryUsageBar;
    @FXML private Text memoryUsageText;

    @FXML private ProgressBar diskUsageBar;
    @FXML private Text diskUsageText;

    public DashboardController(SynchronizationManager syncManager,
                               SchedulerService schedulerService,
                               DatabaseService databaseService,
                               ConfigurationService configService,
                               IntegrationLogService logService) {
        this.syncManager = syncManager;
        this.schedulerService = schedulerService;
        this.databaseService = databaseService;
        this.configService = configService;
        this.logService = logService;
    }

    @FXML
    public void initialize() {
        setupRecentIntegrationsTable();
        refreshDashboard();

        // Schedule automatic refresh every 60 seconds
        refreshExecutor.scheduleAtFixedRate(
                () -> Platform.runLater(this::refreshDashboard),
                60, 60, TimeUnit.SECONDS
        );
    }

    /**
     * Set up columns for the recent integrations table
     */
    private void setupRecentIntegrationsTable() {
        TableColumn<IntegrationRecord, String> typeColumn = new TableColumn<>("Type");
        typeColumn.setCellValueFactory(new PropertyValueFactory<>("integrationType"));

        TableColumn<IntegrationRecord, String> statusColumn = new TableColumn<>("Status");
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        statusColumn.setCellFactory(column -> {
            return new javafx.scene.control.TableCell<IntegrationRecord, String>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);

                    if (empty || item == null) {
                        setText(null);
                        setStyle("");
                    } else {
                        setText(item);
                        if ("COMPLETED".equals(item)) {
                            setStyle("-fx-text-fill: green;");
                        } else if ("FAILED".equals(item)) {
                            setStyle("-fx-text-fill: red;");
                        } else {
                            setStyle("-fx-text-fill: black;");
                        }
                    }
                }
            };
        });

        TableColumn<IntegrationRecord, String> startTimeColumn = new TableColumn<>("Start Time");
        startTimeColumn.setCellValueFactory(new PropertyValueFactory<>("startTime"));

        TableColumn<IntegrationRecord, String> durationColumn = new TableColumn<>("Duration");
        durationColumn.setCellValueFactory(new PropertyValueFactory<>("duration"));

        TableColumn<IntegrationRecord, Integer> entitiesColumn = new TableColumn<>("Entities");
        entitiesColumn.setCellValueFactory(new PropertyValueFactory<>("entitiesProcessed"));

        TableColumn<IntegrationRecord, Integer> updatedColumn = new TableColumn<>("Updated");
        updatedColumn.setCellValueFactory(new PropertyValueFactory<>("entitiesUpdated"));

        recentIntegrationsTable.getColumns().addAll(
                typeColumn, statusColumn, startTimeColumn, durationColumn, entitiesColumn, updatedColumn
        );
    }

    /**
     * Refresh all dashboard data
     */
    @FXML
    public void refreshDashboard() {
        updateLastRefreshTime();
        updateIntegrationMetrics();
        updateIntegrationCharts();
        updateRecentIntegrations();
        updateConnectionStatus();
        updateSystemMetrics();
    }

    /**
     * Update the last refresh time display
     */
    private void updateLastRefreshTime() {
        lastUpdatedText.setText("Last updated: " + dateFormat.format(new Date()));
    }

    /**
     * Update integration metrics
     */
    private void updateIntegrationMetrics() {
        List<SynchronizationManager.SyncRecord> history = syncManager.getSyncHistory(null);

        // Count successful and failed integrations
        int successCount = 0;
        int failedCount = 0;
        int totalEntitiesProcessed = 0;
        long totalDuration = 0;

        for (SynchronizationManager.SyncRecord record : history) {
            if ("COMPLETED".equals(record.getStatus())) {
                successCount++;
            } else {
                failedCount++;
            }
            totalEntitiesProcessed += record.getEntitiesProcessed();
            totalDuration += record.getDurationMs();
        }

        int totalCount = history.size();
        double successRate = totalCount > 0 ? (double) successCount / totalCount * 100 : 0;
        double failureRate = totalCount > 0 ? (double) failedCount / totalCount * 100 : 0;
        double avgDurationSec = totalCount > 0 ? (double) totalDuration / totalCount / 1000 : 0;

        // Update UI components
        successCountText.setText(String.valueOf(successCount));
        successRateText.setText(String.format("%.1f%%", successRate));

        failedCountText.setText(String.valueOf(failedCount));
        failureRateText.setText(String.format("%.1f%%", failureRate));

        entitiesCountText.setText(String.valueOf(totalEntitiesProcessed));
        avgTimeText.setText(String.format("Avg. time: %.1fs", avgDurationSec));

        // Update scheduled tasks count
        Map<String, SchedulerService.ScheduleInfo> scheduledTasks = schedulerService.getAllScheduledTasks();
        int activeTasksCount = 0;
        Date nextScheduled = null;

        for (SchedulerService.ScheduleInfo info : scheduledTasks.values()) {
            if (info.isActive()) {
                activeTasksCount++;
                if (info.getNextRun() != null && (nextScheduled == null || info.getNextRun().before(nextScheduled))) {
                    nextScheduled = info.getNextRun();
                }
            }
        }

        scheduledCountText.setText(String.valueOf(activeTasksCount));
        if (nextScheduled != null) {
            nextScheduledText.setText("Next: " + dateFormat.format(nextScheduled));
        } else {
            nextScheduledText.setText("None scheduled");
        }
    }

    /**
     * Update integration charts
     */
    private void updateIntegrationCharts() {
        List<SynchronizationManager.SyncRecord> history = syncManager.getSyncHistory(null);

        // Update status pie chart
        int successCount = 0;
        int failedCount = 0;

        // Count integration types
        Map<String, Integer> typeCountMap = new HashMap<>();

        for (SynchronizationManager.SyncRecord record : history) {
            if ("COMPLETED".equals(record.getStatus())) {
                successCount++;
            } else {
                failedCount++;
            }

            String type = record.getSyncType();
            typeCountMap.put(type, typeCountMap.getOrDefault(type, 0) + 1);
        }

        // Create pie chart data
        ObservableList<PieChart.Data> pieChartData = FXCollections.observableArrayList(
                new PieChart.Data("Success", successCount),
                new PieChart.Data("Failed", failedCount)
        );
        integrationStatusChart.setData(pieChartData);

        // Apply colors for pie chart
        pieChartData.get(0).getNode().setStyle("-fx-pie-color: #4caf50;"); // Green for success
        pieChartData.get(1).getNode().setStyle("-fx-pie-color: #f44336;"); // Red for failed

        // Create bar chart data
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Integration Count");

        for (Map.Entry<String, Integer> entry : typeCountMap.entrySet()) {
            series.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()));
        }

        integrationTypesChart.getData().clear();
        integrationTypesChart.getData().add(series);
    }

    /**
     * Update recent integrations table
     */
    private void updateRecentIntegrations() {
        List<SynchronizationManager.SyncRecord> history = syncManager.getSyncHistory(null);

        // Sort by start time (most recent first)
        history.sort((r1, r2) -> r2.getStartTime().compareTo(r1.getStartTime()));

        // Convert to table record objects
        ObservableList<IntegrationRecord> records = FXCollections.observableArrayList();

        for (SynchronizationManager.SyncRecord record : history) {
            IntegrationRecord tableRecord = new IntegrationRecord();
            tableRecord.setIntegrationType(record.getSyncType());
            tableRecord.setStatus(record.getStatus());
            tableRecord.setStartTime(dateFormat.format(record.getStartTime()));

            // Format duration
            long seconds = record.getDurationMs() / 1000;
            tableRecord.setDuration(seconds + "s");

            tableRecord.setEntitiesProcessed(record.getEntitiesProcessed());
            tableRecord.setEntitiesUpdated(record.getEntitiesUpdated());

            records.add(tableRecord);

            // Limit to 10 most recent records
            if (records.size() >= 10) {
                break;
            }
        }

        recentIntegrationsTable.setItems(records);
    }

    /**
     * Update connection status indicators
     */
    private void updateConnectionStatus() {
        try {
            ConfigurationService.Configuration config = configService.loadConfiguration();

            // Check P6 connection
            boolean p6Connected = databaseService.testP6Connection(
                    config.getP6Server(),
                    config.getP6Database(),
                    config.getP6Username(),
                    config.getP6Password()
            );

            // Update P6 status
            if (p6Connected) {
                p6StatusIndicator.setStyle("-fx-background-color: #4caf50;"); // Green
                p6StatusText.setText("Connected");
            } else {
                p6StatusIndicator.setStyle("-fx-background-color: #f44336;"); // Red
                p6StatusText.setText("Disconnected");
            }
            p6DetailsText.setText("Server: " + config.getP6Server());

            // Check EBS connection
            boolean ebsConnected = databaseService.testEbsConnection(
                    config.getEbsServer(),
                    config.getEbsSid(),
                    config.getEbsUsername(),
                    config.getEbsPassword()
            );

            // Update EBS status
            if (ebsConnected) {
                ebsStatusIndicator.setStyle("-fx-background-color: #4caf50;"); // Green
                ebsStatusText.setText("Connected");
            } else {
                ebsStatusIndicator.setStyle("-fx-background-color: #f44336;"); // Red
                ebsStatusText.setText("Disconnected");
            }
            ebsDetailsText.setText("Server: " + config.getEbsServer());

        } catch (Exception e) {
            // Handle exception
            logService.logError("Error updating connection status: " + e.getMessage());
        }
    }

    /**
     * Update system metrics (memory, disk usage)
     */
    private void updateSystemMetrics() {
        // Memory usage
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;

        double memoryUsageRatio = (double) usedMemory / totalMemory;
        memoryUsageBar.setProgress(memoryUsageRatio);

        String memoryText = String.format("%.1f MB / %.1f MB",
                usedMemory / (1024.0 * 1024.0),
                totalMemory / (1024.0 * 1024.0));
        memoryUsageText.setText(memoryText);

        // Log disk usage
        try {
            File logDir = new File(System.getProperty("user.home") + "/.p6ebs/logs");
            long logSize = calculateFolderSize(logDir);

            // Assume 1GB max log size for the progress bar
            long maxLogSize = 1024 * 1024 * 1024; // 1 GB
            double diskUsageRatio = (double) logSize / maxLogSize;
            diskUsageBar.setProgress(Math.min(diskUsageRatio, 1.0));

            String diskText = String.format("%.1f MB / %.1f MB",
                    logSize / (1024.0 * 1024.0),
                    maxLogSize / (1024.0 * 1024.0));
            diskUsageText.setText(diskText);

        } catch (Exception e) {
            logService.logError("Error updating connection status: " + e.getMessage());
            diskUsageBar.setProgress(0);
            diskUsageText.setText("N/A");
        }
    }

    /**
     * Calculate the size of a folder and its contents
     */
    private long calculateFolderSize(File folder) {
        if (!folder.exists() || !folder.isDirectory()) {
            return 0;
        }

        long size = 0;
        File[] files = folder.listFiles();

        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    size += file.length();
                } else if (file.isDirectory()) {
                    size += calculateFolderSize(file);
                }
            }
        }

        return size;
    }

    /**
     * Clean up resources when controller is no longer needed
     */
    public void shutdown() {
        refreshExecutor.shutdown();
    }

    /**
     * Data class for the integration records table
     */
    public static class IntegrationRecord {
        private String integrationType;
        private String status;
        private String startTime;
        private String duration;
        private int entitiesProcessed;
        private int entitiesUpdated;

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

        public int getEntitiesUpdated() { return entitiesUpdated; }
        public void setEntitiesUpdated(int entitiesUpdated) { this.entitiesUpdated = entitiesUpdated; }
    }
}
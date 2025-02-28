/**
 * Service for scheduling automatic synchronization jobs
 */
package com.tpcgrp.p6ebs.service.integration;

import com.tpcgrp.p6ebs.service.ConfigurationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.*;

@Service
@Slf4j
public class SchedulerService {

    private final IntegrationService integrationService;
    private final ConfigurationManager configManager;
    private final IntegrationLogService logService;
    private final SynchronizationManager syncManager;

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final Map<String, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();

    @Autowired
    public SchedulerService(IntegrationService integrationService,
                            ConfigurationManager configManager,
                            IntegrationLogService logService,
                            SynchronizationManager syncManager) {
        this.integrationService = integrationService;
        this.configManager = configManager;
        this.logService = logService;
        this.syncManager = syncManager;

        // Initialize scheduled tasks
        initializeScheduledTasks();
    }

    /**
     * Initialize scheduled tasks based on configuration
     */
    private void initializeScheduledTasks() {
        // Schedule tasks for each integration type
        for (Map.Entry<String, Integer> entry : configManager.getConfig().getSyncIntervals().entrySet()) {
            String integrationType = entry.getKey();
            int intervalHours = entry.getValue();

            scheduleIntegration(integrationType, intervalHours);
        }
    }

    /**
     * Schedule an integration job
     */
    public void scheduleIntegration(String integrationType, int intervalHours) {
        // Cancel existing schedule if any
        cancelScheduledIntegration(integrationType);

        // Create new schedule
        ScheduledFuture<?> task = scheduler.scheduleAtFixedRate(
                () -> executeScheduledIntegration(integrationType),
                calculateInitialDelay(integrationType, intervalHours),
                intervalHours,
                TimeUnit.HOURS
        );

        scheduledTasks.put(integrationType, task);
        logService.logInfo("Scheduled integration for " + integrationType + " every " + intervalHours + " hours");
    }

    /**
     * Calculate initial delay for the next schedule
     */
    private long calculateInitialDelay(String integrationType, int intervalHours) {
        Date lastSync = syncManager.getLastSyncTime(integrationType);

        if (lastSync == null) {
            // If never synced, start in 1 minute
            return 1;
        }

        // Calculate time since last sync
        long timeSinceLastSync = System.currentTimeMillis() - lastSync.getTime();
        long intervalMs = intervalHours * 60 * 60 * 1000;

        if (timeSinceLastSync >= intervalMs) {
            // If interval has already passed, start soon
            return 1;
        }

        // Calculate remaining time until next scheduled sync
        return (intervalMs - timeSinceLastSync) / (60 * 1000); // Convert to minutes
    }

    /**
     * Cancel a scheduled integration
     */
    public void cancelScheduledIntegration(String integrationType) {
        ScheduledFuture<?> task = scheduledTasks.get(integrationType);
        if (task != null) {
            task.cancel(false);
            scheduledTasks.remove(integrationType);
            logService.logInfo("Cancelled scheduled integration for " + integrationType);
        }
    }

    /**
     * Execute a scheduled integration
     */
    private void executeScheduledIntegration(String integrationType) {
        logService.logInfo("Executing scheduled integration for " + integrationType);

        try {
            // Load connection parameters from configuration
            ConfigurationService.Configuration config = loadConnectionConfig();

            if (config == null) {
                logService.logError("Failed to load connection configuration");
                return;
            }

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

            // Execute integration with progress tracking
            integrationService.startIntegration(
                    p6Params,
                    ebsParams,
                    Collections.singletonList(integrationType),
                    (current, total, message) -> logService.logInfo(message)
            );

        } catch (Exception e) {
            logService.logError("Scheduled integration failed for " + integrationType);
        }
    }

    /**
     * Load connection configuration
     */
    private ConfigurationService.Configuration loadConnectionConfig() {
        try {
            return new ConfigurationService().loadConfiguration();
        } catch (Exception e) {
            logService.logError("Failed to load configuration");
            return null;
        }
    }

    /**
     * Get all scheduled tasks
     */
    public Map<String, ScheduleInfo> getAllScheduledTasks() {
        Map<String, ScheduleInfo> result = new HashMap<>();

        for (Map.Entry<String, Integer> entry : configManager.getConfig().getSyncIntervals().entrySet()) {
            String integrationType = entry.getKey();
            int intervalHours = entry.getValue();

            ScheduleInfo info = new ScheduleInfo();
            info.setIntegrationType(integrationType);
            info.setIntervalHours(intervalHours);

            Date lastRun = syncManager.getLastSyncTime(integrationType);
            info.setLastRun(lastRun);

            if (lastRun != null) {
                // Calculate next run time
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(lastRun);
                calendar.add(Calendar.HOUR, intervalHours);
                info.setNextRun(calendar.getTime());
            }

            info.setActive(scheduledTasks.containsKey(integrationType));

            result.put(integrationType, info);
        }

        return result;
    }

    /**
     * Run an integration immediately (manual trigger)
     */
    public void runIntegrationNow(String integrationType) {
        logService.logInfo("Manually triggering integration for " + integrationType);
        executeScheduledIntegration(integrationType);
    }

    /**
     * Shutdown the scheduler
     */
    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(10, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Class representing schedule information
     */
    public static class ScheduleInfo {
        private String integrationType;
        private int intervalHours;
        private Date lastRun;
        private Date nextRun;
        private boolean active;

        // Getters and setters
        public String getIntegrationType() { return integrationType; }
        public void setIntegrationType(String integrationType) { this.integrationType = integrationType; }
        public int getIntervalHours() { return intervalHours; }
        public void setIntervalHours(int intervalHours) { this.intervalHours = intervalHours; }
        public Date getLastRun() { return lastRun; }
        public void setLastRun(Date lastRun) { this.lastRun = lastRun; }
        public Date getNextRun() { return nextRun; }
        public void setNextRun(Date nextRun) { this.nextRun = nextRun; }
        public boolean isActive() { return active; }
        public void setActive(boolean active) { this.active = active; }
    }
}
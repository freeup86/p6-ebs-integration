/**
 * Orchestrates the synchronization process between P6 and EBS
 */
package com.tpcgrp.p6ebs.service.integration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@Slf4j
public class SynchronizationManager {

    private final MappingUtility mappingUtility;
    private final IntegrationLogService logService;
    private final ConfigurationManager configManager;

    // Store synchronization history
    private final List<SyncRecord> syncHistory = Collections.synchronizedList(new ArrayList<>());

    // Track completed integrations
    private final Map<String, Date> completedIntegrations = new ConcurrentHashMap<>();

    @Autowired
    public SynchronizationManager(MappingUtility mappingUtility,
                                  IntegrationLogService logService,
                                  ConfigurationManager configManager) {
        this.mappingUtility = mappingUtility;
        this.logService = logService;
        this.configManager = configManager;
    }

    /**
     * Start a synchronization process
     */
    public SyncSession startSynchronization(String syncType, Map<String, Object> params) {
        String sessionId = UUID.randomUUID().toString();
        SyncSession session = new SyncSession(sessionId, syncType, new Date());

        logService.logInfo("Starting synchronization session " + sessionId + " for type: " + syncType);

        // Determine sync direction based on configuration
        String direction = configManager.getSyncDirection(syncType);
        session.setDirection(direction);

        return session;
    }

    /**
     * Complete a synchronization process
     */
    public void completeSynchronization(SyncSession session, Map<String, Object> results) {
        session.setEndTime(new Date());
        session.setStatus("COMPLETED");
        session.setResults(results);

        logService.logInfo("Completed synchronization session " + session.getSessionId());

        // Store in history
        SyncRecord record = createSyncRecord(session);
        syncHistory.add(record);

        // Update completed integrations
        completedIntegrations.put(session.getSyncType(), session.getEndTime());

        // Save mapped IDs
        mappingUtility.saveIdCorrelations();
    }

    /**
     * Handle synchronization failure
     */
    public void failSynchronization(SyncSession session, Exception error) {
        session.setEndTime(new Date());
        session.setStatus("FAILED");
        session.setErrorMessage(error.getMessage());

        logService.logError("Failed synchronization session " + session.getSessionId() + ": " + error.getMessage());

        // Store in history
        SyncRecord record = createSyncRecord(session);
        syncHistory.add(record);
    }

    /**
     * Create a synchronization record for history
     */
    private SyncRecord createSyncRecord(SyncSession session) {
        SyncRecord record = new SyncRecord();
        record.setSessionId(session.getSessionId());
        record.setSyncType(session.getSyncType());
        record.setStartTime(session.getStartTime());
        record.setEndTime(session.getEndTime());
        record.setDirection(session.getDirection());
        record.setStatus(session.getStatus());
        record.setErrorMessage(session.getErrorMessage());

        // Calculate duration
        long durationMs = session.getEndTime().getTime() - session.getStartTime().getTime();
        record.setDurationMs(durationMs);

        // Store statistics
        if (session.getResults() != null) {
            // Extract key statistics from results
            record.setEntitiesProcessed((Integer) session.getResults().getOrDefault("totalEntities", 0));
            record.setEntitiesUpdated((Integer) session.getResults().getOrDefault("updatedEntities", 0));
            record.setEntitiesFailed((Integer) session.getResults().getOrDefault("failedEntities", 0));
        }

        return record;
    }

    /**
     * Record the result of a synchronization process
     */
    public void recordSynchronizationResult(String integrationType, Map<String, Object> result) {
        completedIntegrations.put(integrationType, new Date());
        // Log the result
        logService.logInfo("Recorded synchronization result for " + integrationType);

        // Create a sync record
        SyncRecord record = new SyncRecord();
        record.setSessionId(UUID.randomUUID().toString());
        record.setSyncType(integrationType);
        record.setStartTime(new Date(System.currentTimeMillis() - 1000)); // Approximate
        record.setEndTime(new Date());
        record.setStatus("COMPLETED");

        // Extract statistics if available
        if (result.containsKey("totalProjects")) {
            record.setEntitiesProcessed((Integer) result.get("totalProjects"));
        }
        if (result.containsKey("updatedProjects")) {
            record.setEntitiesUpdated((Integer) result.get("updatedProjects"));
        }

        syncHistory.add(record);
    }

    /**
     * Get the last synchronization time for an integration type
     */
    public Date getLastSyncTime(String integrationType) {
        return completedIntegrations.get(integrationType);
    }

    /**
     * Get all completed integrations
     */
    public Map<String, Date> getCompletedIntegrations() {
        return new HashMap<>(completedIntegrations);
    }

    /**
     * Get synchronization history, optionally filtered by type
     */
    public List<SyncRecord> getSyncHistory(String integrationType) {
        if (integrationType == null) {
            return new ArrayList<>(syncHistory);
        }

        return syncHistory.stream()
                .filter(record -> record.getSyncType().equals(integrationType))
                .collect(Collectors.toList());
    }

    /**
     * Check if synchronization is needed based on last sync time and changes
     */
    public boolean isSyncRequired(String integrationType, Date lastChangeP6, Date lastChangeEbs) {
        Date lastSync = completedIntegrations.get(integrationType);

        if (lastSync == null) {
            return true; // Never synced before
        }

        // Check if there were changes after last sync
        return (lastChangeP6 != null && lastChangeP6.after(lastSync)) ||
                (lastChangeEbs != null && lastChangeEbs.after(lastSync));
    }

    /**
     * Clear synchronization history
     */
    public void clearSyncHistory() {
        syncHistory.clear();
        logService.logInfo("Synchronization history cleared");
    }

    /**
     * Class representing a synchronization session
     */
    public static class SyncSession {
        private final String sessionId;
        private final String syncType;
        private final Date startTime;
        private Date endTime;
        private String status;
        private String direction;
        private String errorMessage;
        private Map<String, Object> results;

        public SyncSession(String sessionId, String syncType, Date startTime) {
            this.sessionId = sessionId;
            this.syncType = syncType;
            this.startTime = startTime;
            this.status = "IN_PROGRESS";
        }

        // Getters and setters
        public String getSessionId() { return sessionId; }
        public String getSyncType() { return syncType; }
        public Date getStartTime() { return startTime; }
        public Date getEndTime() { return endTime; }
        public void setEndTime(Date endTime) { this.endTime = endTime; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getDirection() { return direction; }
        public void setDirection(String direction) { this.direction = direction; }
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
        public Map<String, Object> getResults() { return results; }
        public void setResults(Map<String, Object> results) { this.results = results; }
    }

    /**
     * Class representing a record of completed synchronization
     */
    public static class SyncRecord {
        private String sessionId;
        private String syncType;
        private Date startTime;
        private Date endTime;
        private long durationMs;
        private String direction;
        private String status;
        private String errorMessage;
        private int entitiesProcessed;
        private int entitiesUpdated;
        private int entitiesFailed;

        // Getters and setters
        public String getSessionId() { return sessionId; }
        public void setSessionId(String sessionId) { this.sessionId = sessionId; }
        public String getSyncType() { return syncType; }
        public void setSyncType(String syncType) { this.syncType = syncType; }
        public Date getStartTime() { return startTime; }
        public void setStartTime(Date startTime) { this.startTime = startTime; }
        public Date getEndTime() { return endTime; }
        public void setEndTime(Date endTime) { this.endTime = endTime; }
        public long getDurationMs() { return durationMs; }
        public void setDurationMs(long durationMs) { this.durationMs = durationMs; }
        public String getDirection() { return direction; }
        public void setDirection(String direction) { this.direction = direction; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
        public int getEntitiesProcessed() { return entitiesProcessed; }
        public void setEntitiesProcessed(int entitiesProcessed) { this.entitiesProcessed = entitiesProcessed; }
        public int getEntitiesUpdated() { return entitiesUpdated; }
        public void setEntitiesUpdated(int entitiesUpdated) { this.entitiesUpdated = entitiesUpdated; }
        public int getEntitiesFailed() { return entitiesFailed; }
        public void setEntitiesFailed(int entitiesFailed) { this.entitiesFailed = entitiesFailed; }
    }
}
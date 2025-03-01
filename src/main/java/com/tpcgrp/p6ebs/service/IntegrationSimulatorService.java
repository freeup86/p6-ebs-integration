package com.tpcgrp.p6ebs.service;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

@Service
@Slf4j
public class IntegrationSimulatorService {

    // Simulated data stores
    private final Map<String, List<SimulatedEntity>> p6DataStore = new HashMap<>();
    private final Map<String, List<SimulatedEntity>> ebsDataStore = new HashMap<>();

    /**
     * Generate sample data for simulation
     */
    public void generateSampleData() {
        log.info("Starting sample data generation");

        // Clear existing data
        p6DataStore.clear();
        ebsDataStore.clear();

        try {
            // Projects
            generateProjects();

            // Activities
            generateActivities();

            // Resources
            generateResources();

            log.info("Sample data generation completed successfully");
            log.info("P6 Projects: {}", p6DataStore.get("projects").size());
            log.info("EBS Projects: {}", ebsDataStore.get("projects").size());
            log.info("P6 Activities: {}", p6DataStore.get("activities").size());
            log.info("EBS Activities: {}", ebsDataStore.get("activities").size());
            log.info("P6 Resources: {}", p6DataStore.get("resources").size());
            log.info("EBS Resources: {}", ebsDataStore.get("resources").size());
        } catch (Exception e) {
            log.error("Error generating sample data", e);
        }
    }

    /**
     * Simulate integration for a specific entity type
     */
    public SimulationResult simulateIntegration(String entityType, String direction) {
        SimulationResult result = new SimulationResult();
        result.setEntityType(entityType);
        result.setDirection(direction);
        result.setStartTime(new Date());

        List<SimulatedEntity> sourceEntities;
        List<SimulatedEntity> targetEntities;

        if (direction.equals("P6_TO_EBS")) {
            sourceEntities = p6DataStore.get(entityType);
            targetEntities = ebsDataStore.get(entityType);
        } else {
            sourceEntities = ebsDataStore.get(entityType);
            targetEntities = p6DataStore.get(entityType);
        }

        if (sourceEntities == null || targetEntities == null) {
            result.setStatus("Failed");
            result.setMessage("Invalid entity type");
            return result;
        }

        // Improved matching logic
        int matchedEntities = 0;
        int updatedEntities = 0;
        List<String> matchedIds = new ArrayList<>();

        for (SimulatedEntity sourceEntity : sourceEntities) {
            // Find potential matching entities with more flexible criteria
            List<SimulatedEntity> potentialMatches = targetEntities.stream()
                    .filter(e -> isEntityMatch(sourceEntity, e, entityType))
                    .collect(Collectors.toList());

            if (!potentialMatches.isEmpty()) {
                // If multiple matches, take the first one
                SimulatedEntity targetEntity = potentialMatches.get(0);

                matchedEntities++;

                // Simulate updating target entity
                updateEntity(sourceEntity, targetEntity, entityType);
                updatedEntities++;
                matchedIds.add(sourceEntity.getId());
            }
        }

        result.setStatus("Completed");
        result.setEndTime(new Date());
        result.setMatchedEntities(matchedEntities);
        result.setUpdatedEntities(updatedEntities);
        result.setMatchedEntityIds(matchedIds);

        log.info("Simulation Result for {}: Matched={}, Updated={}",
                entityType, matchedEntities, updatedEntities);

        return result;
    }

    /**
     * More sophisticated entity matching based on entity type
     */
    private boolean isEntityMatch(SimulatedEntity source, SimulatedEntity target, String entityType) {
        switch (entityType) {
            case "projects":
                // Match projects by similar names or partial name match
                return source.getName().toLowerCase().contains(target.getName().toLowerCase()) ||
                        target.getName().toLowerCase().contains(source.getName().toLowerCase());

            case "activities":
                // Match activities by partial name or similar project ID
                return source.getName().toLowerCase().contains(target.getName().toLowerCase()) ||
                        Objects.equals(
                                source.getData().get("projectId"),
                                target.getData().get("projectId")
                        );

            case "resources":
                // Match resources by similar names or email
                return source.getName().toLowerCase().contains(target.getName().toLowerCase()) ||
                        Objects.equals(
                                source.getData().get("email"),
                                target.getData().get("email")
                        );

            default:
                // Fallback to name-based matching
                return source.getName().toLowerCase().contains(target.getName().toLowerCase());
        }
    }

    /**
     * Update target entity with source entity data
     */
    private void updateEntity(SimulatedEntity source, SimulatedEntity target, String entityType) {
        // Merge data from source to target with some type-specific logic
        switch (entityType) {
            case "projects":
                // Merge project-specific fields
                target.getData().put("status", source.getData().get("status"));
                target.getData().put("budget", source.getData().get("budget"));
                break;

            case "activities":
                // Merge activity-specific fields
                target.getData().put("status", source.getData().get("status"));
                target.getData().put("duration", source.getData().get("duration"));
                break;

            case "resources":
                // Merge resource-specific fields
                target.getData().put("role", source.getData().get("role"));
                break;

            default:
                // Generic merge
                target.getData().putAll(source.getData());
        }
    }

    /**
     * Get sample data for a specific entity type
     */
    public List<SimulatedEntity> getSampleData(String entityType, boolean isP6) {
        Map<String, List<SimulatedEntity>> dataStore = isP6 ? p6DataStore : ebsDataStore;
        return dataStore.getOrDefault(entityType, new ArrayList<>());
    }

    /**
     * Generate simulated project data
     */
    private void generateProjects() {
        List<SimulatedEntity> p6Projects = new CopyOnWriteArrayList<>();
        List<SimulatedEntity> ebsProjects = new CopyOnWriteArrayList<>();

        for (int i = 1; i <= 10; i++) {
            // P6 Projects
            SimulatedEntity p6Project = new SimulatedEntity();
            p6Project.setId("P6_PROJ_" + i);
            p6Project.setName("P6 Project " + i);
            p6Project.setData(Map.of(
                    "status", "Active",
                    "startDate", new Date(),
                    "budget", 100000.0 * i
            ));
            p6Projects.add(p6Project);

            // EBS Projects
            SimulatedEntity ebsProject = new SimulatedEntity();
            ebsProject.setId("EBS_PROJ_" + i);
            ebsProject.setName("EBS Project " + i);
            ebsProject.setData(Map.of(
                    "status", "In Progress",
                    "startDate", new Date(),
                    "budget", 95000.0 * i
            ));
            ebsProjects.add(ebsProject);
        }

        p6DataStore.put("projects", p6Projects);
        ebsDataStore.put("projects", ebsProjects);
    }

    /**
     * Generate simulated activity data
     */
    private void generateActivities() {
        List<SimulatedEntity> p6Activities = new CopyOnWriteArrayList<>();
        List<SimulatedEntity> ebsActivities = new CopyOnWriteArrayList<>();

        for (int i = 1; i <= 20; i++) {
            // P6 Activities
            SimulatedEntity p6Activity = new SimulatedEntity();
            p6Activity.setId("P6_ACT_" + i);
            p6Activity.setName("P6 Activity " + i);
            p6Activity.setData(Map.of(
                    "projectId", "P6_PROJ_" + ((i % 10) + 1),
                    "status", "In Progress",
                    "duration", 10 * i
            ));
            p6Activities.add(p6Activity);

            // EBS Activities
            SimulatedEntity ebsActivity = new SimulatedEntity();
            ebsActivity.setId("EBS_ACT_" + i);
            ebsActivity.setName("EBS Activity " + i);
            ebsActivity.setData(Map.of(
                    "projectId", "EBS_PROJ_" + ((i % 10) + 1),
                    "status", "Active",
                    "duration", 9 * i
            ));
            ebsActivities.add(ebsActivity);
        }

        p6DataStore.put("activities", p6Activities);
        ebsDataStore.put("activities", ebsActivities);
    }

    /**
     * Generate simulated resource data
     */
    private void generateResources() {
        List<SimulatedEntity> p6Resources = new CopyOnWriteArrayList<>();
        List<SimulatedEntity> ebsResources = new CopyOnWriteArrayList<>();

        for (int i = 1; i <= 15; i++) {
            // P6 Resources
            SimulatedEntity p6Resource = new SimulatedEntity();
            p6Resource.setId("P6_RES_" + i);
            p6Resource.setName("P6 Resource " + i);
            p6Resource.setData(Map.of(
                    "email", "p6resource" + i + "@company.com",
                    "role", "Project " + (i % 3 == 0 ? "Manager" : "Member")
            ));
            p6Resources.add(p6Resource);

            // EBS Resources
            SimulatedEntity ebsResource = new SimulatedEntity();
            ebsResource.setId("EBS_RES_" + i);
            ebsResource.setName("EBS Resource " + i);
            ebsResource.setData(Map.of(
                    "email", "ebsresource" + i + "@company.com",
                    "role", "Department " + (i % 3 == 0 ? "Head" : "Member")
            ));
            ebsResources.add(ebsResource);
        }

        p6DataStore.put("resources", p6Resources);
        ebsDataStore.put("resources", ebsResources);
    }

    /**
     * Debug method to print all generated data
     */
    public void printAllData() {
        log.info("Printing All Simulated Data:");

        // Print P6 Data
        log.info("P6 DATA:");
        p6DataStore.forEach((type, entities) -> {
            log.info("--- {} ---", type);
            entities.forEach(entity ->
                    log.info("ID: {}, Name: {}, Data: {}",
                            entity.getId(), entity.getName(), entity.getData())
            );
        });

        // Print EBS Data
        log.info("EBS DATA:");
        ebsDataStore.forEach((type, entities) -> {
            log.info("--- {} ---", type);
            entities.forEach(entity ->
                    log.info("ID: {}, Name: {}, Data: {}",
                            entity.getId(), entity.getName(), entity.getData())
            );
        });
    }

    /**
     * Check if entities can be considered a match
     */
    private boolean isEntityMatch(SimulatedEntity source, SimulatedEntity target) {
        // Simple matching logic - can be customized
        return source.getName().equals(target.getName()) ||
                source.getData().get("email") != null &&
                        source.getData().get("email").equals(target.getData().get("email"));
    }

    /**
     * Update target entity with source entity data
     */
    private void updateEntity(SimulatedEntity source, SimulatedEntity target) {
        // Merge data from source to target
        target.getData().putAll(source.getData());
    }

    /**
     * Get all simulation results
     */
    public List<SimulationResult> getAllSimulationResults() {
        // This would typically be stored in a database or file
        return new ArrayList<>();
    }

    // Nested classes for data and results
    @Data
    public static class SimulatedEntity {
        private String id;
        private String name;
        private Map<String, Object> data = new HashMap<>();
    }

    @Data
    public static class SimulationResult {
        private String entityType;
        private String direction;
        private Date startTime;
        private Date endTime;
        private String status;
        private String message;
        private int matchedEntities;
        private int updatedEntities;
        private List<String> matchedEntityIds;
    }
}
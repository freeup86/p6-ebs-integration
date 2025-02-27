package com.tpcgrp.p6ebs.service;

import java.sql.*;
import java.util.*;
import org.springframework.stereotype.Service;

/**
 * Service class for retrieving activities from Primavera P6 database.
 */
@Service
public class P6ActivityService {

    private final DatabaseService databaseService;

    public P6ActivityService(DatabaseService databaseService) {
        this.databaseService = databaseService;
    }

    /**
     * Retrieves all activities from P6 database.
     *
     * @param server The database server address
     * @param database The database name
     * @param username Database username
     * @param password Database password
     * @return List of activity maps containing all activity data
     * @throws SQLException If a database error occurs
     */
    public List<Map<String, Object>> getAllActivities(String server, String database,
                                                      String username, String password) throws SQLException {

        String url = String.format("jdbc:sqlserver://%s;databaseName=%s;encrypt=true;trustServerCertificate=true",
                server, database);

        List<Map<String, Object>> activities = new ArrayList<>();

        try (Connection conn = DriverManager.getConnection(url, username, password)) {
            // SQL query to get activities with key information
            String sql = "SELECT a.activity_id, a.activity_name, a.activity_code, a.start_date, a.finish_date, " +
                    "a.duration, a.status_code, a.type, a.primary_resource_id, " +
                    "p.proj_name, wbs.wbs_name " +
                    "FROM ACTIVITIES a " +
                    "JOIN PROJECTS p ON a.proj_id = p.proj_id " +
                    "JOIN TASKRSRC tr ON a.activity_id = tr.activity_id " +
                    "JOIN WBS wbs ON a.wbs_id = wbs.wbs_id " +
                    "ORDER BY p.proj_name, a.activity_id";

            try (PreparedStatement stmt = conn.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {

                ResultSetMetaData metaData = rs.getMetaData();
                int columnCount = metaData.getColumnCount();

                while (rs.next()) {
                    Map<String, Object> activity = new HashMap<>();

                    for (int i = 1; i <= columnCount; i++) {
                        String columnName = metaData.getColumnName(i);
                        Object value = rs.getObject(i);
                        activity.put(columnName, value);
                    }

                    activities.add(activity);
                }
            }
        }

        return activities;
    }

    /**
     * Retrieves activities from P6 filtered by project ID.
     *
     * @param server The database server address
     * @param database The database name
     * @param username Database username
     * @param password Database password
     * @param projectId The project ID to filter by
     * @return List of activity maps for the specified project
     * @throws SQLException If a database error occurs
     */
    public List<Map<String, Object>> getActivitiesByProject(String server, String database,
                                                            String username, String password,
                                                            String projectId) throws SQLException {

        String url = String.format("jdbc:sqlserver://%s;databaseName=%s;encrypt=true;trustServerCertificate=true",
                server, database);

        List<Map<String, Object>> activities = new ArrayList<>();

        try (Connection conn = DriverManager.getConnection(url, username, password)) {
            String sql = "SELECT a.activity_id, a.activity_name, a.activity_code, a.start_date, a.finish_date, " +
                    "a.duration, a.status_code, a.type, a.primary_resource_id, " +
                    "p.proj_name, wbs.wbs_name " +
                    "FROM ACTIVITIES a " +
                    "JOIN PROJECTS p ON a.proj_id = p.proj_id " +
                    "LEFT JOIN TASKRSRC tr ON a.activity_id = tr.activity_id " +
                    "JOIN WBS wbs ON a.wbs_id = wbs.wbs_id " +
                    "WHERE p.proj_id = ? " +
                    "ORDER BY a.activity_id";

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, projectId);

                try (ResultSet rs = stmt.executeQuery()) {
                    ResultSetMetaData metaData = rs.getMetaData();
                    int columnCount = metaData.getColumnCount();

                    while (rs.next()) {
                        Map<String, Object> activity = new HashMap<>();

                        for (int i = 1; i <= columnCount; i++) {
                            String columnName = metaData.getColumnName(i);
                            Object value = rs.getObject(i);
                            activity.put(columnName, value);
                        }

                        activities.add(activity);
                    }
                }
            }
        }

        return activities;
    }

    /**
     * Get a list of all projects from P6.
     *
     * @param server The database server address
     * @param database The database name
     * @param username Database username
     * @param password Database password
     * @return List of project maps
     * @throws SQLException If a database error occurs
     */
    public List<Map<String, Object>> getAllProjects(String server, String database,
                                                    String username, String password) throws SQLException {

        String url = String.format("jdbc:sqlserver://%s;databaseName=%s;encrypt=true;trustServerCertificate=true",
                server, database);

        List<Map<String, Object>> projects = new ArrayList<>();

        try (Connection conn = DriverManager.getConnection(url, username, password)) {
            String sql = "SELECT proj_id, proj_name, proj_short_name, start_date, finish_date, " +
                    "status_code, create_date, update_date " +
                    "FROM PROJECTS " +
                    "ORDER BY proj_name";

            try (PreparedStatement stmt = conn.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {

                ResultSetMetaData metaData = rs.getMetaData();
                int columnCount = metaData.getColumnCount();

                while (rs.next()) {
                    Map<String, Object> project = new HashMap<>();

                    for (int i = 1; i <= columnCount; i++) {
                        String columnName = metaData.getColumnName(i);
                        Object value = rs.getObject(i);
                        project.put(columnName, value);
                    }

                    projects.add(project);
                }
            }
        }

        return projects;
    }

    /**
     * Add a method to display activities in the console for testing purposes.
     *
     * @param activities List of activity maps to display
     */
    public void displayActivities(List<Map<String, Object>> activities) {
        System.out.println("Found " + activities.size() + " activities");

        if (!activities.isEmpty()) {
            // Print table header
            Map<String, Object> firstActivity = activities.get(0);
            for (String key : firstActivity.keySet()) {
                System.out.print(key + "\t");
            }
            System.out.println();

            // Print separator
            for (int i = 0; i < 100; i++) {
                System.out.print("-");
            }
            System.out.println();

            // Print activity data
            for (Map<String, Object> activity : activities) {
                for (Object value : activity.values()) {
                    System.out.print((value != null ? value.toString() : "NULL") + "\t");
                }
                System.out.println();
            }
        }
    }
}
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

        /** P6 SQL CONNECTION
        String url = String.format("jdbc:sqlserver://%s;databaseName=%s;encrypt=true;trustServerCertificate=true",
                server, database);
         **/

        // Oracle connection format
        String url = String.format("jdbc:oracle:thin:@%s:1521:%s", server, database);

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

        /** P6 SQL CONNECTION
         String url = String.format("jdbc:sqlserver://%s;databaseName=%s;encrypt=true;trustServerCertificate=true",
         server, database);
         **/

        // Oracle connection format
        String url = String.format("jdbc:oracle:thin:@%s:1521:%s", server, database);

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
        
        /** P6 SQL CONNECTION
           /** P6 SQL CONNECTION
        String url = String.format("jdbc:sqlserver://%s;databaseName=%s;encrypt=true;trustServerCertificate=true",
                server, database);
         **/

        // Oracle connection format
        String url = String.format("jdbc:oracle:thin:@%s:1521:%s", server, database);

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

    /**
     * Create or update an activity in P6
     *
     * @param server The database server address
     * @param database The database name
     * @param username Database username
     * @param password Database password
     * @param activityData Activity data to create/update
     * @return boolean indicating success or failure
     * @throws SQLException If a database error occurs
     */
    public boolean createOrUpdateActivity(String server, String database,
                                          String username, String password,
                                          Map<String, Object> activityData) throws SQLException {

        String url = String.format("jdbc:oracle:thin:@%s:1521:%s", server, database);

        try (Connection conn = DriverManager.getConnection(url, username, password)) {
            // Check if activity exists
            String activityId = activityData.get("activity_id").toString();
            String projectId = activityData.get("proj_id").toString();

            String checkSql = "SELECT COUNT(*) FROM ACTIVITIES WHERE activity_id = ? AND proj_id = ?";

            boolean exists = false;

            try (PreparedStatement stmt = conn.prepareStatement(checkSql)) {
                stmt.setString(1, activityId);
                stmt.setString(2, projectId);

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        exists = rs.getInt(1) > 0;
                    }
                }
            }

            if (exists) {
                // Update existing activity
                StringBuilder updateSql = new StringBuilder("UPDATE ACTIVITIES SET ");
                List<String> setClauses = new ArrayList<>();
                List<Object> params = new ArrayList<>();

                for (Map.Entry<String, Object> entry : activityData.entrySet()) {
                    String key = entry.getKey();
                    if (!key.equals("activity_id") && !key.equals("proj_id") && entry.getValue() != null) {
                        setClauses.add(key + " = ?");
                        params.add(entry.getValue());
                    }
                }

                updateSql.append(String.join(", ", setClauses));
                updateSql.append(" WHERE activity_id = ? AND proj_id = ?");

                try (PreparedStatement stmt = conn.prepareStatement(updateSql.toString())) {
                    int paramIndex = 1;
                    for (Object param : params) {
                        stmt.setObject(paramIndex++, param);
                    }

                    stmt.setString(paramIndex++, activityId);
                    stmt.setString(paramIndex, projectId);

                    int rowsUpdated = stmt.executeUpdate();
                    return rowsUpdated > 0;
                }
            } else {
                // Insert new activity
                StringBuilder insertSql = new StringBuilder("INSERT INTO ACTIVITIES (");
                StringBuilder valuesSql = new StringBuilder("VALUES (");
                List<Object> params = new ArrayList<>();

                for (Map.Entry<String, Object> entry : activityData.entrySet()) {
                    if (entry.getValue() != null) {
                        if (params.size() > 0) {
                            insertSql.append(", ");
                            valuesSql.append(", ");
                        }

                        insertSql.append(entry.getKey());
                        valuesSql.append("?");
                        params.add(entry.getValue());
                    }
                }

                insertSql.append(") ").append(valuesSql).append(")");

                try (PreparedStatement stmt = conn.prepareStatement(insertSql.toString())) {
                    int paramIndex = 1;
                    for (Object param : params) {
                        stmt.setObject(paramIndex++, param);
                    }

                    int rowsInserted = stmt.executeUpdate();
                    return rowsInserted > 0;
                }
            }
        } catch (SQLException e) {
            throw e;
        }
    }
}
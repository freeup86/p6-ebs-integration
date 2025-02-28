package com.tpcgrp.p6ebs.service;

import java.sql.*;
import java.util.*;

/**
 * Service class for retrieving resources from Primavera P6 database.
 */
public class P6ResourceService {

    private final DatabaseService databaseService;

    public P6ResourceService(DatabaseService databaseService) {
        this.databaseService = databaseService;
    }

    /**
     * Retrieves all resources from P6 database.
     *
     * @param server The database server address
     * @param database The database name
     * @param username Database username
     * @param password Database password
     * @return List of resource maps containing all resource data
     * @throws SQLException If a database error occurs
     */
    public List<Map<String, Object>> getAllResources(String server, String database,
                                                     String username, String password) throws SQLException {

           /** P6 SQL CONNECTION
        String url = String.format("jdbc:sqlserver://%s;databaseName=%s;encrypt=true;trustServerCertificate=true",
                server, database);
         **/
        // Oracle connection format
        String url = String.format("jdbc:oracle:thin:@%s:1521:%s", server, database);

        List<Map<String, Object>> resources = new ArrayList<>();

        try (Connection conn = DriverManager.getConnection(url, username, password)) {
            // SQL query to get resources with key information
            String sql = "SELECT r.rsrc_id, r.rsrc_name, r.rsrc_short_name, r.email_addr, " +
                    "r.office_phone, r.rsrc_title, r.rsrc_type, r.rsrc_notes, " +
                    "r.parent_rsrc_id, r.calendar_id, r.clndr_name, r.created_by, " +
                    "r.create_date, r.update_date " +
                    "FROM RSRC r " +
                    "LEFT JOIN CALENDAR c ON r.calendar_id = c.clndr_id " +
                    "ORDER BY r.rsrc_name";

            try (PreparedStatement stmt = conn.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {

                ResultSetMetaData metaData = rs.getMetaData();
                int columnCount = metaData.getColumnCount();

                while (rs.next()) {
                    Map<String, Object> resource = new HashMap<>();

                    for (int i = 1; i <= columnCount; i++) {
                        String columnName = metaData.getColumnName(i);
                        Object value = rs.getObject(i);
                        resource.put(columnName, value);
                    }

                    resources.add(resource);
                }
            }
        }

        return resources;
    }

    /**
     * Get resource details by resource ID.
     *
     * @param server The database server address
     * @param database The database name
     * @param username Database username
     * @param password Database password
     * @param resourceId The resource ID to look up
     * @return Map containing resource details
     * @throws SQLException If a database error occurs
     */
    public Map<String, Object> getResourceById(String server, String database,
                                               String username, String password,
                                               String resourceId) throws SQLException {

           /** P6 SQL CONNECTION
        String url = String.format("jdbc:sqlserver://%s;databaseName=%s;encrypt=true;trustServerCertificate=true",
                server, database);
         **/
        // Oracle connection format
        String url = String.format("jdbc:oracle:thin:@%s:1521:%s", server, database);

        try (Connection conn = DriverManager.getConnection(url, username, password)) {
            String sql = "SELECT r.rsrc_id, r.rsrc_name, r.rsrc_short_name, r.email_addr, " +
                    "r.office_phone, r.rsrc_title, r.rsrc_type, r.rsrc_notes, " +
                    "r.calendar_id, c.clndr_name, r.created_by, " +
                    "r.create_date, r.update_date " +
                    "FROM RSRC r " +
                    "LEFT JOIN CALENDAR c ON r.calendar_id = c.clndr_id " +
                    "WHERE r.rsrc_id = ?";

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, resourceId);

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        Map<String, Object> resource = new HashMap<>();
                        ResultSetMetaData metaData = rs.getMetaData();
                        int columnCount = metaData.getColumnCount();

                        for (int i = 1; i <= columnCount; i++) {
                            String columnName = metaData.getColumnName(i);
                            Object value = rs.getObject(i);
                            resource.put(columnName, value);
                        }

                        return resource;
                    }
                }
            }
        }

        return null; // Resource not found
    }

    /**
     * Get resources assigned to a specific project.
     *
     * @param server The database server address
     * @param database The database name
     * @param username Database username
     * @param password Database password
     * @param projectId The project ID
     * @return List of resources assigned to the project
     * @throws SQLException If a database error occurs
     */
    public List<Map<String, Object>> getResourcesByProject(String server, String database,
                                                           String username, String password,
                                                           String projectId) throws SQLException {

           /** P6 SQL CONNECTION
        String url = String.format("jdbc:sqlserver://%s;databaseName=%s;encrypt=true;trustServerCertificate=true",
                server, database);
         **/
        // Oracle connection format
        String url = String.format("jdbc:oracle:thin:@%s:1521:%s", server, database);

        List<Map<String, Object>> resources = new ArrayList<>();

        try (Connection conn = DriverManager.getConnection(url, username, password)) {
            String sql = "SELECT DISTINCT r.rsrc_id, r.rsrc_name, r.rsrc_short_name, r.email_addr, " +
                    "r.office_phone, r.rsrc_title, r.rsrc_type " +
                    "FROM RSRC r " +
                    "JOIN TASKRSRC tr ON r.rsrc_id = tr.rsrc_id " +
                    "JOIN TASK t ON tr.task_id = t.task_id " +
                    "JOIN PROJECT p ON t.proj_id = p.proj_id " +
                    "WHERE p.proj_id = ? " +
                    "ORDER BY r.rsrc_name";

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, projectId);

                try (ResultSet rs = stmt.executeQuery()) {
                    ResultSetMetaData metaData = rs.getMetaData();
                    int columnCount = metaData.getColumnCount();

                    while (rs.next()) {
                        Map<String, Object> resource = new HashMap<>();

                        for (int i = 1; i <= columnCount; i++) {
                            String columnName = metaData.getColumnName(i);
                            Object value = rs.getObject(i);
                            resource.put(columnName, value);
                        }

                        resources.add(resource);
                    }
                }
            }
        }

        return resources;
    }

    /**
     * Get resource assignments for a specific activity.
     *
     * @param server The database server address
     * @param database The database name
     * @param username Database username
     * @param password Database password
     * @param activityId The activity ID
     * @return List of resource assignments for the activity
     * @throws SQLException If a database error occurs
     */
    public List<Map<String, Object>> getResourceAssignmentsByActivity(String server, String database,
                                                                      String username, String password,
                                                                      String activityId) throws SQLException {

           /** P6 SQL CONNECTION
        String url = String.format("jdbc:sqlserver://%s;databaseName=%s;encrypt=true;trustServerCertificate=true",
                server, database);
         **/
        // Oracle connection format
        String url = String.format("jdbc:oracle:thin:@%s:1521:%s", server, database);

        List<Map<String, Object>> assignments = new ArrayList<>();

        try (Connection conn = DriverManager.getConnection(url, username, password)) {
            String sql = "SELECT tr.taskrsrc_id, tr.task_id, tr.rsrc_id, r.rsrc_name, " +
                    "tr.remain_qty, tr.target_qty, tr.act_qty, tr.remain_cost, " +
                    "tr.act_cost, tr.target_cost, a.activity_name " +
                    "FROM TASKRSRC tr " +
                    "JOIN RSRC r ON tr.rsrc_id = r.rsrc_id " +
                    "JOIN ACTIVITIES a ON tr.task_id = a.activity_id " +
                    "WHERE tr.task_id = ?";

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, activityId);

                try (ResultSet rs = stmt.executeQuery()) {
                    ResultSetMetaData metaData = rs.getMetaData();
                    int columnCount = metaData.getColumnCount();

                    while (rs.next()) {
                        Map<String, Object> assignment = new HashMap<>();

                        for (int i = 1; i <= columnCount; i++) {
                            String columnName = metaData.getColumnName(i);
                            Object value = rs.getObject(i);
                            assignment.put(columnName, value);
                        }

                        assignments.add(assignment);
                    }
                }
            }
        }

        return assignments;
    }

    /**
     * Get resource availability (not assigned during a period).
     *
     * @param server The database server address
     * @param database The database name
     * @param username Database username
     * @param password Database password
     * @param startDate The start date in format YYYY-MM-DD
     * @param endDate The end date in format YYYY-MM-DD
     * @return List of available resources with their availability hours
     * @throws SQLException If a database error occurs
     */
    public List<Map<String, Object>> getResourceAvailability(String server, String database,
                                                             String username, String password,
                                                             String startDate, String endDate) throws SQLException {

           /** P6 SQL CONNECTION
        String url = String.format("jdbc:sqlserver://%s;databaseName=%s;encrypt=true;trustServerCertificate=true",
                server, database);
         **/
        // Oracle connection format
        String url = String.format("jdbc:oracle:thin:@%s:1521:%s", server, database);

        List<Map<String, Object>> availability = new ArrayList<>();

        try (Connection conn = DriverManager.getConnection(url, username, password)) {
            // This is a simplified query - actual resource availability calculation would be more complex
            String sql = "SELECT r.rsrc_id, r.rsrc_name, r.rsrc_short_name, " +
                    "r.max_qty_per_hr, r.unit_id, ru.unit_name, " +
                    "COALESCE(ra.avail_qty, r.max_qty_per_hr) as available_hours " +
                    "FROM RSRC r " +
                    "LEFT JOIN RSRCRATE rr ON r.rsrc_id = rr.rsrc_id " +
                    "LEFT JOIN RSRCAVAIL ra ON r.rsrc_id = ra.rsrc_id " +
                    "LEFT JOIN UNIT ru ON r.unit_id = ru.unit_id " +
                    "WHERE (ra.start_date <= ? AND (ra.finish_date >= ? OR ra.finish_date IS NULL)) " +
                    "OR ra.rsrc_id IS NULL " +
                    "ORDER BY r.rsrc_name";

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, endDate);
                stmt.setString(2, startDate);

                try (ResultSet rs = stmt.executeQuery()) {
                    ResultSetMetaData metaData = rs.getMetaData();
                    int columnCount = metaData.getColumnCount();

                    while (rs.next()) {
                        Map<String, Object> resource = new HashMap<>();

                        for (int i = 1; i <= columnCount; i++) {
                            String columnName = metaData.getColumnName(i);
                            Object value = rs.getObject(i);
                            resource.put(columnName, value);
                        }

                        availability.add(resource);
                    }
                }
            }
        }

        return availability;
    }

    /**
     * Display resources in a formatted manner (useful for debugging and testing).
     *
     * @param resources List of resource maps to display
     */
    public void displayResources(List<Map<String, Object>> resources) {
        System.out.println("Found " + resources.size() + " resources");

        if (!resources.isEmpty()) {
            // Print table header
            Map<String, Object> firstResource = resources.get(0);
            for (String key : firstResource.keySet()) {
                System.out.print(key + "\t");
            }
            System.out.println();

            // Print separator
            for (int i = 0; i < 100; i++) {
                System.out.print("-");
            }
            System.out.println();

            // Print resource data
            for (Map<String, Object> resource : resources) {
                for (Object value : resource.values()) {
                    System.out.print((value != null ? value.toString() : "NULL") + "\t");
                }
                System.out.println();
            }
        }
    }
}
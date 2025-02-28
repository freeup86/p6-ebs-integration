package com.tpcgrp.p6ebs.service;

import java.sql.*;
import java.util.*;

/**
 * Service class for retrieving projects from Primavera P6 database.
 */
public class P6ProjectService {

    private final DatabaseService databaseService;

    public P6ProjectService(DatabaseService databaseService) {
        this.databaseService = databaseService;
    }

    /**
     * Retrieves all projects from P6 database.
     *
     * @param server The database server address
     * @param database The database name
     * @param username Database username
     * @param password Database password
     * @return List of project maps containing all project data
     * @throws SQLException If a database error occurs
     */
    public List<Map<String, Object>> getAllProjects(String server, String database,
                                                    String username, String password) throws SQLException {

           /** P6 SQL CONNECTION
        String url = String.format("jdbc:sqlserver://%s;databaseName=%s;encrypt=true;trustServerCertificate=true",
                server, database);
         **/
        // Oracle connection format
        String url = String.format("jdbc:oracle:thin:@%s:1521:%s", server, database);

        List<Map<String, Object>> projects = new ArrayList<>();

        try (Connection conn = DriverManager.getConnection(url, username, password)) {
            // SQL query to get projects with key information
            String sql = "SELECT p.proj_id, p.proj_name, p.proj_short_name, p.status_code, p.wbs_max_sum_level, " +
                    "p.last_recalc_date, p.plan_start_date, p.plan_end_date, p.scd_start_date, " +
                    "p.scd_end_date, p.act_start_date, p.act_end_date, p.create_date, p.update_date, " +
                    "ps.proj_short_name as parent_proj, u.user_name as created_by, " +
                    "p.sum_data_flag, p.clndr_id, c.clndr_name " +
                    "FROM PROJECT p " +
                    "LEFT JOIN PROJECT ps ON p.parent_proj_id = ps.proj_id " +
                    "LEFT JOIN USERS u ON p.created_by = u.user_id " +
                    "LEFT JOIN CALENDAR c ON p.clndr_id = c.clndr_id " +
                    "ORDER BY p.proj_name";

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
     * Get project details by project ID.
     *
     * @param server The database server address
     * @param database The database name
     * @param username Database username
     * @param password Database password
     * @param projectId The project ID to look up
     * @return Map containing project details or null if not found
     * @throws SQLException If a database error occurs
     */
    public Map<String, Object> getProjectById(String server, String database,
                                              String username, String password,
                                              String projectId) throws SQLException {

           /** P6 SQL CONNECTION
        String url = String.format("jdbc:sqlserver://%s;databaseName=%s;encrypt=true;trustServerCertificate=true",
                server, database);
         **/
        // Oracle connection format
        String url = String.format("jdbc:oracle:thin:@%s:1521:%s", server, database);

        try (Connection conn = DriverManager.getConnection(url, username, password)) {
            String sql = "SELECT p.proj_id, p.proj_name, p.proj_short_name, p.status_code, p.wbs_max_sum_level, " +
                    "p.last_recalc_date, p.plan_start_date, p.plan_end_date, p.scd_start_date, " +
                    "p.scd_end_date, p.act_start_date, p.act_end_date, p.create_date, p.update_date, " +
                    "p.parent_proj_id, ps.proj_short_name as parent_proj, u.user_name as created_by, " +
                    "p.rsrc_self_add_flag, p.allow_complete_flag, p.sum_data_flag, p.clndr_id, c.clndr_name, " +
                    "p.default_cost_per_qty, p.task_code_base, p.task_code_step, " +
                    "p.priority_defaults_flag, p.last_financial_period_id, p.last_baseline_update_date " +
                    "FROM PROJECT p " +
                    "LEFT JOIN PROJECT ps ON p.parent_proj_id = ps.proj_id " +
                    "LEFT JOIN USERS u ON p.created_by = u.user_id " +
                    "LEFT JOIN CALENDAR c ON p.clndr_id = c.clndr_id " +
                    "WHERE p.proj_id = ?";

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, projectId);

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        Map<String, Object> project = new HashMap<>();
                        ResultSetMetaData metaData = rs.getMetaData();
                        int columnCount = metaData.getColumnCount();

                        for (int i = 1; i <= columnCount; i++) {
                            String columnName = metaData.getColumnName(i);
                            Object value = rs.getObject(i);
                            project.put(columnName, value);
                        }

                        return project;
                    }
                }
            }
        }

        return null; // Project not found
    }

    /**
     * Get projects by their status code.
     *
     * @param server The database server address
     * @param database The database name
     * @param username Database username
     * @param password Database password
     * @param statusCode The status code to filter by
     * @return List of projects with the specified status
     * @throws SQLException If a database error occurs
     */
    public List<Map<String, Object>> getProjectsByStatus(String server, String database,
                                                         String username, String password,
                                                         String statusCode) throws SQLException {

           /** P6 SQL CONNECTION
        String url = String.format("jdbc:sqlserver://%s;databaseName=%s;encrypt=true;trustServerCertificate=true",
                server, database);
         **/
        // Oracle connection format
        String url = String.format("jdbc:oracle:thin:@%s:1521:%s", server, database);

        List<Map<String, Object>> projects = new ArrayList<>();

        try (Connection conn = DriverManager.getConnection(url, username, password)) {
            String sql = "SELECT p.proj_id, p.proj_name, p.proj_short_name, p.status_code, " +
                    "p.plan_start_date, p.plan_end_date, p.act_start_date, p.act_end_date, " +
                    "ps.proj_short_name as parent_proj, u.user_name as created_by " +
                    "FROM PROJECT p " +
                    "LEFT JOIN PROJECT ps ON p.parent_proj_id = ps.proj_id " +
                    "LEFT JOIN USERS u ON p.created_by = u.user_id " +
                    "WHERE p.status_code = ? " +
                    "ORDER BY p.proj_name";

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, statusCode);

                try (ResultSet rs = stmt.executeQuery()) {
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
        }

        return projects;
    }

    /**
     * Get projects where a specific resource is assigned.
     *
     * @param server The database server address
     * @param database The database name
     * @param username Database username
     * @param password Database password
     * @param resourceId The resource ID
     * @return List of projects where the resource is assigned
     * @throws SQLException If a database error occurs
     */
    public List<Map<String, Object>> getProjectsByResource(String server, String database,
                                                           String username, String password,
                                                           String resourceId) throws SQLException {

           /** P6 SQL CONNECTION
        String url = String.format("jdbc:sqlserver://%s;databaseName=%s;encrypt=true;trustServerCertificate=true",
                server, database);
         **/
        // Oracle connection format
        String url = String.format("jdbc:oracle:thin:@%s:1521:%s", server, database);

        List<Map<String, Object>> projects = new ArrayList<>();

        try (Connection conn = DriverManager.getConnection(url, username, password)) {
            String sql = "SELECT DISTINCT p.proj_id, p.proj_name, p.proj_short_name, p.status_code, " +
                    "p.plan_start_date, p.plan_end_date, p.act_start_date, p.act_end_date " +
                    "FROM PROJECT p " +
                    "JOIN TASK t ON p.proj_id = t.proj_id " +
                    "JOIN TASKRSRC tr ON t.task_id = tr.task_id " +
                    "WHERE tr.rsrc_id = ? " +
                    "ORDER BY p.proj_name";

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, resourceId);

                try (ResultSet rs = stmt.executeQuery()) {
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
        }

        return projects;
    }

    /**
     * Get projects that are scheduled to occur within a specified date range.
     *
     * @param server The database server address
     * @param database The database name
     * @param username Database username
     * @param password Database password
     * @param startDate The start date (YYYY-MM-DD format)
     * @param endDate The end date (YYYY-MM-DD format)
     * @return List of projects scheduled within the date range
     * @throws SQLException If a database error occurs
     */
    public List<Map<String, Object>> getProjectsByDateRange(String server, String database,
                                                            String username, String password,
                                                            String startDate, String endDate) throws SQLException {

           /** P6 SQL CONNECTION
        String url = String.format("jdbc:sqlserver://%s;databaseName=%s;encrypt=true;trustServerCertificate=true",
                server, database);
         **/
        // Oracle connection format
        String url = String.format("jdbc:oracle:thin:@%s:1521:%s", server, database);

        List<Map<String, Object>> projects = new ArrayList<>();

        try (Connection conn = DriverManager.getConnection(url, username, password)) {
            String sql = "SELECT p.proj_id, p.proj_name, p.proj_short_name, p.status_code, " +
                    "p.plan_start_date, p.plan_end_date, p.act_start_date, p.act_end_date " +
                    "FROM PROJECT p " +
                    "WHERE (p.plan_start_date <= ? AND p.plan_end_date >= ?) " +
                    "OR (p.act_start_date <= ? AND p.act_end_date >= ?) " +
                    "OR (p.plan_start_date <= ? AND p.act_end_date >= ?) " +
                    "OR (p.act_start_date <= ? AND p.plan_end_date >= ?) " +
                    "ORDER BY p.proj_name";

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, endDate);
                stmt.setString(2, startDate);
                stmt.setString(3, endDate);
                stmt.setString(4, startDate);
                stmt.setString(5, endDate);
                stmt.setString(6, startDate);
                stmt.setString(7, endDate);
                stmt.setString(8, startDate);

                try (ResultSet rs = stmt.executeQuery()) {
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
        }

        return projects;
    }

    /**
     * Get project summary information including costs, activities count, etc.
     *
     * @param server The database server address
     * @param database The database name
     * @param username Database username
     * @param password Database password
     * @param projectId The project ID
     * @return Map containing project summary information
     * @throws SQLException If a database error occurs
     */
    public Map<String, Object> getProjectSummary(String server, String database,
                                                 String username, String password,
                                                 String projectId) throws SQLException {

           /** P6 SQL CONNECTION
        String url = String.format("jdbc:sqlserver://%s;databaseName=%s;encrypt=true;trustServerCertificate=true",
                server, database);
         **/
        // Oracle connection format
        String url = String.format("jdbc:oracle:thin:@%s:1521:%s", server, database);

        Map<String, Object> summary = new HashMap<>();

        try (Connection conn = DriverManager.getConnection(url, username, password)) {
            // Get basic project info
            String projectSql = "SELECT p.proj_name, p.proj_short_name, p.status_code, " +
                    "p.plan_start_date, p.plan_end_date, p.act_start_date, p.act_end_date " +
                    "FROM PROJECT p WHERE p.proj_id = ?";

            try (PreparedStatement stmt = conn.prepareStatement(projectSql)) {
                stmt.setString(1, projectId);

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        summary.put("proj_name", rs.getString("proj_name"));
                        summary.put("proj_short_name", rs.getString("proj_short_name"));
                        summary.put("status_code", rs.getString("status_code"));
                        summary.put("plan_start_date", rs.getDate("plan_start_date"));
                        summary.put("plan_end_date", rs.getDate("plan_end_date"));
                        summary.put("act_start_date", rs.getDate("act_start_date"));
                        summary.put("act_end_date", rs.getDate("act_end_date"));
                    }
                }
            }

            // Get activity counts
            String activityCountSql = "SELECT COUNT(*) as total_activities, " +
                    "SUM(CASE WHEN status_code = 'Not Started' THEN 1 ELSE 0 END) as not_started, " +
                    "SUM(CASE WHEN status_code = 'In Progress' THEN 1 ELSE 0 END) as in_progress, " +
                    "SUM(CASE WHEN status_code = 'Completed' THEN 1 ELSE 0 END) as completed " +
                    "FROM ACTIVITIES WHERE proj_id = ?";

            try (PreparedStatement stmt = conn.prepareStatement(activityCountSql)) {
                stmt.setString(1, projectId);

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        summary.put("total_activities", rs.getInt("total_activities"));
                        summary.put("not_started_activities", rs.getInt("not_started"));
                        summary.put("in_progress_activities", rs.getInt("in_progress"));
                        summary.put("completed_activities", rs.getInt("completed"));
                    }
                }
            }

            // Get resource counts
            String resourceCountSql = "SELECT COUNT(DISTINCT tr.rsrc_id) as total_resources " +
                    "FROM TASKRSRC tr " +
                    "JOIN TASK t ON tr.task_id = t.task_id " +
                    "WHERE t.proj_id = ?";

            try (PreparedStatement stmt = conn.prepareStatement(resourceCountSql)) {
                stmt.setString(1, projectId);

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        summary.put("total_resources", rs.getInt("total_resources"));
                    }
                }
            }

            // Get cost summary
            String costSql = "SELECT SUM(tr.target_cost) as planned_cost, " +
                    "SUM(tr.act_cost) as actual_cost, " +
                    "SUM(tr.remain_cost) as remaining_cost " +
                    "FROM TASKRSRC tr " +
                    "JOIN TASK t ON tr.task_id = t.task_id " +
                    "WHERE t.proj_id = ?";

            try (PreparedStatement stmt = conn.prepareStatement(costSql)) {
                stmt.setString(1, projectId);

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        summary.put("planned_cost", rs.getDouble("planned_cost"));
                        summary.put("actual_cost", rs.getDouble("actual_cost"));
                        summary.put("remaining_cost", rs.getDouble("remaining_cost"));
                    }
                }
            }
        }

        return summary;
    }

    /**
     * Display projects in a formatted manner (useful for debugging and testing).
     *
     * @param projects List of project maps to display
     */
    public void displayProjects(List<Map<String, Object>> projects) {
        System.out.println("Found " + projects.size() + " projects");

        if (!projects.isEmpty()) {
            // Print table header
            Map<String, Object> firstProject = projects.get(0);
            for (String key : firstProject.keySet()) {
                System.out.print(key + "\t");
            }
            System.out.println();

            // Print separator
            for (int i = 0; i < 100; i++) {
                System.out.print("-");
            }
            System.out.println();

            // Print project data
            for (Map<String, Object> project : projects) {
                for (Object value : project.values()) {
                    System.out.print((value != null ? value.toString() : "NULL") + "\t");
                }
                System.out.println();
            }
        }
    }
}
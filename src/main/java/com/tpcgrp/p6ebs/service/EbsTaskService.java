package com.tpcgrp.p6ebs.service;

import java.sql.*;
import java.util.*;

/**
 * Service class for retrieving tasks from Oracle EBS database.
 */
public class EbsTaskService {

    private final DatabaseService databaseService;

    public EbsTaskService(DatabaseService databaseService) {
        this.databaseService = databaseService;
    }

    /**
     * Retrieves all tasks from Oracle EBS database.
     *
     * @param server The database server address
     * @param sid The Oracle SID
     * @param username Database username
     * @param password Database password
     * @return List of task maps containing all task data
     * @throws SQLException If a database error occurs
     */
    public List<Map<String, Object>> getAllTasks(String server, String sid,
                                                 String username, String password) throws SQLException {

        String url = String.format("jdbc:oracle:thin:@%s:1521:%s", server, sid);

        List<Map<String, Object>> tasks = new ArrayList<>();

        try (Connection conn = DriverManager.getConnection(url, username, password)) {
            // SQL query to get tasks with key information
            String sql = "SELECT t.task_id, t.task_number, t.task_name, t.description, " +
                    "t.project_id, p.segment1 as project_number, p.name as project_name, " +
                    "t.start_date, t.completion_date, " +
                    "t.actual_start_date, t.actual_finish_date, " +
                    "t.planned_duration, t.actual_duration, " +
                    "t.task_status_code, ts.task_status_name, " +
                    "t.wbs_level, t.parent_task_id, pt.task_name as parent_task_name, " +
                    "t.creation_date, t.last_update_date " +
                    "FROM pa_tasks t " +
                    "JOIN pa_projects_all p ON t.project_id = p.project_id " +
                    "JOIN pa_task_statuses_v ts ON t.task_status_code = ts.task_status_code " +
                    "LEFT JOIN pa_tasks pt ON t.parent_task_id = pt.task_id " +
                    "ORDER BY p.segment1, t.task_number";

            try (PreparedStatement stmt = conn.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {

                ResultSetMetaData metaData = rs.getMetaData();
                int columnCount = metaData.getColumnCount();

                while (rs.next()) {
                    Map<String, Object> task = new HashMap<>();

                    for (int i = 1; i <= columnCount; i++) {
                        String columnName = metaData.getColumnName(i);
                        Object value = rs.getObject(i);
                        task.put(columnName, value);
                    }

                    tasks.add(task);
                }
            }
        }

        return tasks;
    }

    /**
     * Get task details by task ID.
     *
     * @param server The database server address
     * @param sid The Oracle SID
     * @param username Database username
     * @param password Database password
     * @param taskId The task ID to look up
     * @return Map containing task details or null if not found
     * @throws SQLException If a database error occurs
     */
    public Map<String, Object> getTaskById(String server, String sid,
                                           String username, String password,
                                           String taskId) throws SQLException {

        String url = String.format("jdbc:oracle:thin:@%s:1521:%s", server, sid);

        try (Connection conn = DriverManager.getConnection(url, username, password)) {
            String sql = "SELECT t.task_id, t.task_number, t.task_name, t.description, " +
                    "t.project_id, p.segment1 as project_number, p.name as project_name, " +
                    "t.start_date, t.completion_date, " +
                    "t.actual_start_date, t.actual_finish_date, " +
                    "t.scheduled_start_date, t.scheduled_finish_date, " +
                    "t.planned_duration, t.actual_duration, " +
                    "t.task_status_code, ts.task_status_name, " +
                    "t.wbs_level, t.parent_task_id, pt.task_name as parent_task_name, " +
                    "t.billable_flag, t.service_type_code, t.creation_date, t.last_update_date " +
                    "FROM pa_tasks t " +
                    "JOIN pa_projects_all p ON t.project_id = p.project_id " +
                    "JOIN pa_task_statuses_v ts ON t.task_status_code = ts.task_status_code " +
                    "LEFT JOIN pa_tasks pt ON t.parent_task_id = pt.task_id " +
                    "WHERE t.task_id = ?";

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, taskId);

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        Map<String, Object> task = new HashMap<>();
                        ResultSetMetaData metaData = rs.getMetaData();
                        int columnCount = metaData.getColumnCount();

                        for (int i = 1; i <= columnCount; i++) {
                            String columnName = metaData.getColumnName(i);
                            Object value = rs.getObject(i);
                            task.put(columnName, value);
                        }

                        return task;
                    }
                }
            }
        }

        return null; // Task not found
    }

    /**
     * Get tasks for a specific project.
     *
     * @param server The database server address
     * @param sid The Oracle SID
     * @param username Database username
     * @param password Database password
     * @param projectId The project ID
     * @return List of tasks for the specified project
     * @throws SQLException If a database error occurs
     */
    public List<Map<String, Object>> getTasksByProject(String server, String sid,
                                                       String username, String password,
                                                       String projectId) throws SQLException {

        String url = String.format("jdbc:oracle:thin:@%s:1521:%s", server, sid);

        List<Map<String, Object>> tasks = new ArrayList<>();

        try (Connection conn = DriverManager.getConnection(url, username, password)) {
            String sql = "SELECT t.task_id, t.task_number, t.task_name, t.description, " +
                    "t.start_date, t.completion_date, " +
                    "t.actual_start_date, t.actual_finish_date, " +
                    "t.planned_duration, t.actual_duration, " +
                    "t.task_status_code, ts.task_status_name, " +
                    "t.wbs_level, t.parent_task_id, pt.task_name as parent_task_name " +
                    "FROM pa_tasks t " +
                    "JOIN pa_task_statuses_v ts ON t.task_status_code = ts.task_status_code " +
                    "LEFT JOIN pa_tasks pt ON t.parent_task_id = pt.task_id " +
                    "WHERE t.project_id = ? " +
                    "ORDER BY t.wbs_level, t.task_number";

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, projectId);

                try (ResultSet rs = stmt.executeQuery()) {
                    ResultSetMetaData metaData = rs.getMetaData();
                    int columnCount = metaData.getColumnCount();

                    while (rs.next()) {
                        Map<String, Object> task = new HashMap<>();

                        for (int i = 1; i <= columnCount; i++) {
                            String columnName = metaData.getColumnName(i);
                            Object value = rs.getObject(i);
                            task.put(columnName, value);
                        }

                        tasks.add(task);
                    }
                }
            }
        }

        return tasks;
    }

    /**
     * Get tasks by status.
     *
     * @param server The database server address
     * @param sid The Oracle SID
     * @param username Database username
     * @param password Database password
     * @param statusCode The task status code
     * @return List of tasks with the specified status
     * @throws SQLException If a database error occurs
     */
    public List<Map<String, Object>> getTasksByStatus(String server, String sid,
                                                      String username, String password,
                                                      String statusCode) throws SQLException {

        String url = String.format("jdbc:oracle:thin:@%s:1521:%s", server, sid);

        List<Map<String, Object>> tasks = new ArrayList<>();

        try (Connection conn = DriverManager.getConnection(url, username, password)) {
            String sql = "SELECT t.task_id, t.task_number, t.task_name, " +
                    "t.project_id, p.segment1 as project_number, p.name as project_name, " +
                    "t.start_date, t.completion_date, " +
                    "t.task_status_code, ts.task_status_name " +
                    "FROM pa_tasks t " +
                    "JOIN pa_projects_all p ON t.project_id = p.project_id " +
                    "JOIN pa_task_statuses_v ts ON t.task_status_code = ts.task_status_code " +
                    "WHERE t.task_status_code = ? " +
                    "ORDER BY p.segment1, t.task_number";

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, statusCode);

                try (ResultSet rs = stmt.executeQuery()) {
                    ResultSetMetaData metaData = rs.getMetaData();
                    int columnCount = metaData.getColumnCount();

                    while (rs.next()) {
                        Map<String, Object> task = new HashMap<>();

                        for (int i = 1; i <= columnCount; i++) {
                            String columnName = metaData.getColumnName(i);
                            Object value = rs.getObject(i);
                            task.put(columnName, value);
                        }

                        tasks.add(task);
                    }
                }
            }
        }

        return tasks;
    }

    /**
     * Get tasks by date range (tasks active during this period).
     *
     * @param server The database server address
     * @param sid The Oracle SID
     * @param username Database username
     * @param password Database password
     * @param startDate The start date (in format 'YYYY-MM-DD')
     * @param endDate The end date (in format 'YYYY-MM-DD')
     * @return List of tasks active during the date range
     * @throws SQLException If a database error occurs
     */
    public List<Map<String, Object>> getTasksByDateRange(String server, String sid,
                                                         String username, String password,
                                                         String startDate, String endDate) throws SQLException {

        String url = String.format("jdbc:oracle:thin:@%s:1521:%s", server, sid);

        List<Map<String, Object>> tasks = new ArrayList<>();

        try (Connection conn = DriverManager.getConnection(url, username, password)) {
            String sql = "SELECT t.task_id, t.task_number, t.task_name, " +
                    "t.project_id, p.segment1 as project_number, p.name as project_name, " +
                    "t.start_date, t.completion_date, " +
                    "t.actual_start_date, t.actual_finish_date, " +
                    "t.task_status_code, ts.task_status_name " +
                    "FROM pa_tasks t " +
                    "JOIN pa_projects_all p ON t.project_id = p.project_id " +
                    "JOIN pa_task_statuses_v ts ON t.task_status_code = ts.task_status_code " +
                    "WHERE (t.start_date <= TO_DATE(?, 'YYYY-MM-DD') AND " +
                    "       t.completion_date >= TO_DATE(?, 'YYYY-MM-DD')) OR " +
                    "      (t.start_date BETWEEN TO_DATE(?, 'YYYY-MM-DD') AND TO_DATE(?, 'YYYY-MM-DD')) OR " +
                    "      (t.completion_date BETWEEN TO_DATE(?, 'YYYY-MM-DD') AND TO_DATE(?, 'YYYY-MM-DD')) " +
                    "ORDER BY p.segment1, t.task_number";

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, endDate);
                stmt.setString(2, startDate);
                stmt.setString(3, startDate);
                stmt.setString(4, endDate);
                stmt.setString(5, startDate);
                stmt.setString(6, endDate);

                try (ResultSet rs = stmt.executeQuery()) {
                    ResultSetMetaData metaData = rs.getMetaData();
                    int columnCount = metaData.getColumnCount();

                    while (rs.next()) {
                        Map<String, Object> task = new HashMap<>();

                        for (int i = 1; i <= columnCount; i++) {
                            String columnName = metaData.getColumnName(i);
                            Object value = rs.getObject(i);
                            task.put(columnName, value);
                        }

                        tasks.add(task);
                    }
                }
            }
        }

        return tasks;
    }

    /**
     * Get child tasks for a parent task.
     *
     * @param server The database server address
     * @param sid The Oracle SID
     * @param username Database username
     * @param password Database password
     * @param parentTaskId The parent task ID
     * @return List of child tasks
     * @throws SQLException If a database error occurs
     */
    public List<Map<String, Object>> getChildTasks(String server, String sid,
                                                   String username, String password,
                                                   String parentTaskId) throws SQLException {

        String url = String.format("jdbc:oracle:thin:@%s:1521:%s", server, sid);

        List<Map<String, Object>> tasks = new ArrayList<>();

        try (Connection conn = DriverManager.getConnection(url, username, password)) {
            String sql = "SELECT t.task_id, t.task_number, t.task_name, t.description, " +
                    "t.start_date, t.completion_date, " +
                    "t.planned_duration, t.actual_duration, " +
                    "t.task_status_code, ts.task_status_name, " +
                    "t.wbs_level " +
                    "FROM pa_tasks t " +
                    "JOIN pa_task_statuses_v ts ON t.task_status_code = ts.task_status_code " +
                    "WHERE t.parent_task_id = ? " +
                    "ORDER BY t.task_number";

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, parentTaskId);

                try (ResultSet rs = stmt.executeQuery()) {
                    ResultSetMetaData metaData = rs.getMetaData();
                    int columnCount = metaData.getColumnCount();

                    while (rs.next()) {
                        Map<String, Object> task = new HashMap<>();

                        for (int i = 1; i <= columnCount; i++) {
                            String columnName = metaData.getColumnName(i);
                            Object value = rs.getObject(i);
                            task.put(columnName, value);
                        }

                        tasks.add(task);
                    }
                }
            }
        }

        return tasks;
    }

    /**
     * Get task resources (assignments).
     *
     * @param server The database server address
     * @param sid The Oracle SID
     * @param username Database username
     * @param password Database password
     * @param taskId The task ID
     * @return List of resources assigned to the task
     * @throws SQLException If a database error occurs
     */
    public List<Map<String, Object>> getTaskResources(String server, String sid,
                                                      String username, String password,
                                                      String taskId) throws SQLException {

        String url = String.format("jdbc:oracle:thin:@%s:1521:%s", server, sid);

        List<Map<String, Object>> resources = new ArrayList<>();

        try (Connection conn = DriverManager.getConnection(url, username, password)) {
            String sql = "SELECT ta.task_assignment_id, ta.task_id, ta.person_id, " +
                    "per.full_name as resource_name, per.email_address, " +
                    "ta.assignment_start_date, ta.assignment_end_date, " +
                    "ta.assigned_units, ta.planned_effort, ta.actual_effort, " +
                    "ta.assignment_status_code, tas.assignment_status_name " +
                    "FROM pa_task_assignments ta " +
                    "JOIN pa_assignment_statuses_v tas ON ta.assignment_status_code = tas.assignment_status_code " +
                    "JOIN per_all_people_f per ON ta.person_id = per.person_id " +
                    "WHERE ta.task_id = ? " +
                    "ORDER BY per.full_name";

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, taskId);

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
     * Get task financial information.
     *
     * @param server The database server address
     * @param sid The Oracle SID
     * @param username Database username
     * @param password Database password
     * @param taskId The task ID
     * @return Map containing task financial information
     * @throws SQLException If a database error occurs
     */
    public Map<String, Object> getTaskFinancialInfo(String server, String sid,
                                                    String username, String password,
                                                    String taskId) throws SQLException {

        String url = String.format("jdbc:oracle:thin:@%s:1521:%s", server, sid);

        Map<String, Object> financialInfo = new HashMap<>();

        try (Connection conn = DriverManager.getConnection(url, username, password)) {
            // Get task basic info
            String taskSql = "SELECT t.task_id, t.task_number, t.task_name, " +
                    "p.segment1 as project_number, p.name as project_name, " +
                    "ppc.project_currency_code " +
                    "FROM pa_tasks t " +
                    "JOIN pa_projects_all p ON t.project_id = p.project_id " +
                    "JOIN pa_project_currencies ppc ON p.project_id = ppc.project_id " +
                    "WHERE t.task_id = ?";

            try (PreparedStatement stmt = conn.prepareStatement(taskSql)) {
                stmt.setString(1, taskId);

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        financialInfo.put("task_id", rs.getString("task_id"));
                        financialInfo.put("task_number", rs.getString("task_number"));
                        financialInfo.put("task_name", rs.getString("task_name"));
                        financialInfo.put("project_number", rs.getString("project_number"));
                        financialInfo.put("project_name", rs.getString("project_name"));
                        financialInfo.put("currency_code", rs.getString("project_currency_code"));
                    }
                }
            }

            // Get budget data
            String budgetSql = "SELECT SUM(tb.budget_amount) as budgeted_amount " +
                    "FROM pa_task_budgets tb " +
                    "WHERE tb.task_id = ?";

            try (PreparedStatement stmt = conn.prepareStatement(budgetSql)) {
                stmt.setString(1, taskId);

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        financialInfo.put("budgeted_amount", rs.getDouble("budgeted_amount"));
                    }
                }
            }

            // Get actual costs
            String costSql = "SELECT SUM(pei.burdened_cost) as actual_cost " +
                    "FROM pa_expenditure_items_all pei " +
                    "WHERE pei.task_id = ?";

            try (PreparedStatement stmt = conn.prepareStatement(costSql)) {
                stmt.setString(1, taskId);

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        financialInfo.put("actual_cost", rs.getDouble("actual_cost"));
                    }
                }
            }

            // Get commitment data
            String commitmentSql = "SELECT SUM(pc.commitment_amount) as committed_amount " +
                    "FROM pa_commitments pc " +
                    "WHERE pc.task_id = ?";

            try (PreparedStatement stmt = conn.prepareStatement(commitmentSql)) {
                stmt.setString(1, taskId);

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        financialInfo.put("committed_amount", rs.getDouble("committed_amount"));
                    }
                }
            }

            // Calculate remaining budget
            Double budgetedAmount = (Double) financialInfo.getOrDefault("budgeted_amount", 0.0);
            Double actualCost = (Double) financialInfo.getOrDefault("actual_cost", 0.0);
            Double committedAmount = (Double) financialInfo.getOrDefault("committed_amount", 0.0);

            financialInfo.put("remaining_budget", budgetedAmount - actualCost - committedAmount);

            if (budgetedAmount > 0) {
                Double utilizationPct = (actualCost / budgetedAmount) * 100.0;
                financialInfo.put("budget_utilization_pct", utilizationPct);
            }
        }

        return financialInfo;
    }

    /**
     * Display tasks in a formatted manner (useful for debugging and testing).
     *
     * @param tasks List of task maps to display
     */
    public void displayTasks(List<Map<String, Object>> tasks) {
        System.out.println("Found " + tasks.size() + " Oracle EBS tasks");

        if (!tasks.isEmpty()) {
            // Print table header
            Map<String, Object> firstTask = tasks.get(0);
            for (String key : firstTask.keySet()) {
                System.out.print(key + "\t");
            }
            System.out.println();

            // Print separator
            for (int i = 0; i < 100; i++) {
                System.out.print("-");
            }
            System.out.println();

            // Print task data
            for (Map<String, Object> task : tasks) {
                for (Object value : task.values()) {
                    System.out.print((value != null ? value.toString() : "NULL") + "\t");
                }
                System.out.println();
            }
        }
    }
}
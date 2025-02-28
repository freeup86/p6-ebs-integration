package com.tpcgrp.p6ebs.service;

import org.springframework.stereotype.Service;

import java.sql.*;
import java.util.*;

/**
 * Service class for retrieving projects from Oracle EBS database.
 */
public class EbsProjectService {

    private final DatabaseService databaseService;

    public EbsProjectService(DatabaseService databaseService) {
        this.databaseService = databaseService;
    }

    /**
     * Retrieves all projects from Oracle EBS database.
     *
     * @param server The database server address
     * @param sid The Oracle SID
     * @param username Database username
     * @param password Database password
     * @return List of project maps containing all project data
     * @throws SQLException If a database error occurs
     */
    public List<Map<String, Object>> getAllProjects(String server, String sid,
                                                    String username, String password) throws SQLException {

        String url = String.format("jdbc:oracle:thin:@%s:1521:%s", server, sid);

        List<Map<String, Object>> projects = new ArrayList<>();

        try (Connection conn = DriverManager.getConnection(url, username, password)) {
            // SQL query to get projects with key information
            String sql = "SELECT p.project_id, p.segment1 as project_number, p.name as project_name, " +
                    "p.description, p.start_date, p.completion_date, " +
                    "p.project_status_code, ps.project_status_name, " +
                    "pt.project_type_name, p.carrying_out_organization_id, " +
                    "org.name as organization_name, p.created_by, " +
                    "p.creation_date, p.last_updated_by, p.last_update_date, " +
                    "ppc.project_currency_code " +
                    "FROM pa_projects_all p " +
                    "JOIN pa_project_statuses_v ps ON p.project_status_code = ps.project_status_code " +
                    "JOIN pa_project_types pt ON p.project_type_code = pt.project_type_code " +
                    "JOIN hr_all_organization_units org ON p.carrying_out_organization_id = org.organization_id " +
                    "JOIN pa_project_currencies ppc ON p.project_id = ppc.project_id " +
                    "ORDER BY p.segment1";

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
     * @param sid The Oracle SID
     * @param username Database username
     * @param password Database password
     * @param projectId The project ID to look up
     * @return Map containing project details or null if not found
     * @throws SQLException If a database error occurs
     */
    public Map<String, Object> getProjectById(String server, String sid,
                                              String username, String password,
                                              String projectId) throws SQLException {

        String url = String.format("jdbc:oracle:thin:@%s:1521:%s", server, sid);

        try (Connection conn = DriverManager.getConnection(url, username, password)) {
            String sql = "SELECT p.project_id, p.segment1 as project_number, p.name as project_name, " +
                    "p.description, p.long_name, p.start_date, p.completion_date, " +
                    "p.project_status_code, ps.project_status_name, " +
                    "p.project_type_code, pt.project_type_name, p.carrying_out_organization_id, " +
                    "org.name as organization_name, p.created_by, " +
                    "p.creation_date, p.last_updated_by, p.last_update_date, " +
                    "p.public_sector_flag, p.allow_cross_charge_flag, " +
                    "ppc.project_currency_code, ppc.project_functional_currency " +
                    "FROM pa_projects_all p " +
                    "JOIN pa_project_statuses_v ps ON p.project_status_code = ps.project_status_code " +
                    "JOIN pa_project_types pt ON p.project_type_code = pt.project_type_code " +
                    "JOIN hr_all_organization_units org ON p.carrying_out_organization_id = org.organization_id " +
                    "JOIN pa_project_currencies ppc ON p.project_id = ppc.project_id " +
                    "WHERE p.project_id = ?";

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
     * @param sid The Oracle SID
     * @param username Database username
     * @param password Database password
     * @param statusCode The status code to filter by
     * @return List of projects with the specified status
     * @throws SQLException If a database error occurs
     */
    public List<Map<String, Object>> getProjectsByStatus(String server, String sid,
                                                         String username, String password,
                                                         String statusCode) throws SQLException {

        String url = String.format("jdbc:oracle:thin:@%s:1521:%s", server, sid);

        List<Map<String, Object>> projects = new ArrayList<>();

        try (Connection conn = DriverManager.getConnection(url, username, password)) {
            String sql = "SELECT p.project_id, p.segment1 as project_number, p.name as project_name, " +
                    "p.description, p.start_date, p.completion_date, " +
                    "p.project_status_code, ps.project_status_name, " +
                    "pt.project_type_name, org.name as organization_name " +
                    "FROM pa_projects_all p " +
                    "JOIN pa_project_statuses_v ps ON p.project_status_code = ps.project_status_code " +
                    "JOIN pa_project_types pt ON p.project_type_code = pt.project_type_code " +
                    "JOIN hr_all_organization_units org ON p.carrying_out_organization_id = org.organization_id " +
                    "WHERE p.project_status_code = ? " +
                    "ORDER BY p.segment1";

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
     * Get projects by organization.
     *
     * @param server The database server address
     * @param sid The Oracle SID
     * @param username Database username
     * @param password Database password
     * @param organizationId The organization ID
     * @return List of projects belonging to the specified organization
     * @throws SQLException If a database error occurs
     */
    public List<Map<String, Object>> getProjectsByOrganization(String server, String sid,
                                                               String username, String password,
                                                               String organizationId) throws SQLException {

        String url = String.format("jdbc:oracle:thin:@%s:1521:%s", server, sid);

        List<Map<String, Object>> projects = new ArrayList<>();

        try (Connection conn = DriverManager.getConnection(url, username, password)) {
            String sql = "SELECT p.project_id, p.segment1 as project_number, p.name as project_name, " +
                    "p.description, p.start_date, p.completion_date, " +
                    "p.project_status_code, ps.project_status_name, " +
                    "pt.project_type_name " +
                    "FROM pa_projects_all p " +
                    "JOIN pa_project_statuses_v ps ON p.project_status_code = ps.project_status_code " +
                    "JOIN pa_project_types pt ON p.project_type_code = pt.project_type_code " +
                    "WHERE p.carrying_out_organization_id = ? " +
                    "ORDER BY p.segment1";

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, organizationId);

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
     * Get projects by date range (projects active during this period).
     *
     * @param server The database server address
     * @param sid The Oracle SID
     * @param username Database username
     * @param password Database password
     * @param startDate The start date (in format 'YYYY-MM-DD')
     * @param endDate The end date (in format 'YYYY-MM-DD')
     * @return List of projects active during the date range
     * @throws SQLException If a database error occurs
     */
    public List<Map<String, Object>> getProjectsByDateRange(String server, String sid,
                                                            String username, String password,
                                                            String startDate, String endDate) throws SQLException {

        String url = String.format("jdbc:oracle:thin:@%s:1521:%s", server, sid);

        List<Map<String, Object>> projects = new ArrayList<>();

        try (Connection conn = DriverManager.getConnection(url, username, password)) {
            String sql = "SELECT p.project_id, p.segment1 as project_number, p.name as project_name, " +
                    "p.description, p.start_date, p.completion_date, " +
                    "p.project_status_code, ps.project_status_name, " +
                    "pt.project_type_name, org.name as organization_name " +
                    "FROM pa_projects_all p " +
                    "JOIN pa_project_statuses_v ps ON p.project_status_code = ps.project_status_code " +
                    "JOIN pa_project_types pt ON p.project_type_code = pt.project_type_code " +
                    "JOIN hr_all_organization_units org ON p.carrying_out_organization_id = org.organization_id " +
                    "WHERE (p.start_date <= TO_DATE(?, 'YYYY-MM-DD') AND " +
                    "       p.completion_date >= TO_DATE(?, 'YYYY-MM-DD')) OR " +
                    "      (p.start_date BETWEEN TO_DATE(?, 'YYYY-MM-DD') AND TO_DATE(?, 'YYYY-MM-DD')) OR " +
                    "      (p.completion_date BETWEEN TO_DATE(?, 'YYYY-MM-DD') AND TO_DATE(?, 'YYYY-MM-DD')) " +
                    "ORDER BY p.segment1";

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
     * Get project tasks for a specific project.
     *
     * @param server The database server address
     * @param sid The Oracle SID
     * @param username Database username
     * @param password Database password
     * @param projectId The project ID
     * @return List of tasks for the specified project
     * @throws SQLException If a database error occurs
     */
    public List<Map<String, Object>> getProjectTasks(String server, String sid,
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
                    "t.wbs_level, t.parent_task_id, pt.task_name as parent_task_name, " +
                    "t.creation_date, t.last_update_date " +
                    "FROM pa_tasks t " +
                    "JOIN pa_task_statuses_v ts ON t.task_status_code = ts.task_status_code " +
                    "LEFT JOIN pa_tasks pt ON t.parent_task_id = pt.task_id " +
                    "WHERE t.project_id = ? " +
                    "ORDER BY t.task_number";

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
     * Get project financial summary including budgets, costs, etc.
     *
     * @param server The database server address
     * @param sid The Oracle SID
     * @param username Database username
     * @param password Database password
     * @param projectId The project ID
     * @return Map containing financial summary information
     * @throws SQLException If a database error occurs
     */
    public Map<String, Object> getProjectFinancialSummary(String server, String sid,
                                                          String username, String password,
                                                          String projectId) throws SQLException {

        String url = String.format("jdbc:oracle:thin:@%s:1521:%s", server, sid);

        Map<String, Object> summary = new HashMap<>();

        try (Connection conn = DriverManager.getConnection(url, username, password)) {
            // Get project basic info
            String projectSql = "SELECT p.name as project_name, p.segment1 as project_number, " +
                    "p.project_status_code, ps.project_status_name, " +
                    "ppc.project_currency_code " +
                    "FROM pa_projects_all p " +
                    "JOIN pa_project_statuses_v ps ON p.project_status_code = ps.project_status_code " +
                    "JOIN pa_project_currencies ppc ON p.project_id = ppc.project_id " +
                    "WHERE p.project_id = ?";

            try (PreparedStatement stmt = conn.prepareStatement(projectSql)) {
                stmt.setString(1, projectId);

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        summary.put("project_name", rs.getString("project_name"));
                        summary.put("project_number", rs.getString("project_number"));
                        summary.put("project_status", rs.getString("project_status_name"));
                        summary.put("currency_code", rs.getString("project_currency_code"));
                    }
                }
            }

            // Get budget data
            String budgetSql = "SELECT SUM(budget_amount) as total_budget " +
                    "FROM pa_project_budgets " +
                    "WHERE project_id = ?";

            try (PreparedStatement stmt = conn.prepareStatement(budgetSql)) {
                stmt.setString(1, projectId);

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        summary.put("total_budget", rs.getDouble("total_budget"));
                    }
                }
            }

            // Get actual costs
            String costSql = "SELECT SUM(burdened_cost) as actual_cost " +
                    "FROM pa_expenditure_items_all pei " +
                    "JOIN pa_expenditure_items_v peiv ON pei.expenditure_item_id = peiv.expenditure_item_id " +
                    "WHERE pei.project_id = ?";

            try (PreparedStatement stmt = conn.prepareStatement(costSql)) {
                stmt.setString(1, projectId);

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        summary.put("actual_cost", rs.getDouble("actual_cost"));
                    }
                }
            }

            // Get commitment costs
            String commitmentSql = "SELECT SUM(commitment_amount) as committed_cost " +
                    "FROM pa_commitments " +
                    "WHERE project_id = ?";

            try (PreparedStatement stmt = conn.prepareStatement(commitmentSql)) {
                stmt.setString(1, projectId);

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        summary.put("committed_cost", rs.getDouble("committed_cost"));
                    }
                }
            }

            // Get revenue data
            String revenueSql = "SELECT SUM(revenue_amount) as total_revenue " +
                    "FROM pa_project_revenues " +
                    "WHERE project_id = ?";

            try (PreparedStatement stmt = conn.prepareStatement(revenueSql)) {
                stmt.setString(1, projectId);

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        summary.put("total_revenue", rs.getDouble("total_revenue"));
                    }
                }
            }

            // Calculate some metrics
            Double budget = (Double) summary.getOrDefault("total_budget", 0.0);
            Double actualCost = (Double) summary.getOrDefault("actual_cost", 0.0);
            Double committedCost = (Double) summary.getOrDefault("committed_cost", 0.0);
            Double totalRevenue = (Double) summary.getOrDefault("total_revenue", 0.0);

            if (budget > 0) {
                summary.put("budget_utilization_pct", (actualCost / budget) * 100);
            }

            summary.put("remaining_budget", budget - actualCost - committedCost);

            if (actualCost > 0) {
                summary.put("profit_margin_pct", ((totalRevenue - actualCost) / totalRevenue) * 100);
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
        System.out.println("Found " + projects.size() + " Oracle EBS projects");

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
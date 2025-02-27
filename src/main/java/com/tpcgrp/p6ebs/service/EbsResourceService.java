package com.tpcgrp.p6ebs.service;

import java.sql.*;
import java.util.*;

/**
 * Service class for retrieving resources from Oracle EBS database.
 */
public class EbsResourceService {

    private final DatabaseService databaseService;

    public EbsResourceService(DatabaseService databaseService) {
        this.databaseService = databaseService;
    }

    /**
     * Retrieves all resources from Oracle EBS database.
     *
     * @param server The database server address
     * @param sid The Oracle SID
     * @param username Database username
     * @param password Database password
     * @return List of resource maps containing all resource data
     * @throws SQLException If a database error occurs
     */
    public List<Map<String, Object>> getAllResources(String server, String sid,
                                                     String username, String password) throws SQLException {

        String url = String.format("jdbc:oracle:thin:@%s:1521:%s", server, sid);

        List<Map<String, Object>> resources = new ArrayList<>();

        try (Connection conn = DriverManager.getConnection(url, username, password)) {
            // SQL query to get resources with key information
            String sql = "SELECT ppf.person_id, ppf.employee_number, ppf.full_name, " +
                    "ppf.email_address, ppf.effective_start_date, ppf.effective_end_date, " +
                    "pj.job_id, pj.name as job_title, " +
                    "haou.organization_id, haou.name as organization_name, " +
                    "paam.assignment_status_type_id, past.user_status as assignment_status, " +
                    "papf.job_bill_rate, papf.job_cost_rate " +
                    "FROM per_all_people_f ppf " +
                    "JOIN per_all_assignments_m paam ON ppf.person_id = paam.person_id " +
                    "LEFT JOIN per_jobs pj ON paam.job_id = pj.job_id " +
                    "LEFT JOIN hr_all_organization_units haou ON paam.organization_id = haou.organization_id " +
                    "LEFT JOIN per_assignment_status_types past ON paam.assignment_status_type_id = past.assignment_status_type_id " +
                    "LEFT JOIN pa_person_fee_rates papf ON ppf.person_id = papf.person_id " +
                    "WHERE SYSDATE BETWEEN ppf.effective_start_date AND ppf.effective_end_date " +
                    "AND paam.primary_flag = 'Y' " +
                    "ORDER BY ppf.full_name";

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
     * @param sid The Oracle SID
     * @param username Database username
     * @param password Database password
     * @param personId The person ID to look up
     * @return Map containing resource details or null if not found
     * @throws SQLException If a database error occurs
     */
    public Map<String, Object> getResourceById(String server, String sid,
                                               String username, String password,
                                               String personId) throws SQLException {

        String url = String.format("jdbc:oracle:thin:@%s:1521:%s", server, sid);

        try (Connection conn = DriverManager.getConnection(url, username, password)) {
            String sql = "SELECT ppf.person_id, ppf.employee_number, ppf.full_name, " +
                    "ppf.first_name, ppf.last_name, ppf.middle_names, " +
                    "ppf.email_address, ppf.phone_number, ppf.effective_start_date, " +
                    "ppf.effective_end_date, ppf.original_date_of_hire, " +
                    "paam.assignment_id, pj.job_id, pj.name as job_title, " +
                    "haou.organization_id, haou.name as organization_name, " +
                    "paam.assignment_status_type_id, past.user_status as assignment_status, " +
                    "papf.job_bill_rate, papf.job_cost_rate, pcak.category as job_category, " +
                    "paam.supervisor_id, sup.full_name as supervisor_name " +
                    "FROM per_all_people_f ppf " +
                    "JOIN per_all_assignments_m paam ON ppf.person_id = paam.person_id " +
                    "LEFT JOIN per_jobs pj ON paam.job_id = pj.job_id " +
                    "LEFT JOIN hr_all_organization_units haou ON paam.organization_id = haou.organization_id " +
                    "LEFT JOIN per_assignment_status_types past ON paam.assignment_status_type_id = past.assignment_status_type_id " +
                    "LEFT JOIN pa_person_fee_rates papf ON ppf.person_id = papf.person_id " +
                    "LEFT JOIN per_categories_vl pcak ON pj.category_id = pcak.category_id " +
                    "LEFT JOIN per_all_people_f sup ON paam.supervisor_id = sup.person_id " +
                    "WHERE ppf.person_id = ? " +
                    "AND SYSDATE BETWEEN ppf.effective_start_date AND ppf.effective_end_date " +
                    "AND paam.primary_flag = 'Y'";

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, personId);

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
     * Get resources by organization.
     *
     * @param server The database server address
     * @param sid The Oracle SID
     * @param username Database username
     * @param password Database password
     * @param organizationId The organization ID
     * @return List of resources in the specified organization
     * @throws SQLException If a database error occurs
     */
    public List<Map<String, Object>> getResourcesByOrganization(String server, String sid,
                                                                String username, String password,
                                                                String organizationId) throws SQLException {

        String url = String.format("jdbc:oracle:thin:@%s:1521:%s", server, sid);

        List<Map<String, Object>> resources = new ArrayList<>();

        try (Connection conn = DriverManager.getConnection(url, username, password)) {
            String sql = "SELECT ppf.person_id, ppf.employee_number, ppf.full_name, " +
                    "ppf.email_address, pj.name as job_title, " +
                    "haou.name as organization_name, past.user_status as assignment_status " +
                    "FROM per_all_people_f ppf " +
                    "JOIN per_all_assignments_m paam ON ppf.person_id = paam.person_id " +
                    "LEFT JOIN per_jobs pj ON paam.job_id = pj.job_id " +
                    "LEFT JOIN hr_all_organization_units haou ON paam.organization_id = haou.organization_id " +
                    "LEFT JOIN per_assignment_status_types past ON paam.assignment_status_type_id = past.assignment_status_type_id " +
                    "WHERE paam.organization_id = ? " +
                    "AND SYSDATE BETWEEN ppf.effective_start_date AND ppf.effective_end_date " +
                    "AND paam.primary_flag = 'Y' " +
                    "ORDER BY ppf.full_name";

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, organizationId);

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
     * Get resources by job title/category.
     *
     * @param server The database server address
     * @param sid The Oracle SID
     * @param username Database username
     * @param password Database password
     * @param jobId The job ID
     * @return List of resources with the specified job
     * @throws SQLException If a database error occurs
     */
    public List<Map<String, Object>> getResourcesByJob(String server, String sid,
                                                       String username, String password,
                                                       String jobId) throws SQLException {

        String url = String.format("jdbc:oracle:thin:@%s:1521:%s", server, sid);

        List<Map<String, Object>> resources = new ArrayList<>();

        try (Connection conn = DriverManager.getConnection(url, username, password)) {
            String sql = "SELECT ppf.person_id, ppf.employee_number, ppf.full_name, " +
                    "ppf.email_address, pj.name as job_title, " +
                    "haou.name as organization_name, past.user_status as assignment_status " +
                    "FROM per_all_people_f ppf " +
                    "JOIN per_all_assignments_m paam ON ppf.person_id = paam.person_id " +
                    "JOIN per_jobs pj ON paam.job_id = pj.job_id " +
                    "LEFT JOIN hr_all_organization_units haou ON paam.organization_id = haou.organization_id " +
                    "LEFT JOIN per_assignment_status_types past ON paam.assignment_status_type_id = past.assignment_status_type_id " +
                    "WHERE paam.job_id = ? " +
                    "AND SYSDATE BETWEEN ppf.effective_start_date AND ppf.effective_end_date " +
                    "AND paam.primary_flag = 'Y' " +
                    "ORDER BY ppf.full_name";

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, jobId);

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
     * Get resources assigned to a specific project.
     *
     * @param server The database server address
     * @param sid The Oracle SID
     * @param username Database username
     * @param password Database password
     * @param projectId The project ID
     * @return List of resources assigned to the project
     * @throws SQLException If a database error occurs
     */
    public List<Map<String, Object>> getResourcesByProject(String server, String sid,
                                                           String username, String password,
                                                           String projectId) throws SQLException {

        String url = String.format("jdbc:oracle:thin:@%s:1521:%s", server, sid);

        List<Map<String, Object>> resources = new ArrayList<>();

        try (Connection conn = DriverManager.getConnection(url, username, password)) {
            String sql = "SELECT DISTINCT ppf.person_id, ppf.employee_number, ppf.full_name, " +
                    "ppf.email_address, pj.name as job_title, " +
                    "haou.name as organization_name, past.user_status as assignment_status, " +
                    "COUNT(DISTINCT pta.task_id) as assigned_tasks " +
                    "FROM per_all_people_f ppf " +
                    "JOIN pa_task_assignments pta ON ppf.person_id = pta.person_id " +
                    "JOIN pa_tasks pt ON pta.task_id = pt.task_id " +
                    "JOIN per_all_assignments_m paam ON ppf.person_id = paam.person_id " +
                    "LEFT JOIN per_jobs pj ON paam.job_id = pj.job_id " +
                    "LEFT JOIN hr_all_organization_units haou ON paam.organization_id = haou.organization_id " +
                    "LEFT JOIN per_assignment_status_types past ON paam.assignment_status_type_id = past.assignment_status_type_id " +
                    "WHERE pt.project_id = ? " +
                    "AND SYSDATE BETWEEN ppf.effective_start_date AND ppf.effective_end_date " +
                    "AND paam.primary_flag = 'Y' " +
                    "GROUP BY ppf.person_id, ppf.employee_number, ppf.full_name, " +
                    "ppf.email_address, pj.name, haou.name, past.user_status " +
                    "ORDER BY ppf.full_name";

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
     * Get resource assignments for a specific project.
     *
     * @param server The database server address
     * @param sid The Oracle SID
     * @param username Database username
     * @param password Database password
     * @param projectId The project ID
     * @param personId The person ID
     * @return List of task assignments for the resource in the project
     * @throws SQLException If a database error occurs
     */
    public List<Map<String, Object>> getResourceProjectAssignments(String server, String sid,
                                                                   String username, String password,
                                                                   String projectId, String personId) throws SQLException {

        String url = String.format("jdbc:oracle:thin:@%s:1521:%s", server, sid);

        List<Map<String, Object>> assignments = new ArrayList<>();

        try (Connection conn = DriverManager.getConnection(url, username, password)) {
            String sql = "SELECT pta.task_assignment_id, pta.task_id, pt.task_number, pt.task_name, " +
                    "pta.assignment_start_date, pta.assignment_end_date, " +
                    "pta.assigned_units, pta.planned_effort, pta.actual_effort, " +
                    "pta.billable_flag, pta.assignment_status_code, tas.assignment_status_name " +
                    "FROM pa_task_assignments pta " +
                    "JOIN pa_tasks pt ON pta.task_id = pt.task_id " +
                    "JOIN pa_projects_all ppa ON pt.project_id = ppa.project_id " +
                    "JOIN pa_assignment_statuses_v tas ON pta.assignment_status_code = tas.assignment_status_code " +
                    "WHERE pta.person_id = ? " +
                    "AND pt.project_id = ? " +
                    "ORDER BY pt.task_number";

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, personId);
                stmt.setString(2, projectId);

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
     * Get resource billing and cost rates.
     *
     * @param server The database server address
     * @param sid The Oracle SID
     * @param username Database username
     * @param password Database password
     * @param personId The person ID
     * @return Map containing resource rate information
     * @throws SQLException If a database error occurs
     */
    public Map<String, Object> getResourceRates(String server, String sid,
                                                String username, String password,
                                                String personId) throws SQLException {

        String url = String.format("jdbc:oracle:thin:@%s:1521:%s", server, sid);

        try (Connection conn = DriverManager.getConnection(url, username, password)) {
            String sql = "SELECT ppf.person_id, ppf.full_name, " +
                    "papf.job_bill_rate, papf.job_cost_rate, " +
                    "papf.standard_bill_rate, papf.standard_cost_rate, " +
                    "papf.overtime_bill_multiplier, papf.overtime_cost_multiplier, " +
                    "papf.currency_code, papf.effective_start_date, papf.effective_end_date " +
                    "FROM per_all_people_f ppf " +
                    "JOIN pa_person_fee_rates papf ON ppf.person_id = papf.person_id " +
                    "WHERE ppf.person_id = ? " +
                    "AND SYSDATE BETWEEN ppf.effective_start_date AND ppf.effective_end_date " +
                    "AND SYSDATE BETWEEN papf.effective_start_date AND papf.effective_end_date";

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, personId);

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        Map<String, Object> rates = new HashMap<>();
                        ResultSetMetaData metaData = rs.getMetaData();
                        int columnCount = metaData.getColumnCount();

                        for (int i = 1; i <= columnCount; i++) {
                            String columnName = metaData.getColumnName(i);
                            Object value = rs.getObject(i);
                            rates.put(columnName, value);
                        }

                        return rates;
                    }
                }
            }
        }

        return null; // Resource rates not found
    }

    /**
     * Get resource utilization for a specified period.
     *
     * @param server The database server address
     * @param sid The Oracle SID
     * @param username Database username
     * @param password Database password
     * @param personId The person ID
     * @param startDate The start date (in format 'YYYY-MM-DD')
     * @param endDate The end date (in format 'YYYY-MM-DD')
     * @return Map containing resource utilization information
     * @throws SQLException If a database error occurs
     */
    public Map<String, Object> getResourceUtilization(String server, String sid,
                                                      String username, String password,
                                                      String personId, String startDate, String endDate) throws SQLException {

        String url = String.format("jdbc:oracle:thin:@%s:1521:%s", server, sid);

        Map<String, Object> utilization = new HashMap<>();

        try (Connection conn = DriverManager.getConnection(url, username, password)) {
            // Get resource basic info
            String resourceSql = "SELECT ppf.person_id, ppf.full_name, ppf.employee_number " +
                    "FROM per_all_people_f ppf " +
                    "WHERE ppf.person_id = ? " +
                    "AND SYSDATE BETWEEN ppf.effective_start_date AND ppf.effective_end_date";

            try (PreparedStatement stmt = conn.prepareStatement(resourceSql)) {
                stmt.setString(1, personId);

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        utilization.put("person_id", rs.getString("person_id"));
                        utilization.put("full_name", rs.getString("full_name"));
                        utilization.put("employee_number", rs.getString("employee_number"));
                    }
                }
            }

            // Get actual hours worked
            String actualHoursSql = "SELECT SUM(pei.quantity) as actual_hours " +
                    "FROM pa_expenditure_items_all pei " +
                    "JOIN pa_expenditures_all pea ON pei.expenditure_id = pea.expenditure_id " +
                    "WHERE pei.person_id = ? " +
                    "AND pea.expenditure_ending_date BETWEEN TO_DATE(?, 'YYYY-MM-DD') AND TO_DATE(?, 'YYYY-MM-DD')";

            try (PreparedStatement stmt = conn.prepareStatement(actualHoursSql)) {
                stmt.setString(1, personId);
                stmt.setString(2, startDate);
                stmt.setString(3, endDate);

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        utilization.put("actual_hours", rs.getDouble("actual_hours"));
                    }
                }
            }

            // Get planned hours
            String plannedHoursSql = "SELECT SUM(pta.planned_effort) as planned_hours " +
                    "FROM pa_task_assignments pta " +
                    "WHERE pta.person_id = ? " +
                    "AND (pta.assignment_start_date <= TO_DATE(?, 'YYYY-MM-DD') AND " +
                    "    (pta.assignment_end_date >= TO_DATE(?, 'YYYY-MM-DD') OR pta.assignment_end_date IS NULL))";

            try (PreparedStatement stmt = conn.prepareStatement(plannedHoursSql)) {
                stmt.setString(1, personId);
                stmt.setString(2, endDate);
                stmt.setString(3, startDate);

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        utilization.put("planned_hours", rs.getDouble("planned_hours"));
                    }
                }
            }

            // Get total available hours (this is a simplified calculation)
            String availableHoursSql = "SELECT 8 * (SELECT COUNT(*) " +
                    "FROM (SELECT TO_DATE(?, 'YYYY-MM-DD') + LEVEL - 1 AS day_date " +
                    "      FROM dual " +
                    "      CONNECT BY LEVEL <= (TO_DATE(?, 'YYYY-MM-DD') - TO_DATE(?, 'YYYY-MM-DD') + 1)) days " +
                    "WHERE TO_CHAR(day_date, 'DY') NOT IN ('SAT', 'SUN')) as available_hours " +
                    "FROM dual";

            try (PreparedStatement stmt = conn.prepareStatement(availableHoursSql)) {
                stmt.setString(1, startDate);
                stmt.setString(2, endDate);
                stmt.setString(3, startDate);

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        utilization.put("available_hours", rs.getDouble("available_hours"));
                    }
                }
            }

            // Calculate utilization percentages
            Double availableHours = (Double) utilization.getOrDefault("available_hours", 0.0);
            Double actualHours = (Double) utilization.getOrDefault("actual_hours", 0.0);
            Double plannedHours = (Double) utilization.getOrDefault("planned_hours", 0.0);

            if (availableHours > 0) {
                utilization.put("actual_utilization_pct", (actualHours / availableHours) * 100);
                utilization.put("planned_utilization_pct", (plannedHours / availableHours) * 100);
            }
        }

        return utilization;
    }

    /**
     * Display resources in a formatted manner (useful for debugging and testing).
     *
     * @param resources List of resource maps to display
     */
    public void displayResources(List<Map<String, Object>> resources) {
        System.out.println("Found " + resources.size() + " Oracle EBS resources");

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
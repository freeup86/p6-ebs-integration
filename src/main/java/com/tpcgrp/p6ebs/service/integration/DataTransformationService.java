/**
 * Service for complex data transformations between systems
 */
package com.tpcgrp.p6ebs.service.integration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Function;

@Service
@Slf4j
public class DataTransformationService {

    private final MappingUtility mappingUtility;
    private final IntegrationLogService logService;

    // Store transformation functions for different entity types
    private final Map<String, Map<String, Function<Object, Object>>> transformFunctions = new HashMap<>();

    public DataTransformationService(MappingUtility mappingUtility, IntegrationLogService logService) {
        this.mappingUtility = mappingUtility;
        this.logService = logService;

        // Initialize transformation functions
        initializeTransformFunctions();
    }

    /**
     * Initialize transformation functions for fields
     */
    private void initializeTransformFunctions() {
        // Project transformations
        Map<String, Function<Object, Object>> projectTransforms = new HashMap<>();

        // Date format conversion
        projectTransforms.put("start_date", value -> formatDate(value, "yyyy-MM-dd"));
        projectTransforms.put("completion_date", value -> formatDate(value, "yyyy-MM-dd"));

        // Status code mapping
        projectTransforms.put("status_code", value -> mapStatusCode(value));

        transformFunctions.put("project", projectTransforms);

        // Activity transformations
        Map<String, Function<Object, Object>> activityTransforms = new HashMap<>();
        activityTransforms.put("start_date", value -> formatDate(value, "yyyy-MM-dd"));
        activityTransforms.put("finish_date", value -> formatDate(value, "yyyy-MM-dd"));

        transformFunctions.put("activity", activityTransforms);
    }

    /**
     * Transform financial data between systems
     */
    public Map<String, Object> transformFinancialData(Map<String, Object> p6Data,
                                                      Map<String, Object> ebsData,
                                                      String direction) {
        logService.logInfo("Transforming financial data with direction: " + direction);

        Map<String, Object> result = new HashMap<>();

        if ("P6_TO_EBS".equals(direction)) {
            // Transform P6 data for EBS
            if (p6Data != null) {
                // Extract and transform budget data
                if (p6Data.containsKey("planned_cost")) {
                    BigDecimal plannedCost = toBigDecimal(p6Data.get("planned_cost"));
                    result.put("budget_amount", plannedCost);
                }

                // Extract and transform actual cost data
                if (p6Data.containsKey("actual_cost")) {
                    BigDecimal actualCost = toBigDecimal(p6Data.get("actual_cost"));
                    result.put("actual_cost", actualCost);
                }

                // Extract and transform remaining cost data
                if (p6Data.containsKey("remaining_cost")) {
                    BigDecimal remainingCost = toBigDecimal(p6Data.get("remaining_cost"));
                    result.put("committed_amount", remainingCost);
                }
            }
        } else if ("EBS_TO_P6".equals(direction)) {
            // Transform EBS data for P6
            if (ebsData != null) {
                // Extract and transform budget data
                if (ebsData.containsKey("budgeted_amount")) {
                    BigDecimal budgetAmount = toBigDecimal(ebsData.get("budgeted_amount"));
                    result.put("target_cost", budgetAmount);
                }

                // Extract and transform actual cost data
                if (ebsData.containsKey("actual_cost")) {
                    BigDecimal actualCost = toBigDecimal(ebsData.get("actual_cost"));
                    result.put("act_cost", actualCost);
                }

                // Extract and transform remaining cost data
                if (ebsData.containsKey("committed_amount")) {
                    BigDecimal committedAmount = toBigDecimal(ebsData.get("committed_amount"));
                    result.put("remain_cost", committedAmount);
                }
            }
        } else if ("BIDIRECTIONAL".equals(direction)) {
            // Merge data from both systems based on priority rules
            result = mergeFinancialData(p6Data, ebsData);
        } else {
            logService.logWarning("Unknown transformation direction: " + direction);
        }

        return result;
    }

    /**
     * Merge financial data from both systems
     */
    private Map<String, Object> mergeFinancialData(Map<String, Object> p6Data, Map<String, Object> ebsData) {
        Map<String, Object> result = new HashMap<>();

        // Implement complex merging logic here
        // Example: Take actuals from EBS, budget from P6, etc.

        // For now, a simple implementation that prioritizes EBS data
        if (ebsData != null) {
            result.putAll(ebsData);
        }

        if (p6Data != null) {
            // Only add P6 data if not already present from EBS
            for (Map.Entry<String, Object> entry : p6Data.entrySet()) {
                if (!result.containsKey(entry.getKey())) {
                    result.put(entry.getKey(), entry.getValue());
                }
            }
        }

        return result;
    }

    /**
     * Transform a map of values using registered transformation functions
     */
    public Map<String, Object> transformMap(String entityType, Map<String, Object> sourceMap,
                                            String sourceSystem, String targetSystem) {
        if (sourceMap == null) {
            return null;
        }

        Map<String, Object> result = new HashMap<>();
        Map<String, Function<Object, Object>> transforms = transformFunctions.get(entityType);

        // First map field names based on source and target systems
        Map<String, Object> mappedData;
        if ("P6".equals(sourceSystem) && "EBS".equals(targetSystem)) {
            mappedData = mappingUtility.mapP6ToEbs(entityType, sourceMap);
        } else if ("EBS".equals(sourceSystem) && "P6".equals(targetSystem)) {
            mappedData = mappingUtility.mapEbsToP6(entityType, sourceMap);
        } else {
            logService.logWarning("Unknown system combination: " + sourceSystem + " to " + targetSystem);
            return sourceMap;
        }

        // Then apply transformations to individual fields
        if (transforms != null) {
            for (Map.Entry<String, Object> entry : mappedData.entrySet()) {
                String fieldName = entry.getKey();
                Object value = entry.getValue();

                // Apply transformation if available
                if (transforms.containsKey(fieldName)) {
                    value = transforms.get(fieldName).apply(value);
                }

                result.put(fieldName, value);
            }
        } else {
            // No transformations defined, use mapped data as is
            result = mappedData;
        }

        return result;
    }

    /**
     * Format date according to specified pattern
     */
    private String formatDate(Object dateValue, String pattern) {
        if (dateValue == null) {
            return null;
        }

        try {
            Date date;
            if (dateValue instanceof Date) {
                date = (Date) dateValue;
            } else if (dateValue instanceof java.sql.Date) {
                date = new Date(((java.sql.Date) dateValue).getTime());
            } else if (dateValue instanceof String) {
                // Try to parse the string as a date
                date = new SimpleDateFormat("yyyy-MM-dd").parse((String) dateValue);
            } else {
                return dateValue.toString();
            }

            return new SimpleDateFormat(pattern).format(date);
        } catch (Exception e) {
            logService.logWarning("Failed to format date: " + dateValue, e);
            return dateValue.toString();
        }
    }

    /**
     * Map status codes between systems
     */
    private String mapStatusCode(Object statusCode) {
        if (statusCode == null) {
            return null;
        }

        // Define status code mappings
        Map<String, String> statusMappings = new HashMap<>();
        statusMappings.put("1", "APPROVED");
        statusMappings.put("2", "IN_PROGRESS");
        statusMappings.put("3", "COMPLETED");
        statusMappings.put("4", "CANCELLED");

        // Handle different input types
        String statusStr = statusCode.toString();

        return statusMappings.getOrDefault(statusStr, statusStr);
    }

    /**
     * Convert object to BigDecimal safely
     */
    private BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }

        if (value instanceof Number) {
            return new BigDecimal(value.toString());
        } else if (value instanceof String) {
            try {
                return new BigDecimal((String) value);
            } catch (NumberFormatException e) {
                return BigDecimal.ZERO;
            }
        }

        return BigDecimal.ZERO;
    }

    /**
     * Specialized transformation for resource data
     */
    public Map<String, Object> transformResourceData(Map<String, Object> sourceData,
                                                     String sourceSystem,
                                                     String targetSystem) {
        // Implementation for resource data transformation
        return transformMap("resource", sourceData, sourceSystem, targetSystem);
    }

    /**
     * Specialized transformation for timesheet data
     */
    public Map<String, Object> transformTimesheetData(Map<String, Object> sourceData,
                                                      String sourceSystem,
                                                      String targetSystem) {
        // Implementation for timesheet data transformation

        // Special logic for timesheet data
        Map<String, Object> transformedData = new HashMap<>(sourceData);

        // Transform dates
        if (sourceData.containsKey("work_date")) {
            transformedData.put("work_date",
                    formatDate(sourceData.get("work_date"), "yyyy-MM-dd"));
        }

        // Transform hours
        if (sourceData.containsKey("hours") && sourceData.get("hours") instanceof Number) {
            double hours = ((Number) sourceData.get("hours")).doubleValue();
            // Round to 2 decimal places
            BigDecimal rounded = new BigDecimal(hours).setScale(2, BigDecimal.ROUND_HALF_UP);
            transformedData.put("hours", rounded);
        }

        return transformedData;
    }
}
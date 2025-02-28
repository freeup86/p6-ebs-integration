package com.tpcgrp.p6ebs.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.opencsv.CSVWriter;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.*;

@Service
@Slf4j
public class FileImportExportService {

    private final ObjectMapper objectMapper;

    public FileImportExportService() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    /**
     * Export data to JSON file
     * @param data Data to export
     * @param filePath Destination file path
     */
    public void exportToJson(Object data, String filePath) throws IOException {
        try {
            objectMapper.writeValue(new File(filePath), data);
            log.info("Data exported to JSON: {}", filePath);
        } catch (IOException e) {
            log.error("Error exporting to JSON", e);
            throw e;
        }
    }

    /**
     * Import data from JSON file
     * @param filePath Source file path
     * @param valueType Type of data to import
     * @return Imported data
     */
    public <T> T importFromJson(String filePath, Class<T> valueType) throws IOException {
        try {
            return objectMapper.readValue(new File(filePath), valueType);
        } catch (IOException e) {
            log.error("Error importing from JSON", e);
            throw e;
        }
    }

    /**
     * Export data to CSV file
     * @param data List of data to export
     * @param filePath Destination file path
     */
    public void exportToCsv(List<Map<String, Object>> data, String filePath) throws IOException {
        try (CSVWriter writer = new CSVWriter(new FileWriter(filePath))) {
            if (data.isEmpty()) {
                return;
            }

            // Write headers
            Set<String> headers = new LinkedHashSet<>();
            data.forEach(row -> headers.addAll(row.keySet()));
            writer.writeNext(headers.toArray(new String[0]));

            // Write data rows
            for (Map<String, Object> row : data) {
                String[] rowData = headers.stream()
                        .map(header -> String.valueOf(row.getOrDefault(header, "")))
                        .toArray(String[]::new);
                writer.writeNext(rowData);
            }

            log.info("Data exported to CSV: {}", filePath);
        } catch (IOException e) {
            log.error("Error exporting to CSV", e);
            throw e;
        }
    }

    /**
     * Export data to Excel file
     * @param data List of data to export
     * @param filePath Destination file path
     */
    public void exportToExcel(List<Map<String, Object>> data, String filePath) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Data");

            if (data.isEmpty()) {
                return;
            }

            // Create headers
            Set<String> headers = new LinkedHashSet<>();
            data.forEach(row -> headers.addAll(row.keySet()));

            // Write headers
            Row headerRow = sheet.createRow(0);
            int headerCellIndex = 0;
            for (String header : headers) {
                Cell cell = headerRow.createCell(headerCellIndex++);
                cell.setCellValue(header);
            }

            // Write data rows
            int rowNum = 1;
            for (Map<String, Object> row : data) {
                Row dataRow = sheet.createRow(rowNum++);
                int cellIndex = 0;
                for (String header : headers) {
                    Cell cell = dataRow.createCell(cellIndex++);
                    Object value = row.getOrDefault(header, "");

                    if (value instanceof Number) {
                        cell.setCellValue(((Number) value).doubleValue());
                    } else {
                        cell.setCellValue(String.valueOf(value));
                    }
                }
            }

            // Auto-size columns
            for (int i = 0; i < headers.size(); i++) {
                sheet.autoSizeColumn(i);
            }

            // Write to file
            try (FileOutputStream outputStream = new FileOutputStream(filePath)) {
                workbook.write(outputStream);
            }

            log.info("Data exported to Excel: {}", filePath);
        } catch (IOException e) {
            log.error("Error exporting to Excel", e);
            throw e;
        }
    }

    /**
     * Import data from CSV file
     * @param filePath Source file path
     * @return List of imported data
     */
    public List<Map<String, String>> importFromCsv(String filePath) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            List<Map<String, String>> data = new ArrayList<>();
            String[] headers = reader.readLine().split(",");

            String line;
            while ((line = reader.readLine()) != null) {
                String[] values = line.split(",");
                Map<String, String> row = new HashMap<>();

                for (int i = 0; i < headers.length; i++) {
                    row.put(headers[i], i < values.length ? values[i] : "");
                }

                data.add(row);
            }

            log.info("Data imported from CSV: {}", filePath);
            return data;
        } catch (IOException e) {
            log.error("Error importing from CSV", e);
            throw e;
        }
    }
}
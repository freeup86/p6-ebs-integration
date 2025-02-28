/**
 * Service for logging integration activities
 */
package com.tpcgrp.p6ebs.service.integration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

@Service
@Slf4j
public class IntegrationLogService {

    private final String LOG_DIRECTORY = System.getProperty("user.home") + "/.p6ebs/logs";
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

    private final Queue<LogEntry> logQueue = new ConcurrentLinkedQueue<>();
    private final List<LogEntry> inMemoryLogs = Collections.synchronizedList(new ArrayList<>());
    private final int MAX_IN_MEMORY_LOGS = 1000;

    private final Timer logFlushTimer;
    private volatile boolean shuttingDown = false;

    // Constructor - initialize the log directory and set up flush timer
    public IntegrationLogService() {
        try {
            Files.createDirectories(Paths.get(LOG_DIRECTORY));
        } catch (IOException e) {
            log.error("Failed to create log directory", e);
        }

        // Set up timer to flush logs periodically
        logFlushTimer = new Timer("LogFlushTimer", true);
        logFlushTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                flushLogs();
            }
        }, 5000, 5000); // Flush every 5 seconds

        // Add shutdown hook to flush logs on application exit
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
    }

    /**
     * Log an info message
     */
    public void logInfo(String message) {
        addLogEntry(LogLevel.INFO, message, null);
    }

    /**
     * Log a warning message
     */
    public void logWarning(String message) {
        addLogEntry(LogLevel.WARNING, message, null);
    }

    /**
     * Log a warning message with exception
     */
    public void logWarning(String message, Throwable exception) {
        addLogEntry(LogLevel.WARNING, message, exception);
    }

    /**
     * Log an error message
     */
    public void logError(String message) {
        Throwable exception = null;
        addLogEntry(LogLevel.ERROR, message, null);
    }

    /**
     * Log a debug message
     */
    public void logDebug(String message) {
        addLogEntry(LogLevel.DEBUG, message, null);
    }

    /**
     * Add a log entry to the queue and in-memory store
     */
    private void addLogEntry(LogLevel level, String message, Throwable exception) {
        LogEntry entry = new LogEntry();
        entry.setTimestamp(new Date());
        entry.setLevel(level);
        entry.setMessage(message);

        if (exception != null) {
            entry.setException(exception.toString());

            // Get stack trace
            StringBuilder stackTrace = new StringBuilder();
            for (StackTraceElement elem : exception.getStackTrace()) {
                stackTrace.append("\n  at ").append(elem.toString());
            }
            entry.setStackTrace(stackTrace.toString());
        }

        // Add to queue for file logging
        logQueue.add(entry);

        // Add to in-memory log with size limit
        synchronized (inMemoryLogs) {
            inMemoryLogs.add(entry);
            if (inMemoryLogs.size() > MAX_IN_MEMORY_LOGS) {
                inMemoryLogs.remove(0);
            }
        }

        // Log to SLF4J as well
        switch (level) {
            case INFO:
                log.info(message);
                break;
            case WARNING:
                log.warn(message);
                break;
            case ERROR:
                log.error(message, exception);
                break;
            case DEBUG:
                log.debug(message);
                break;
        }
    }

    /**
     * Flush queued logs to file
     */
    private void flushLogs() {
        if (logQueue.isEmpty() || shuttingDown) {
            return;
        }

        String logFile = LOG_DIRECTORY + "/integration_" +
                new SimpleDateFormat("yyyyMMdd").format(new Date()) + ".log";

        try (FileWriter writer = new FileWriter(logFile, true)) {
            LogEntry entry;
            while ((entry = logQueue.poll()) != null) {
                writer.write(formatLogEntry(entry));
                writer.write(System.lineSeparator());

                // Write stack trace if present
                if (entry.getStackTrace() != null) {
                    writer.write(entry.getStackTrace());
                    writer.write(System.lineSeparator());
                }
            }
        } catch (IOException e) {
            log.error("Failed to write to log file", e);
            // Re-add entries to queue
            // Not ideal but prevents log loss in temporary IO failure
        }
    }

    /**
     * Format a log entry for file output
     */
    private String formatLogEntry(LogEntry entry) {
        return dateFormat.format(entry.getTimestamp()) + " [" + entry.getLevel() + "] " + entry.getMessage();
    }

    /**
     * Get recent logs
     */
    public List<LogEntry> getRecentLogs() {
        synchronized (inMemoryLogs) {
            return new ArrayList<>(inMemoryLogs);
        }
    }

    /**
     * Get recent logs filtered by level
     */
    public List<LogEntry> getRecentLogs(LogLevel minLevel) {
        List<LogEntry> filteredLogs = new ArrayList<>();

        synchronized (inMemoryLogs) {
            for (LogEntry entry : inMemoryLogs) {
                if (entry.getLevel().ordinal() >= minLevel.ordinal()) {
                    filteredLogs.add(entry);
                }
            }
        }

        return filteredLogs;
    }

    /**
     * Get log files
     */
    public List<File> getLogFiles() {
        File dir = new File(LOG_DIRECTORY);
        File[] files = dir.listFiles((d, name) -> name.startsWith("integration_") && name.endsWith(".log"));

        if (files == null) {
            return Collections.emptyList();
        }

        return Arrays.asList(files);
    }

    /**
     * Clean up and ensure all logs are flushed
     */
    public void shutdown() {
        shuttingDown = true;
        flushLogs();
        logFlushTimer.cancel();
    }

    /**
     * Enum for log levels
     */
    public enum LogLevel {
        DEBUG,
        INFO,
        WARNING,
        ERROR
    }

    /**
     * Class representing a log entry
     */
    public static class LogEntry {
        private Date timestamp;
        private LogLevel level;
        private String message;
        private String exception;
        private String stackTrace;

        // Getters and setters
        public Date getTimestamp() { return timestamp; }
        public void setTimestamp(Date timestamp) { this.timestamp = timestamp; }
        public LogLevel getLevel() { return level; }
        public void setLevel(LogLevel level) { this.level = level; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public String getException() { return exception; }
        public void setException(String exception) { this.exception = exception; }
        public String getStackTrace() { return stackTrace; }
        public void setStackTrace(String stackTrace) { this.stackTrace = stackTrace; }
    }
}
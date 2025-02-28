/**
 * Service for sending notifications about integration status
 */
package com.tpcgrp.p6ebs.service.integration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.mail.*;
import javax.mail.internet.*;
import java.util.*;

@Service
@Slf4j
public class NotificationService {

    private final IntegrationLogService logService;
    private final ConfigurationManager configManager;

    private Properties emailProperties;
    private Session emailSession;
    private boolean emailConfigured = false;

    public NotificationService(IntegrationLogService logService, ConfigurationManager configManager) {
        this.logService = logService;
        this.configManager = configManager;

        // Initialize email configuration
        initializeEmailConfig();
    }

    /**
     * Initialize email configuration
     */
    private void initializeEmailConfig() {
        try {
            emailProperties = new Properties();
            emailProperties.put("mail.smtp.host", "smtp.company.com");
            emailProperties.put("mail.smtp.port", "587");
            emailProperties.put("mail.smtp.auth", "true");
            emailProperties.put("mail.smtp.starttls.enable", "true");

            emailSession = Session.getInstance(emailProperties, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication("integration@company.com", "password");
                }
            });

            emailConfigured = true;
            logService.logInfo("Email notification service initialized");
        } catch (Exception e) {
            logService.logError("Failed to initialize email service");
            emailConfigured = false;
        }
    }

    /**
     * Send an integration success notification
     */
    public void sendSuccessNotification(String integrationType, Map<String, Object> results) {
        if (!emailConfigured) {
            logService.logWarning("Email service not configured, skipping success notification");
            return;
        }

        try {
            String subject = "Integration Success: " + integrationType;
            StringBuilder body = new StringBuilder();
            body.append("Integration completed successfully for: ").append(integrationType).append("\n\n");

            // Add summary of results
            body.append("Summary:\n");
            if (results.containsKey("totalProjects")) {
                body.append("Total projects: ").append(results.get("totalProjects")).append("\n");
            }
            if (results.containsKey("updatedProjects")) {
                body.append("Updated projects: ").append(results.get("updatedProjects")).append("\n");
            }

            body.append("\nTimestamp: ").append(new Date()).append("\n");

            // Send the email
            sendEmail(getRecipients(), subject, body.toString());

            logService.logInfo("Sent success notification for " + integrationType);
        } catch (Exception e) {
            logService.logError("Failed to send success notification");
        }
    }

    /**
     * Send an integration failure notification
     */
    public void sendFailureNotification(String integrationType, String errorMessage) {
        if (!emailConfigured) {
            logService.logWarning("Email service not configured, skipping failure notification");
            return;
        }

        try {
            String subject = "Integration Failure: " + integrationType;
            StringBuilder body = new StringBuilder();
            body.append("Integration failed for: ").append(integrationType).append("\n\n");
            body.append("Error: ").append(errorMessage).append("\n\n");
            body.append("Timestamp: ").append(new Date()).append("\n");
            body.append("\nPlease check the integration logs for more details.");

            // Send the email
            sendEmail(getRecipients(), subject, body.toString());

            logService.logInfo("Sent failure notification for " + integrationType);
        } catch (Exception e) {
            logService.logError("Failed to send failure notification");
        }
    }

    /**
     * Send validation issues notification
     */
    public void sendValidationNotification(String integrationType, ValidationService.ValidationReport report) {
        if (!emailConfigured) {
            logService.logWarning("Email service not configured, skipping validation notification");
            return;
        }

        try {
            String subject = "Integration Validation Issues: " + integrationType;
            StringBuilder body = new StringBuilder();
            body.append("Validation completed for: ").append(integrationType).append("\n\n");

            body.append("Summary:\n");
            body.append("Total issues: ").append(report.getTotalIssues()).append("\n");
            body.append("Blocking issues: ").append(report.getBlockingIssues()).append("\n");
            body.append("Warning issues: ").append(report.getWarningIssues()).append("\n\n");

            if (report.getIssues() != null && !report.getIssues().isEmpty()) {
                body.append("Top issues:\n");
                int count = 0;
                for (ValidationService.ValidationIssue issue : report.getIssues()) {
                    if (count++ >= 10) break; // Limit to top 10 issues

                    body.append("- ")
                            .append(issue.isBlocking() ? "[BLOCKING] " : "[WARNING] ")
                            .append(issue.getEntityType())
                            .append(": ")
                            .append(issue.getDescription())
                            .append("\n");
                }
            }

            body.append("\nTimestamp: ").append(report.getTimestamp()).append("\n");

            // Send the email
            sendEmail(getRecipients(), subject, body.toString());

            logService.logInfo("Sent validation notification for " + integrationType);
        } catch (Exception e) {
            logService.logError("Failed to send validation notification");
        }
    }

    /**
     * Send an email
     */
    private void sendEmail(List<String> recipients, String subject, String body) throws MessagingException {
        if (!emailConfigured) {
            throw new MessagingException("Email service not configured");
        }

        Message message = new MimeMessage(emailSession);
        message.setFrom(new InternetAddress("integration@company.com"));

        for (String recipient : recipients) {
            message.addRecipient(Message.RecipientType.TO, new InternetAddress(recipient));
        }

        message.setSubject(subject);
        message.setText(body);

        Transport.send(message);
    }

    /**
     * Get notification recipients
     */
    private List<String> getRecipients() {
        List<String> recipients = new ArrayList<>();
        recipients.add("admin@company.com");

        // Add more recipients based on configuration
        // This would typically be loaded from configuration

        return recipients;
    }

    /**
     * Send a status report notification
     */
    public void sendStatusReport(Map<String, SchedulerService.ScheduleInfo> scheduleInfo) {
        if (!emailConfigured) {
            logService.logWarning("Email service not configured, skipping status report");
            return;
        }

        try {
            String subject = "P6-EBS Integration Status Report";
            StringBuilder body = new StringBuilder();
            body.append("P6-EBS Integration Status Report\n\n");

            body.append("Integration Schedules:\n");
            for (Map.Entry<String, SchedulerService.ScheduleInfo> entry : scheduleInfo.entrySet()) {
                SchedulerService.ScheduleInfo info = entry.getValue();

                body.append("- ").append(info.getIntegrationType()).append(":\n");
                body.append("  Status: ").append(info.isActive() ? "Active" : "Inactive").append("\n");
                body.append("  Interval: Every ").append(info.getIntervalHours()).append(" hours\n");

                if (info.getLastRun() != null) {
                    body.append("  Last run: ").append(info.getLastRun()).append("\n");
                } else {
                    body.append("  Last run: Never\n");
                }

                if (info.getNextRun() != null) {
                    body.append("  Next run: ").append(info.getNextRun()).append("\n");
                }

                body.append("\n");
            }

            body.append("Report generated: ").append(new Date()).append("\n");

            // Send the email
            sendEmail(getRecipients(), subject, body.toString());

            logService.logInfo("Sent integration status report");
        } catch (Exception e) {
            logService.logError("Failed to send status report");
        }
    }
}
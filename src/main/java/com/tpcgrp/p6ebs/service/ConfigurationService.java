package com.tpcgrp.p6ebs.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;

@Service
public class ConfigurationService {
    private static final String CONFIG_FILE = "integration-config.json";
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Data
    public static class Configuration {
        // P6 settings
        private String p6Server;
        private String p6Database;
        private String p6Username;
        private String p6Password;

        // EBS settings
        private String ebsServer;
        private String ebsSid;
        private String ebsUsername;
        private String ebsPassword;

        // Integration settings
        private boolean projectFinancialsEnabled;
        private boolean resourceManagementEnabled;
        private boolean procurementEnabled;
        private boolean timesheetEnabled;
        private boolean projectWbsEnabled;
    }

    public void saveConfiguration(Configuration config) throws IOException {
        File configFile = getConfigFile();
        objectMapper.writeValue(configFile, config);
    }

    public Configuration loadConfiguration() throws IOException {
        File configFile = getConfigFile();
        if (configFile.exists()) {
            return objectMapper.readValue(configFile, Configuration.class);
        }
        return new Configuration();
    }

    private File getConfigFile() {
        String userHome = System.getProperty("user.home");
        return Paths.get(userHome, ".p6ebs", CONFIG_FILE).toFile();
    }
}
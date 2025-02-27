package com.tpcgrp.p6ebs.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import org.springframework.stereotype.Service;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;

@Service
public class ConfigurationService {
    private final String CONFIG_FILE = System.getProperty("user.home") + "/.p6ebs/config.json";
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Data
    public static class Configuration {
        private String p6Server;
        private String p6Database;
        private String p6Username;
        private String p6Password;
        private String ebsServer;
        private String ebsSid;
        private String ebsUsername;
        private String ebsPassword;
        private boolean projectFinancialsEnabled;
        private boolean resourceManagementEnabled;
        private boolean procurementEnabled;
        private boolean timesheetEnabled;
        private boolean projectWbsEnabled;
    }

    public void saveConfiguration(Configuration config) throws IOException {
        File configDir = new File(System.getProperty("user.home") + "/.p6ebs");
        if (!configDir.exists()) {
            configDir.mkdirs();
        }
        objectMapper.writeValue(new File(CONFIG_FILE), config);
    }

    public Configuration loadConfiguration() throws IOException {
        File configFile = new File(CONFIG_FILE);
        if (!configFile.exists()) {
            return new Configuration();
        }
        return objectMapper.readValue(configFile, Configuration.class);
    }
}
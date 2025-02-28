/**
 * Service for managing security aspects of the integration
 */
package com.tpcgrp.p6ebs.service.integration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class SecurityHandler {

    private final IntegrationLogService logService;
    private final byte[] salt;
    private final IvParameterSpec iv;
    private final SecretKey secretKey;

    // Store session tokens
    private final Map<String, SessionInfo> sessionTokens = new HashMap<>();

    public SecurityHandler(IntegrationLogService logService) {
        this.logService = logService;

        // Generate random salt and IV
        SecureRandom random = new SecureRandom();
        salt = new byte[16];
        byte[] ivBytes = new byte[16];
        random.nextBytes(salt);
        random.nextBytes(ivBytes);
        iv = new IvParameterSpec(ivBytes);

        // Derive secret key from a hardcoded password (in real app, use environment variable or secure storage)
        try {
            String password = "p6ebsIntegrationSecretKey";
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, 65536, 256);
            secretKey = new SecretKeySpec(factory.generateSecret(spec).getEncoded(), "AES");
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize security handler", e);
        }
    }

    /**
     * Encrypt sensitive data
     */
    public String encrypt(String data) {
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, iv);
            byte[] encrypted = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            logService.logError("Encryption failed");
            return null;
        }
    }

    /**
     * Decrypt sensitive data
     */
    public String decrypt(String encryptedData) {
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, secretKey, iv);
            byte[] decoded = Base64.getDecoder().decode(encryptedData);
            byte[] decrypted = cipher.doFinal(decoded);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            logService.logError("Decryption failed");
            return null;
        }
    }

    /**
     * Store credentials securely
     */
    public void storeCredentials(String systemId, String username, String password) {
        try {
            String encryptedPassword = encrypt(password);

            // In a real application, this would store to a secure credential store
            // For this example, we'll just log that it was stored
            logService.logInfo("Stored encrypted credentials for " + systemId + " (user: " + username + ")");
        } catch (Exception e) {
            logService.logError("Failed to store credentials for " + systemId);
        }
    }

    /**
     * Create an authentication token for a session
     */
    public String createSessionToken(String username, String sourceIp) {
        // Generate a random token
        SecureRandom random = new SecureRandom();
        byte[] tokenBytes = new byte[32];
        random.nextBytes(tokenBytes);
        String token = Base64.getEncoder().encodeToString(tokenBytes);

        // Store session info
        SessionInfo session = new SessionInfo();
        session.setUsername(username);
        session.setSourceIp(sourceIp);
        session.setCreationTime(System.currentTimeMillis());
        session.setLastAccessTime(System.currentTimeMillis());

        sessionTokens.put(token, session);

        return token;
    }

    /**
     * Validate an authentication token
     */
    public boolean validateSessionToken(String token, String sourceIp) {
        SessionInfo session = sessionTokens.get(token);

        if (session == null) {
            return false;
        }

        // Check if token is expired (30 minutes)
        long now = System.currentTimeMillis();
        if (now - session.getCreationTime() > 30 * 60 * 1000) {
            sessionTokens.remove(token);
            return false;
        }

        // Check if IP matches
        if (!session.getSourceIp().equals(sourceIp)) {
            logService.logWarning("Session token IP mismatch: " + sourceIp + " vs " + session.getSourceIp());
            return false;
        }

        // Update last access time
        session.setLastAccessTime(now);

        return true;
    }

    /**
     * Invalidate a session token
     */
    public void invalidateSessionToken(String token) {
        sessionTokens.remove(token);
    }

    /**
     * Generate secure random password
     */
    public String generateSecurePassword(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*()";
        StringBuilder password = new StringBuilder();
        SecureRandom random = new SecureRandom();

        for (int i = 0; i < length; i++) {
            int index = random.nextInt(chars.length());
            password.append(chars.charAt(index));
        }

        return password.toString();
    }

    /**
     * Check password strength
     */
    public boolean isPasswordStrong(String password) {
        if (password == null || password.length() < 8) {
            return false;
        }

        boolean hasLower = false;
        boolean hasUpper = false;
        boolean hasDigit = false;
        boolean hasSpecial = false;

        for (char c : password.toCharArray()) {
            if (Character.isLowerCase(c)) {
                hasLower = true;
            } else if (Character.isUpperCase(c)) {
                hasUpper = true;
            } else if (Character.isDigit(c)) {
                hasDigit = true;
            } else {
                hasSpecial = true;
            }
        }

        return hasLower && hasUpper && hasDigit && hasSpecial;
    }

    /**
     * Class to store session information
     */
    private static class SessionInfo {
        private String username;
        private String sourceIp;
        private long creationTime;
        private long lastAccessTime;

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getSourceIp() { return sourceIp; }
        public void setSourceIp(String sourceIp) { this.sourceIp = sourceIp; }
        public long getCreationTime() { return creationTime; }
        public void setCreationTime(long creationTime) { this.creationTime = creationTime; }
        public long getLastAccessTime() { return lastAccessTime; }
        // Add this missing setter method:
        public void setLastAccessTime(long lastAccessTime) { this.lastAccessTime = lastAccessTime; }
    }
}
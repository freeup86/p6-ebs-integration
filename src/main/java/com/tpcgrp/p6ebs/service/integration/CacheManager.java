/**
 * Service for caching data to improve performance
 */
package com.tpcgrp.p6ebs.service.integration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class CacheManager {

    // Maps to store cached data
    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();

    // Default TTL for cache entries (in milliseconds)
    private final long DEFAULT_TTL = 30 * 60 * 1000; // 30 minutes

    // Timer for cache cleanup
    private final Timer cleanupTimer;

    public CacheManager() {
        cleanupTimer = new Timer("CacheCleanupTimer", true);
        cleanupTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                cleanupExpiredEntries();
            }
        }, 60000, 60000); // Run every minute
    }

    /**
     * Put an item in the cache with default TTL
     */
    public void put(String cacheKey, Object value) {
        put(cacheKey, value, DEFAULT_TTL);
    }

    /**
     * Put an item in the cache with specified TTL
     */
    public void put(String cacheKey, Object value, long ttlMillis) {
        if (cacheKey == null || value == null) {
            return;
        }

        CacheEntry entry = new CacheEntry();
        entry.setValue(value);
        entry.setExpirationTime(System.currentTimeMillis() + ttlMillis);

        cache.put(cacheKey, entry);
    }

    /**
     * Get an item from the cache
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String cacheKey) {
        if (cacheKey == null) {
            return null;
        }

        CacheEntry entry = cache.get(cacheKey);

        if (entry == null) {
            return null;
        }

        // Check if entry is expired
        if (System.currentTimeMillis() > entry.getExpirationTime()) {
            cache.remove(cacheKey);
            return null;
        }

        return (T) entry.getValue();
    }

    /**
     * Remove an item from the cache
     */
    public void remove(String cacheKey) {
        if (cacheKey != null) {
            cache.remove(cacheKey);
        }
    }

    /**
     * Clear entire cache
     */
    public void clear() {
        cache.clear();
    }

    /**
     * Clear cache for a specific prefix
     */
    public void clearPrefix(String prefix) {
        if (prefix == null) {
            return;
        }

        List<String> keysToRemove = new ArrayList<>();

        for (String key : cache.keySet()) {
            if (key.startsWith(prefix)) {
                keysToRemove.add(key);
            }
        }

        for (String key : keysToRemove) {
            cache.remove(key);
        }
    }

    /**
     * Clean up expired entries
     */
    private void cleanupExpiredEntries() {
        long now = System.currentTimeMillis();
        List<String> keysToRemove = new ArrayList<>();

        for (Map.Entry<String, CacheEntry> entry : cache.entrySet()) {
            if (now > entry.getValue().getExpirationTime()) {
                keysToRemove.add(entry.getKey());
            }
        }

        for (String key : keysToRemove) {
            cache.remove(key);
        }
    }

    /**
     * Get cache statistics
     */
    public CacheStats getStats() {
        CacheStats stats = new CacheStats();
        stats.setTotalEntries(cache.size());

        long now = System.currentTimeMillis();
        int expiredEntries = 0;

        for (CacheEntry entry : cache.values()) {
            if (now > entry.getExpirationTime()) {
                expiredEntries++;
            }
        }

        stats.setExpiredEntries(expiredEntries);

        return stats;
    }

    /**
     * Shutdown the cache manager
     */
    public void shutdown() {
        cleanupTimer.cancel();
    }

    /**
     * Cache entry class
     */
    private static class CacheEntry {
        private Object value;
        private long expirationTime;

        public Object getValue() { return value; }
        public void setValue(Object value) { this.value = value; }
        public long getExpirationTime() { return expirationTime; }
        public void setExpirationTime(long expirationTime) { this.expirationTime = expirationTime; }
    }

    /**
     * Cache statistics class
     */
    public static class CacheStats {
        private int totalEntries;
        private int expiredEntries;

        public int getTotalEntries() { return totalEntries; }
        public void setTotalEntries(int totalEntries) { this.totalEntries = totalEntries; }
        public int getExpiredEntries() { return expiredEntries; }
        public void setExpiredEntries(int expiredEntries) { this.expiredEntries = expiredEntries; }
    }
}
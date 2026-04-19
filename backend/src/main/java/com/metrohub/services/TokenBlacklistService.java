package com.metrohub.services;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TokenBlacklistService {

    private static final Logger log = LoggerFactory.getLogger(TokenBlacklistService.class);

    

    private final Map<String, Long> blacklistedTokens = new ConcurrentHashMap<>();

    

    private final Map<Long, Long> userTokenInvalidationTime = new ConcurrentHashMap<>();

        public void blacklistToken(String token, long expirationTimeInSeconds) {
        if (token == null || token.isEmpty()) {
            return;
        }
        
        // Calculate absolute expiration time
        long expirationTime = System.currentTimeMillis() + (expirationTimeInSeconds * 1000);
        
        // Hash the token for storage (security measure)
        String tokenHash = hashToken(token);
        blacklistedTokens.put(tokenHash, expirationTime);
        
        log.debug("🚫 Token blacklisted. Hash: {}..., expires in {} seconds", 
            tokenHash.substring(0, 8), expirationTimeInSeconds);
    }

        public boolean isBlacklisted(String token) {
        if (token == null || token.isEmpty()) {
            return false;
        }
        
        String tokenHash = hashToken(token);
        Long expirationTime = blacklistedTokens.get(tokenHash);
        
        if (expirationTime == null) {
            return false;
        }
        
        // Check if the token has expired naturally (can be removed)
        if (System.currentTimeMillis() > expirationTime) {
            blacklistedTokens.remove(tokenHash);
            return false;
        }
        
        return true;
    }

        public void blacklistAllUserTokens(Long userId) {
        if (userId == null) {
            return;
        }
        
        // Record the current time - any tokens issued before this are invalid
        userTokenInvalidationTime.put(userId, System.currentTimeMillis());
        
        log.info("🚫 All tokens blacklisted for user ID: {}", userId);
    }

    

    public boolean isTokenInvalidatedForUser(Long userId, long tokenIssuedAt) {
        if (userId == null) {
            return false;
        }
        
        Long invalidationTime = userTokenInvalidationTime.get(userId);
        if (invalidationTime == null) {
            return false;
        }
        
        return tokenIssuedAt < invalidationTime;
    }

    @Scheduled(fixedRate = 3600000) // Every hour
    public void cleanupExpiredTokens() {
        long now = System.currentTimeMillis();
        int removed = 0;

        // Remove expired tokens from blacklist
        Set<String> keysToRemove = new java.util.HashSet<>();
        for (Map.Entry<String, Long> entry : blacklistedTokens.entrySet()) {
            if (isTokenExpiredAt(entry, now)) {
                keysToRemove.add(entry.getKey());
            }
        }

        for (String key : keysToRemove) {
            blacklistedTokens.remove(key);
            removed++;
        }

        // Clean up old user invalidation entries (older than 7 days)
        long oneWeekAgo = now - (7 * 24 * 60 * 60 * 1000L);
        Set<Long> userKeysToRemove = new java.util.HashSet<>();
        for (Map.Entry<Long, Long> entry : userTokenInvalidationTime.entrySet()) {
            if (isOldUserInvalidation(entry, oneWeekAgo)) {
                userKeysToRemove.add(entry.getKey());
            }
        }

        for (Long key : userKeysToRemove) {
            userTokenInvalidationTime.remove(key);
        }

        if (removed > 0 || !userKeysToRemove.isEmpty()) {
            log.info("🧹 Token blacklist cleanup: removed {} expired tokens, {} old user entries",
                removed, userKeysToRemove.size());
        }
    }

        public long getBlacklistedCount() {
        return blacklistedTokens.size();
    }

    

    private String hashToken(String token) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(token.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            // Fallback to simple hash
            return String.valueOf(token.hashCode());
        }
    }

    private boolean isTokenExpiredAt(Map.Entry<String, Long> entry, long now) {
        return entry.getValue() < now;
    }

    private boolean isOldUserInvalidation(Map.Entry<Long, Long> entry, long threshold) {
        return entry.getValue() < threshold;
    }
}

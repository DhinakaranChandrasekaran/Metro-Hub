package com.metrohub.security;

import com.metrohub.models.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Component
public class JwtTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);

    @Value("${metrohub.jwt.secret}")
    private String jwtSecret;

    @Value("${metrohub.jwt.expiration-ms}")
    private long jwtExpirationMs;

    @Value("${metrohub.jwt.refresh-expiration-ms}")
    private long refreshExpirationMs;

    

    public String generateAccessToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("role", user.getRole().name());
        claims.put("departmentId", user.getDepartmentId());
        claims.put("name", user.getName());
        
        return createToken(claims, user.getEmail(), jwtExpirationMs);
    }

    

    public String generateAccessToken(Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        Map<String, Object> claims = new HashMap<>();
        claims.put("roles", userDetails.getAuthorities());
        
        return createToken(claims, userDetails.getUsername(), jwtExpirationMs);
    }

    

    public String generateRefreshToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("type", "refresh");
        claims.put("userId", user.getId());
        
        return createToken(claims, user.getEmail(), refreshExpirationMs);
    }

    

    private String createToken(Map<String, Object> claims, String subject, long expiration) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
    }

    

    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    

    public Long extractUserId(String token) {
        return extractClaim(token, this::extractUserIdFromClaims);
    }

    public String extractRole(String token) {
        return extractClaim(token, this::extractRoleFromClaims);
    }

    public Long extractDepartmentId(String token) {
        return extractClaim(token, this::extractDepartmentIdFromClaims);
    }

    

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    

    public boolean isTokenExpired(String token) {
        try {
            return extractExpiration(token).before(new Date());
        } catch (ExpiredJwtException e) {
            return true;
        }
    }

    

    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token);
            return true;
        } catch (MalformedJwtException e) {
            log.error("Invalid JWT token: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            log.error("Expired JWT token: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.error("Unsupported JWT token: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.error("JWT claims string is empty: {}", e.getMessage());
        }
        return false;
    }

    

    public boolean validateToken(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }

    

    public long getExpirationInSeconds() {
        return jwtExpirationMs / 1000;
    }

    

    public boolean isRefreshToken(String token) {
        try {
            String type = extractClaim(token, this::extractTokenTypeFromClaims);
            return "refresh".equals(type);
        } catch (Exception e) {
            return false;
        }
    }

    private Long extractUserIdFromClaims(Claims claims) {
        return claims.get("userId", Long.class);
    }

    private String extractRoleFromClaims(Claims claims) {
        return claims.get("role", String.class);
    }

    private Long extractDepartmentIdFromClaims(Claims claims) {
        return claims.get("departmentId", Long.class);
    }

    private String extractTokenTypeFromClaims(Claims claims) {
        return claims.get("type", String.class);
    }
}

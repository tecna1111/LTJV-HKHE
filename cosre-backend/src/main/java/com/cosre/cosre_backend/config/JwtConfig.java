package com.cosre.cosre_backend.config;

import com.cosre.cosre_backend.common.constants.RoleEnum;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

@Component
public class JwtConfig {

    @Value("${security.jwt.secret:changeitsecretkeychangeme1234567890}")
    private String jwtSecret;

    @Value("${security.jwt.expiration-ms:86400000}")
    private long expirationMs;

    @Value("${security.jwt.refresh-expiration-ms:604800000}")
    private long refreshExpirationMs;

    private Key key;

    @PostConstruct
    public void init() {
        this.key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(String username, RoleEnum role) {
        return generateToken(username, role, "access", expirationMs);
    }

    public String generateRefreshToken(String username, RoleEnum role) {
        return generateToken(username, role, "refresh", refreshExpirationMs);
    }

    private String generateToken(String username, RoleEnum role, String type, long lifetime) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + lifetime);

        return Jwts.builder()
                .setSubject(username)
                .claim("role", role.name())
                .claim("type", type)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(key)
                .compact();
    }

    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    public String getUsername(String token) {
        return parseClaims(token).getBody().getSubject();
    }

    public RoleEnum getRole(String token) {
        String role = parseClaims(token).getBody().get("role", String.class);
        return role != null ? RoleEnum.valueOf(role) : null;
    }

    public boolean isAccessToken(String token) {
        return "access".equals(parseClaims(token).getBody().get("type", String.class));
    }

    public boolean isRefreshToken(String token) {
        return "refresh".equals(parseClaims(token).getBody().get("type", String.class));
    }

    public long getExpirationMs() {
        return expirationMs;
    }

    private Jws<Claims> parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token);
    }
}

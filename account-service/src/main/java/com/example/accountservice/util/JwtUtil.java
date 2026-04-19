package com.example.accountservice.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.example.accountservice.enums.Role;
import com.example.accountservice.model.Account;

import javax.crypto.SecretKey;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
public class JwtUtil {

    @Value("${jwt.secret:mySecretKeyForJwtTokenThatShouldBeAtLeast256BitsLongToEnsureSecurityAndProperFunctioning}")
    private String jwtSecret;

    @Value("${jwt.expiration:86400000}") // 24 hours in milliseconds
    private long jwtExpiration;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

    public String generateToken(Account account) {
        String username = account.getUsername();
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpiration);
        String jwtId = UUID.randomUUID().toString(); // Tạo unique JWT ID

        return Jwts.builder()
                .subject(username) // Dùng userId làm subject thay vì username
                // .claim("userId", userId)
                .claim("username", account.getUsername())
                .claim("role", account.getRole())
                .id(jwtId) // Thêm JWT ID
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
    }

    public String getUsernameFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claims.getSubject(); // Sẽ trả về userId dưới dạng string
    }

    public Long getUserIdFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claims.get("userId", Long.class);
    }

    // Method để lấy JWT ID
    public String getJwtIdFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claims.getId();
    }

    public String extractDomain(String url) {
        if (url == null || url.isEmpty()) {
            return null;
        }

        try {
            // Nếu người dùng truyền thiếu http/https thì thêm tạm
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                url = "http://" + url;
            }

            URI uri = new URI(url);
            String host = uri.getHost();

            if (host == null) {
                return null;
            }

            // Bỏ "www." nếu có
            if (host.startsWith("www.")) {
                host = host.substring(4);
            }

            return host;

        } catch (URISyntaxException e) {
            System.err.println("Invalid URL format: " + url);
            return null;
        }
    }

}
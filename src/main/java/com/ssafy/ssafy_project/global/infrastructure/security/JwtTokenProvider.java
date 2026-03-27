package com.ssafy.ssafy_project.global.infrastructure.security;

import com.ssafy.ssafy_project.global.domain.entity.TokenType;
import com.ssafy.ssafy_project.global.domain.entity.Tokens;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@Component
@NoArgsConstructor
public class JwtTokenProvider {
    @Value("${jwt.secret_key}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expirationTime;

    @Getter
    @Value("${jwt.refresh.expiration}")
    private long refreshExpirationTime;

    private SecretKey secretKey;

    @PostConstruct
    private void init() {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public Tokens generateToken(Long userId) {
       String accessToken = generateAccessToken(userId);
       String refreshToken = generateRefreshToken(userId);


        return new Tokens(accessToken, refreshToken);
    }

    public String generateAccessToken(Long userId) {
        Instant now = Instant.now();
        Instant atExp = now.plus(expirationTime, ChronoUnit.SECONDS);

        return Jwts.builder()
                .subject("" + userId)
                .claim("type", TokenType.REFRESH_TOKEN.getTypeName())
                .issuedAt(Date.from(now))
                .expiration(Date.from(atExp))
                .signWith(secretKey)
                .compact();
    }

    public String generateRefreshToken(Long userId) {
        Instant now = Instant.now();
        Instant rtExp = now.plus(refreshExpirationTime, ChronoUnit.SECONDS);

        return Jwts.builder()
                .subject("" + userId)
                .claim("type", TokenType.REFRESH_TOKEN.getTypeName())
                .issuedAt(Date.from(now))
                .expiration(Date.from(rtExp))
                .signWith(secretKey)
                .compact();
    }

    public boolean validateToken(String token, TokenType tokenType) {
        return false;
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

}

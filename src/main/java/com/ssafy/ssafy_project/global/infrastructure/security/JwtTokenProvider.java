package com.ssafy.ssafy_project.global.infrastructure.security;

import com.ssafy.ssafy_project.global.domain.entity.Tokens;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@Component
@NoArgsConstructor
public class JwtTokenProvider {
    @Value("${secret_key}")
    private String secretKey;

    @Value("${jwt.expiration}")
    private String expirationTime;

    @Value("${jwt.refresh.expiration}")
    private String refreshExpirationTime;

    public Tokens generateToken(Long userId) {
        Instant now = Instant.now();
        Instant atExp = now.plus(Long.parseLong(expirationTime), ChronoUnit.SECONDS);
        Instant rtExp = now.plus(Long.parseLong(refreshExpirationTime), ChronoUnit.SECONDS);

        String accessToken = Jwts.builder()
                .setSubject("" + userId)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(atExp))
                .signWith(SignatureAlgorithm.HS256, secretKey)
                .compact();

        String refreshToken = Jwts.builder()
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(rtExp))
                .signWith(SignatureAlgorithm.HS256, secretKey)
                .compact();



        return new Tokens(accessToken, refreshToken, atExp, rtExp);
    }

}

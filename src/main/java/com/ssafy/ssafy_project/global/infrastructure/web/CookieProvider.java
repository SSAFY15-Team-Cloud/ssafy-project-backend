package com.ssafy.ssafy_project.global.infrastructure.web;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Optional;

@Component
public class CookieProvider {

    private static final String REFRESH_TOKEN_COOKIE_NAME = "refreshToken";

    @Value("${jwt.refresh.expiration}")
    private long refreshTokenExpirationTime;

    private long refreshTokenMaxAge;

    @PostConstruct
    public void init() {
        refreshTokenMaxAge = refreshTokenExpirationTime / 1000;
    }

    public ResponseCookie createRefreshTokenCookie(String refreshToken) {
        return ResponseCookie.from(REFRESH_TOKEN_COOKIE_NAME, refreshToken)
                .httpOnly(true)
                .secure(false) // 배포 환경 HTTPS에서는 꼭 true로 변경
                .path("/")
                .maxAge(refreshTokenMaxAge)
                .sameSite("Lax") // 나중에 도메인 따라 변경해야함
                .build();
    }

    public ResponseCookie deleteRefreshTokenCookie() {
        return ResponseCookie.from(REFRESH_TOKEN_COOKIE_NAME, "")
                .httpOnly(true)
                .secure(false) // HTTPS 환경에서는 true
                .path("/")
                .maxAge(0)
                .sameSite("Lax") // 나중에 도메인 따라 변경
                .build();
    }

    public String getRefreshTokenFromCookie(HttpServletRequest request) {
        if(request.getCookies() == null) {
            return null;
        }

        Optional<Cookie> cookie = Arrays.stream(request.getCookies())
                .filter(c -> REFRESH_TOKEN_COOKIE_NAME.equals(c.getName()))
                .findFirst();

        return cookie.map(Cookie::getValue).orElse(null);
    }
}

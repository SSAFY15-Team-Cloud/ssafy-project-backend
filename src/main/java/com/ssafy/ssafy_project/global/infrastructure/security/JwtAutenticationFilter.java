package com.ssafy.ssafy_project.global.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAutenticationFilter extends OncePerRequestFilter {
    // TODO : 인증 과정 작성 공간

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        // TODO : JWT 토큰 유효성 검증 및 Response 객체에 User 정보 넣어주고, 오류나면 오류 객체 넣어주기
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String requestURI = request.getRequestURI();
        String method = request.getMethod();

        return method.equals("POST") && ( requestURI.contains("/login") || requestURI.contains("/reissue"));
    }

    
}

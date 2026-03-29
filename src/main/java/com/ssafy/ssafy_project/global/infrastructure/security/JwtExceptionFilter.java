package com.ssafy.ssafy_project.global.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@RequiredArgsConstructor
public class JwtExceptionFilter extends OncePerRequestFilter {
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
//        try {
//            filterChain.doFilter(request, response);
//        } catch (JwtAuthenticationException e) {
//            writeResponse(response, e.getErrorCode());
//        }
    }

//    private void writeResponse(HttpServletResponse response, JwtErrorCode errorCode) throws IOException{
//        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
//        response.setCharacterEncoding(StandardCharsets.UTF_8);
//        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
//
//        ErrorResponseDto errorResponse = new ErrorResponseDto(
//                errorCode.getCode(),
//                errorCode.getMessage()
//        );
//
//        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
//    }
}

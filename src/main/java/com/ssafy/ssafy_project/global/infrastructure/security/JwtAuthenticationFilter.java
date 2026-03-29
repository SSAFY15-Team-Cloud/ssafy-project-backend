package com.ssafy.ssafy_project.global.infrastructure.security;

import com.ssafy.ssafy_project.global.application.port.out.JwtPortOut;
import com.ssafy.ssafy_project.global.domain.entity.TokenType;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtPortOut jwtPortOut;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String token = resolveToken(request);

        if (token == null) {
            filterChain.doFilter(request, response);
            return;
        }

        if (!jwtPortOut.validate(token, TokenType.ACCESS_TOKEN)) {
            throw new BadCredentialsException("INVALID TOKEN");
        }

        Long userId = jwtPortOut.extractUserId(token);

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        userId,
                        null,
                        AuthorityUtils.NO_AUTHORITIES  // 추후 권한 설정
                );

        SecurityContextHolder.getContext().setAuthentication(authentication);


        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");

        if (bearerToken == null || bearerToken.isBlank()) {
            return null;
        }

        if (!bearerToken.startsWith("Bearer ")) {
            //throw new JwtAuthenticationException(JwtErrorCode.INVALID_TOKEN);
            throw new BadCredentialsException("INVALID TOKEN");
        }

        return bearerToken.substring(7);
    }

}

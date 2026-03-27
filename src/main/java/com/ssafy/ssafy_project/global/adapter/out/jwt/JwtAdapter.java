package com.ssafy.ssafy_project.global.adapter.out.jwt;

import com.ssafy.ssafy_project.global.application.port.out.JwtPortOut;
import com.ssafy.ssafy_project.global.application.port.out.RefreshTokenPortOut;
import com.ssafy.ssafy_project.global.domain.entity.Tokens;
import com.ssafy.ssafy_project.global.infrastructure.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JwtAdapter implements JwtPortOut {

    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenPortOut refreshTokenPortOut;


    @Override
    public Tokens generate(Long userId) {
        Tokens tokens = jwtTokenProvider.generateToken(userId);
        refreshTokenPortOut.save(userId, tokens.refreshToken(),  jwtTokenProvider.getRefreshExpirationTime());
        return tokens;
    }
}

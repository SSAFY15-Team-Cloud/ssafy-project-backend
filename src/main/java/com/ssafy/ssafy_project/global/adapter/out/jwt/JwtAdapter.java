package com.ssafy.ssafy_project.global.adapter.out.jwt;

import com.ssafy.ssafy_project.global.application.port.out.JwtPortOut;
import com.ssafy.ssafy_project.global.application.port.out.RedisPortOut;
import com.ssafy.ssafy_project.global.domain.entity.Tokens;
import com.ssafy.ssafy_project.global.infrastructure.redis.RedisStore;
import com.ssafy.ssafy_project.global.infrastructure.security.JwtTokenProvider;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class JwtAdapter implements JwtPortOut {


    private final JwtTokenProvider jwtTokenProvider;
    private final RedisStore redisStore;

    @Override
    public Tokens generate(Long userId) {
        Tokens tokens = jwtTokenProvider.generateToken(userId);

        redisStore.setRefreshToken("refreshToken:" + userId, tokens.refreshToken(), tokens.rtExpiresIn());

        return tokens;
    }

}

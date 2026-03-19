package com.ssafy.ssafy_project.global.infrastructure.redis;

import com.ssafy.ssafy_project.global.application.port.out.RedisPortOut;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class RedisStore {
    private final RedisTemplate<String, String> redisTemplate;

    public void setRefreshToken(String key, String token, Instant expires) {
        Duration duration = Duration.between( Instant.now(), expires);
        redisTemplate.opsForValue().set(key, token, duration.getSeconds(), TimeUnit.SECONDS);
    }

}

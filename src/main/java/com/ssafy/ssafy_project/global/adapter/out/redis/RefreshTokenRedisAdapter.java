package com.ssafy.ssafy_project.global.adapter.out.redis;

import com.ssafy.ssafy_project.global.application.port.out.RefreshTokenPortOut;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class RefreshTokenRedisAdapter implements RefreshTokenPortOut {

    private static final String KEY_PREFIX = "refreshToken:";

    private final RedisTemplate<String, String> redisTemplate;

    @Override
    public void save(Long userId, String refreshToken, long expirationMillis) {
        redisTemplate.opsForValue().set(
                KEY_PREFIX + userId,
                refreshToken,
                Duration.ofMillis(expirationMillis)
        );
    }

    @Override
    public Optional<String> find(Long userId) {
        return Optional.ofNullable(redisTemplate.opsForValue().get(KEY_PREFIX + userId));
    }

    @Override
    public void delete(Long userId) {
        redisTemplate.delete(KEY_PREFIX + userId);
    }
}
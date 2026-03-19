package com.ssafy.ssafy_project.global.application.port.out;

import java.time.Instant;

public interface RedisPortOut {
    void saveToken(String refreshToken, Instant exp);
}

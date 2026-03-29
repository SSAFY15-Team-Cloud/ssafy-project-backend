package com.ssafy.ssafy_project.global.application.port.out;

import java.util.Optional;

public interface RefreshTokenPortOut {

    void save(Long userId, String refreshToken, long expirationMillis);

    Optional<String> find(Long userId);

    void delete(Long userId);
}

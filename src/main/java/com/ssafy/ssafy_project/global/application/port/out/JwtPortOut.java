package com.ssafy.ssafy_project.global.application.port.out;

import com.ssafy.ssafy_project.global.domain.entity.TokenType;
import com.ssafy.ssafy_project.global.domain.entity.Tokens;

public interface JwtPortOut {
    Tokens generate(Long userId);

    boolean validate(String token, TokenType tokenType);

    Long extractUserId(String token);

    boolean matchesRefreshToken(Long userId, String refreshToken);

    void deleteRefreshToken(Long userId);
}

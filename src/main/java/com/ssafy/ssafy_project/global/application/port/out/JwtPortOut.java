package com.ssafy.ssafy_project.global.application.port.out;

import com.ssafy.ssafy_project.global.domain.entity.Tokens;

public interface JwtPortOut {
    Tokens generate(Long userId);
}

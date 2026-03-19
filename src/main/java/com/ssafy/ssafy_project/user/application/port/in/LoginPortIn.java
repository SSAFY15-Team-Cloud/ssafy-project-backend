package com.ssafy.ssafy_project.user.application.port.in;

import com.ssafy.ssafy_project.global.domain.entity.Tokens;

public interface LoginPortIn {

    // 테스트용 Login 이다. jwt 토큰 사용으로 변경해야 함.
    Tokens login(String username, String password);
}

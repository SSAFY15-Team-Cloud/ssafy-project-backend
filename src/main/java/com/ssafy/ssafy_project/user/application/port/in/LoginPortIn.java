package com.ssafy.ssafy_project.user.application.port.in;

import com.ssafy.ssafy_project.global.domain.entity.Tokens;

public interface LoginPortIn {
    Tokens login(String email, String password);
}

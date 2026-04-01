package com.ssafy.ssafy_project.user.application.port.out;

import com.ssafy.ssafy_project.user.domain.User;

public interface LoadUserPortOut {
    User loadById(Long userId);
    User loadByEmail(String email);

    boolean existsByEmail(String email);
    boolean existsActiveByNickname(String nickname);
}

package com.ssafy.ssafy_project.user.application.port.out;

import com.ssafy.ssafy_project.user.domain.User;

public interface LoadUserPortOut {
    User findByUsername(String username);

}

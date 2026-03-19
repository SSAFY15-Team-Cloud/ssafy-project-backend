package com.ssafy.ssafy_project.user.application.port.out;

import com.ssafy.ssafy_project.user.adapter.out.persistence.entity.UserJpaEntity;
import com.ssafy.ssafy_project.user.domain.user.User;

import java.util.Optional;

public interface LoadUserPortOut {
    User findByUsername(String username);

}

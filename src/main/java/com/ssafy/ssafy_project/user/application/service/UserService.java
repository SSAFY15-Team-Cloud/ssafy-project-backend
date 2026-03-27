package com.ssafy.ssafy_project.user.application.service;

import com.ssafy.ssafy_project.global.application.port.out.JwtPortOut;
import com.ssafy.ssafy_project.global.domain.entity.Tokens;
import com.ssafy.ssafy_project.user.application.port.in.LoginPortIn;
import com.ssafy.ssafy_project.user.application.port.out.LoadUserPortOut;
import com.ssafy.ssafy_project.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService implements LoginPortIn {

    private final LoadUserPortOut loadUserPortOut;
    private final JwtPortOut jwtPortOut;

    @Override
    public Tokens login(String email, String password) {
        User user = loadUserPortOut.loadByEmail(email);

        if (user.isDeleted()) {
            throw new RuntimeException("삭제된 사용자입니다.");
        }

        if (!user.getPassword().equals(password)) {
            throw new RuntimeException("비밀번호가 일치하지 않습니다.");
        }

        return jwtPortOut.generate(user.getId());
    }
}

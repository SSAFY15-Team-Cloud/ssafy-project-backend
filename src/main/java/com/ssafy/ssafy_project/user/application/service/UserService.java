package com.ssafy.ssafy_project.user.application.service;

import com.ssafy.ssafy_project.global.application.port.out.JwtPortOut;
import com.ssafy.ssafy_project.global.domain.entity.Tokens;
import com.ssafy.ssafy_project.user.adapter.in.web.dto.response.ResponseLogin;
import com.ssafy.ssafy_project.user.application.port.in.LoginPortIn;
import com.ssafy.ssafy_project.user.application.port.out.LoadUserPortOut;
import com.ssafy.ssafy_project.user.domain.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/*
    PortIn의 구현체
 */
@Service
@RequiredArgsConstructor
public class UserService implements LoginPortIn {

    private final LoadUserPortOut loadUserPortOut;
    private final JwtPortOut jwtPortOut;

    @Override
    public Tokens login(String username, String password) {


        User foundedUser = loadUserPortOut.findByUsername(username);

        if(!foundedUser.getUsername().equals(username) || !foundedUser.getPassword().equals(password)) {
            throw new RuntimeException("비밀번호 불일치");
        }

        return jwtPortOut.generate(foundedUser.getId());
    }
}

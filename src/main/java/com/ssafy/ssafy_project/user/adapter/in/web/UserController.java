package com.ssafy.ssafy_project.user.adapter.in.web;

import com.ssafy.ssafy_project.global.domain.entity.Tokens;
import com.ssafy.ssafy_project.user.adapter.in.web.dto.request.LoginRequest;
import com.ssafy.ssafy_project.user.application.port.in.LoginPortIn;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class UserController {
    private final LoginPortIn loginPortIn;

    @PostMapping("/login")
    public Tokens login(@RequestBody LoginRequest loginRequest) {
        System.out.println(loginRequest);
        return loginPortIn.login(loginRequest.getUsername(), loginRequest.getPassword());
    }
}

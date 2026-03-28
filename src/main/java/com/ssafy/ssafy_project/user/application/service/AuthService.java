package com.ssafy.ssafy_project.user.application.service;


import com.ssafy.ssafy_project.global.application.port.out.JwtPortOut;
import com.ssafy.ssafy_project.global.domain.entity.TokenType;
import com.ssafy.ssafy_project.global.domain.entity.Tokens;
import com.ssafy.ssafy_project.user.application.port.in.*;
import com.ssafy.ssafy_project.user.application.port.out.LoadUserPortOut;
import com.ssafy.ssafy_project.user.application.port.out.RegisterUserPortOut;
import com.ssafy.ssafy_project.user.domain.User;
import com.ssafy.ssafy_project.user.domain.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService implements LoginPortIn, SignUpPortIn, LogoutPortIn, RefreshTokenPortIn {
    private final LoadUserPortOut loadUserPortOut;
    private final RegisterUserPortOut registerUserPortOut;
    private final JwtPortOut jwtPortOut;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public Tokens login(String email, String password) {
        User user = loadUserPortOut.loadByEmail(email);

        if(user.isDeleted()) {
            throw new IllegalArgumentException("삭제된 사용자 입니다.");
        }

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }

        return jwtPortOut.generate(user.getId());
    }

    @Override
    @Transactional
    public void logout(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return;
        }

        if (!jwtPortOut.validate(refreshToken, TokenType.REFRESH_TOKEN)) {
            return;
        }

        Long userId = jwtPortOut.extractUserId(refreshToken);
        jwtPortOut.deleteRefreshToken(userId);
    }

    @Override
    @Transactional
    public Tokens reissue(String refreshToken) {
        if (!jwtPortOut.validate(refreshToken, TokenType.REFRESH_TOKEN)) {
            throw new IllegalArgumentException("올바르지 않은 토큰");
        }

        Long userId = jwtPortOut.extractUserId(refreshToken);

        if (!jwtPortOut.matchesRefreshToken(userId, refreshToken)) {
            throw new IllegalArgumentException("올바르지 않은 토큰");
        }
        ;

        return jwtPortOut.generate(userId);
    }

    @Override
    @Transactional
    public Long signUp(SignUpCommand signUpCommand) {
        if (loadUserPortOut.existsByEmail(signUpCommand.email())) {
            throw new IllegalArgumentException("이미 존재하는 이메일");
        }

        String encodedPassword = passwordEncoder.encode(signUpCommand.password());

        User user = new User(
                signUpCommand.email(),
                encodedPassword,
                signUpCommand.nickname(),
                signUpCommand.name(),
                UserRole.USER,
                null
        );

        registerUserPortOut.registerUser(user);

        return user.getId();
    }
}

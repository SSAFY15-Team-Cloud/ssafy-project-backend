package com.ssafy.ssafy_project.user.application.service;


import com.ssafy.ssafy_project.global.application.port.out.JwtPortOut;
import com.ssafy.ssafy_project.global.exception.CommonErrorCode;
import com.ssafy.ssafy_project.global.exception.CustomException;
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
            throw new CustomException(CommonErrorCode.INVALID_CREDENTIALS);
        }

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new CustomException(CommonErrorCode.INVALID_CREDENTIALS);
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
            throw new CustomException(CommonErrorCode.INVALID_TOKEN);
        }

        Long userId = jwtPortOut.extractUserId(refreshToken);

        if (!jwtPortOut.matchesRefreshToken(userId, refreshToken)) {
            throw new CustomException(CommonErrorCode.INVALID_TOKEN);
        }


        return jwtPortOut.generate(userId);
    }

    @Override
    @Transactional
    public Long signUp(SignUpCommand command) {
        if (loadUserPortOut.existsByEmail(command.email())) {
            throw new CustomException(CommonErrorCode.DUPLICATE_EMAIL);
        }

        if(loadUserPortOut.existsActiveByNickname(command.nickname())) {
            throw new CustomException(CommonErrorCode.DUPLICATE_NICKNAME);
        }

        String encodedPassword = passwordEncoder.encode(command.password());

        User user = new User(
                command.email(),
                encodedPassword,
                command.nickname(),
                command.name(),
                UserRole.USER,
                null
        );

        return registerUserPortOut.registerUser(user).getId();
    }
}

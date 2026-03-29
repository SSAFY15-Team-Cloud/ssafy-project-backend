package com.ssafy.ssafy_project.user.adapter.in.web;

import com.ssafy.ssafy_project.global.domain.entity.Tokens;
import com.ssafy.ssafy_project.global.infrastructure.web.CookieProvider;
import com.ssafy.ssafy_project.user.adapter.in.web.dto.request.LoginRequest;
import com.ssafy.ssafy_project.user.adapter.in.web.dto.request.SignUpRequest;
import com.ssafy.ssafy_project.user.adapter.in.web.dto.response.AuthTokenResponse;
import com.ssafy.ssafy_project.user.application.port.in.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final LoginPortIn loginPortIn;
    private final RefreshTokenPortIn refreshTokenPortIn;
    private final LogoutPortIn logoutPortIn;
    private final SignUpPortIn signUpPortIn;
    private final CookieProvider cookieProvider;


    @PostMapping("/login")
    public ResponseEntity<AuthTokenResponse> login(@RequestBody LoginRequest request) {
        Tokens tokens = loginPortIn.login(request.email(), request.password());

        ResponseCookie refreshTokenCookie = cookieProvider.createRefreshTokenCookie(
                tokens.refreshToken()
        );

        AuthTokenResponse response = new AuthTokenResponse(tokens.accessToken());

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.SET_COOKIE,
                        refreshTokenCookie.toString()
                )
                .body(response);
    }

    @PostMapping("/signup")
    public ResponseEntity<Long> signup(@RequestBody SignUpRequest request) {
        Long response = signUpPortIn.signUp(
                new SignUpCommand(
                        request.email(),
                        request.password(),
                        request.nickname(),
                        request.name()
                )
        );

        return ResponseEntity.ok(response);
    }

    @PostMapping("/reissue")
    public ResponseEntity<AuthTokenResponse> reissue(HttpServletRequest request) {
        String refreshToken = cookieProvider.getRefreshTokenFromCookie(request);

        Tokens tokens = refreshTokenPortIn.reissue(refreshToken);

        ResponseCookie refreshTokenCookie = cookieProvider.createRefreshTokenCookie((
                tokens.refreshToken()
        ));

        AuthTokenResponse response = new AuthTokenResponse(tokens.accessToken());

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.SET_COOKIE,
                        refreshTokenCookie.toString()
                )
                .body(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        String refreshToken = cookieProvider.getRefreshTokenFromCookie(request);

        logoutPortIn.logout(refreshToken);

        ResponseCookie deletedCookie =cookieProvider.deleteRefreshTokenCookie();

        return ResponseEntity.status(HttpStatus.NO_CONTENT)
                .header(HttpHeaders.SET_COOKIE, deletedCookie.toString())
                .build();
    }
}

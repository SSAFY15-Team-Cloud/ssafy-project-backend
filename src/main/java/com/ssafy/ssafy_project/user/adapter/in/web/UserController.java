package com.ssafy.ssafy_project.user.adapter.in.web;

import com.ssafy.ssafy_project.global.infrastructure.web.CookieProvider;
import com.ssafy.ssafy_project.user.adapter.in.web.dto.request.ChangePasswordRequest;
import com.ssafy.ssafy_project.user.adapter.in.web.dto.request.ChangeProfileImageRequest;
import com.ssafy.ssafy_project.user.adapter.in.web.dto.request.UpdateNicknameRequest;
import com.ssafy.ssafy_project.user.adapter.in.web.dto.request.WithdrawRequest;
import com.ssafy.ssafy_project.user.adapter.in.web.dto.response.GenerateProfileImageUploadUrlResponse;
import com.ssafy.ssafy_project.user.adapter.in.web.dto.response.MyInfoResponse;
import com.ssafy.ssafy_project.user.application.port.in.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController {
    private final GetMyInfoPortIn getMyInfoPortIn;
    private final UpdateNicknamePortIn updateNicknamePortIn;
    private final ChangePasswordPortIn changePasswordPortIn;
    private final WithdrawUserPortIn withdrawUserPortIn;
    private final ChangeProfileImagePortIn changeProfileImagePortIn;
    private final GenerateProfileImageUploadUrlPortIn generateProfileImageUploadUrlPortIn;
    private final DeleteProfileImagePortIn deleteProfileImagePortIn;
    private final CookieProvider cookieProvider;

    @GetMapping("/me")
    public ResponseEntity<MyInfoResponse> getMyInfo(@AuthenticationPrincipal Long userId) {
        MyInfoResult result = getMyInfoPortIn.getMyInfo(new GetMyInfoCommand(userId));
        MyInfoResponse response = new MyInfoResponse(
                result.userId(),
                result.email(),
                result.nickname(),
                result.name(),
                result.profileImageUrl()
        );

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/me/nickname")
    public ResponseEntity<Void> updateUserNickname(@AuthenticationPrincipal Long userId, @Valid @RequestBody UpdateNicknameRequest request) {
        updateNicknamePortIn.updateNickname(
                new UpdateNicknameCommand(userId, request.nickname())
        );

        return ResponseEntity.noContent()
                .build();
    }

    @PatchMapping("/me/password")
    public ResponseEntity<Void> changeUserPassword(@AuthenticationPrincipal Long userId, @Valid @RequestBody ChangePasswordRequest request) {
        changePasswordPortIn.changePassword(
                new ChangePasswordCommand(userId, request.currentPassword(), request.newPassword())
        );

        return ResponseEntity.noContent()
                .build();
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> withdrawUser(@AuthenticationPrincipal Long userId, @Valid @RequestBody WithdrawRequest request) {

        withdrawUserPortIn.withdraw(
                new WithdrawUserCommand(userId, request.password())
        );

        ResponseCookie deletedCookie = cookieProvider.deleteRefreshTokenCookie();

        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, deletedCookie.toString())
                .build();
    }

    @GetMapping("/me/profile-image/upload-url")
    public ResponseEntity<GenerateProfileImageUploadUrlResponse> generateProfileImageUploadUrl(@AuthenticationPrincipal Long userId) {
        GenerateProfileImageUploadUrlResult result = generateProfileImageUploadUrlPortIn.generateUrl(
                new GenerateProfileImageUploadUrlCommand(userId)
        );

        return ResponseEntity.ok(
                new GenerateProfileImageUploadUrlResponse(
                        result.uploadUrl(),
                        result.objectKey()
                )
        );
    }

    @PostMapping("/me/profile-image")
    public ResponseEntity<Void> changeUserProfileImage(@AuthenticationPrincipal Long userId, @Valid @RequestBody ChangeProfileImageRequest request) {
        changeProfileImagePortIn.changeProfileImage(
                new ChangeProfileImageCommand(
                        userId, request.objectKey()
                )
        );

        return ResponseEntity.noContent()
                .build();
    }

    @DeleteMapping("/me/profile-image")
    public ResponseEntity<Void> deleteUserProfileImage(@AuthenticationPrincipal Long userId) {
        deleteProfileImagePortIn.deleteProfileImage(
                new DeleteProfileImageCommand(userId)
        );

        return ResponseEntity.noContent()
                .build();
    }

}

package com.ssafy.ssafy_project.user.adapter.in.web;

import com.ssafy.ssafy_project.user.adapter.in.web.dto.request.ChangePasswordRequest;
import com.ssafy.ssafy_project.user.adapter.in.web.dto.request.UpdateNicknameRequest;
import com.ssafy.ssafy_project.user.adapter.in.web.dto.response.MyInfoResponse;
import com.ssafy.ssafy_project.user.application.port.in.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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

}

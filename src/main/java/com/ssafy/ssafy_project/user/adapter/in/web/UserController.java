package com.ssafy.ssafy_project.user.adapter.in.web;

import com.ssafy.ssafy_project.user.adapter.in.web.dto.response.MyInfoResponse;
import com.ssafy.ssafy_project.user.application.port.in.GetMyInfoPortIn;
import com.ssafy.ssafy_project.user.application.port.in.MyInfoResult;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController {
    private final GetMyInfoPortIn getMyInfoPortIn;

    @GetMapping("/me")
    public ResponseEntity<MyInfoResponse> getMyInfo(@AuthenticationPrincipal Long userId) {
        MyInfoResult result = getMyInfoPortIn.getMyInfo(userId);
        MyInfoResponse response = new MyInfoResponse(
                result.userId(),
                result.email(),
                result.nickname(),
                result.name(),
                result.profileImageUrl()
        );

        return ResponseEntity.ok()
                .body(response);
    }

}

package com.ssafy.ssafy_project.user.adapter.in.web.dto.request;

import jakarta.validation.constraints.NotBlank;

public record WithdrawRequest(
        @NotBlank(message = "비밀번호를 입력해주세요.")
        String password
) {
}

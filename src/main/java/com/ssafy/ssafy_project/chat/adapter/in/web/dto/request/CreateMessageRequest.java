package com.ssafy.ssafy_project.chat.adapter.in.web.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CreateMessageRequest(
        @NotBlank
        String message
) {
}
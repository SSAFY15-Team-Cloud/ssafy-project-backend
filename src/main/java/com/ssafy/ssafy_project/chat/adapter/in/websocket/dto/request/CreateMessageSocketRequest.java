package com.ssafy.ssafy_project.chat.adapter.in.websocket.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CreateMessageSocketRequest(
        @NotBlank
        String message
) {
}

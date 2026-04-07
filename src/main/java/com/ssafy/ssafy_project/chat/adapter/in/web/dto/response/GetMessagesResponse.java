package com.ssafy.ssafy_project.chat.adapter.in.web.dto.response;

import java.util.List;

public record GetMessagesResponse(
        List<MessageDetailResponse> messages
) {
}

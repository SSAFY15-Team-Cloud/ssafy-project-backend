package com.ssafy.ssafy_project.chat.application.port.in;

import java.util.List;

public record GetMessagesResult(
        List<MessageDetailResult> messages
) {
}

package com.ssafy.ssafy_project.chat.application.port.out;

import com.ssafy.ssafy_project.chat.domain.ChatMessage;

public interface CreateMessagePortOut {
    ChatMessage createMessage(ChatMessage chatMessage);
}

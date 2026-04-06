package com.ssafy.ssafy_project.chat.application.port.out;

import com.ssafy.ssafy_project.chat.domain.ChatMessage;

import java.util.List;

public interface GetMessagesPortOut {
    List<ChatMessage> getMessages(Long roomId);
}

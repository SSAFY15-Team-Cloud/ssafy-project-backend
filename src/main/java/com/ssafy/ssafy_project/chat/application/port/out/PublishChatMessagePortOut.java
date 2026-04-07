package com.ssafy.ssafy_project.chat.application.port.out;

public interface PublishChatMessagePortOut {
    void publish(ChatMessagePublishedData chatMessagePublishedData);
}

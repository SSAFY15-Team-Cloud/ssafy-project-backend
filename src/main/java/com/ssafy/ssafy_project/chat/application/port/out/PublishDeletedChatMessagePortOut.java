package com.ssafy.ssafy_project.chat.application.port.out;

public interface PublishDeletedChatMessagePortOut {
    void publish(ChatMessageDeletedData chatMessageDeletedData);
}

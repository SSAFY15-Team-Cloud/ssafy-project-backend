package com.ssafy.ssafy_project.chat.adapter.out.messaging;

import com.ssafy.ssafy_project.chat.adapter.out.messaging.dto.response.ChatMessageDeletedPayload;
import com.ssafy.ssafy_project.chat.application.port.out.ChatMessageDeletedData;
import com.ssafy.ssafy_project.chat.application.port.out.PublishDeletedChatMessagePortOut;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StompChatDeletePublisherAdapter implements PublishDeletedChatMessagePortOut {

    private final SimpMessagingTemplate simpMessagingTemplate;

    @Override
    public void publish(ChatMessageDeletedData chatMessageDeletedData) {
        ChatMessageDeletedPayload payload = new ChatMessageDeletedPayload(
                chatMessageDeletedData.messageId()
        );

        simpMessagingTemplate.convertAndSend(
                "/sub/rooms/" + chatMessageDeletedData.roomId() + "/deletions",
                payload
        );
    }
}

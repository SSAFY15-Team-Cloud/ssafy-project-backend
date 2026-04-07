package com.ssafy.ssafy_project.chat.adapter.out.messaging;

import com.ssafy.ssafy_project.chat.adapter.out.messaging.dto.response.ChatMessagePayload;
import com.ssafy.ssafy_project.chat.application.port.out.ChatMessagePublishedData;
import com.ssafy.ssafy_project.chat.application.port.out.PublishChatMessagePortOut;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StompChatPublisherAdapter implements PublishChatMessagePortOut {

    private final SimpMessagingTemplate simpMessagingTemplate;

    @Override
    public void publish(ChatMessagePublishedData chatMessagePublishedData) {
        ChatMessagePayload payload = new ChatMessagePayload(
                chatMessagePublishedData.messageId(),
                chatMessagePublishedData.senderId(),
                chatMessagePublishedData.senderNickname(),
                chatMessagePublishedData.message(),
                chatMessagePublishedData.createdTime()
        );

        simpMessagingTemplate.convertAndSend(
                "/sub/rooms/" + chatMessagePublishedData.roomId(),
                payload
        );
    }
}

package com.ssafy.ssafy_project.chat.adapter.in.websocket;

import com.ssafy.ssafy_project.chat.adapter.in.websocket.dto.request.CreateMessageSocketRequest;
import com.ssafy.ssafy_project.chat.application.port.in.CreateMessageCommand;
import com.ssafy.ssafy_project.chat.application.port.in.CreateMessagePortIn;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

import java.util.Map;

@Controller
@RequiredArgsConstructor
public class ChatStompController {
    private static final String USER_ID_SESSION_KEY = "userId";

    private final CreateMessagePortIn createMessagePortIn;

    @MessageMapping("/rooms/{roomId}/messages")
    public void createMessage(
            @DestinationVariable Long roomId,
            @Valid CreateMessageSocketRequest createMessageSocketRequest,
            SimpMessageHeaderAccessor headerAccessor
    ) {
        Map<String, Object> sessionAttributes = headerAccessor.getSessionAttributes();
        if (sessionAttributes == null || sessionAttributes.get(USER_ID_SESSION_KEY) == null) {
            throw new IllegalArgumentException("웹소켓 사용자 정보를 찾을 수 없습니다.");
        }

        Object userIdValue = sessionAttributes.get(USER_ID_SESSION_KEY);
        Long userId = userIdValue instanceof Long value
                ? value
                : Long.parseLong(String.valueOf(userIdValue));

        CreateMessageCommand command = new CreateMessageCommand(
                roomId,
                createMessageSocketRequest.message(),
                userId
        );

        createMessagePortIn.createMessage(command);
    }
}

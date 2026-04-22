package com.ssafy.ssafy_project.global.infrastructure.websocket;

import com.ssafy.ssafy_project.global.application.port.out.JwtPortOut;
import com.ssafy.ssafy_project.global.domain.entity.TokenType;
import com.ssafy.ssafy_project.roomparticipant.application.port.out.FindRoomParticipantPortOut;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketAuthChannelInterceptor implements ChannelInterceptor {

    private static final String ROOM_SUBSCRIBE_PREFIX = "/sub/rooms/";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String USER_ID_SESSION_KEY = "userId";

    private final JwtPortOut jwtPortOut;
    private final FindRoomParticipantPortOut findRoomParticipantPortOut;
    private final WebSocketSessionRegistry webSocketSessionRegistry;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        StompCommand command = accessor.getCommand();

        if (command == null) {
            return message;
        }

        if (StompCommand.CONNECT.equals(command)) {
            authenticate(accessor);
        }

        if (StompCommand.SUBSCRIBE.equals(command)) {
            authorizeSubscribe(accessor);
        }

        if (StompCommand.DISCONNECT.equals(command)) {
            unregister(accessor);
        }

        return message;
    }

    private void authenticate(StompHeaderAccessor accessor) {
        String authorizationHeader = getFirstNativeHeader(accessor, "Authorization");

        if (authorizationHeader == null || !authorizationHeader.startsWith(BEARER_PREFIX)) {
            throw new IllegalArgumentException("웹소켓 인증 정보가 없습니다.");
        }

        String token = authorizationHeader.substring(BEARER_PREFIX.length());

        if (!jwtPortOut.validate(token, TokenType.ACCESS_TOKEN)) {
            throw new IllegalArgumentException("유효하지 않은 웹소켓 토큰입니다.");
        }

        Long userId = jwtPortOut.extractUserId(token);
        accessor.setUser(new StompPrincipal(String.valueOf(userId)));
        Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
        if (sessionAttributes != null) {
            sessionAttributes.put(USER_ID_SESSION_KEY, userId);
        }

        String sessionId = accessor.getSessionId();
        if (sessionId != null) {
            webSocketSessionRegistry.bindUser(sessionId, userId);
        }
    }

    private void authorizeSubscribe(StompHeaderAccessor accessor) {
        String destination = accessor.getDestination();

        if (destination == null || !destination.startsWith(ROOM_SUBSCRIBE_PREFIX)) {
            return;
        }

        Long roomId = extractRoomId(destination);
        Long userId = resolveUserId(accessor);

        boolean isActiveParticipant =
                findRoomParticipantPortOut.existsByRoom_IdAndUser_IdAndIsActiveTrue(roomId, userId);

        if (!isActiveParticipant) {
            log.warn("WebSocket subscribe denied. roomId={}, userId={}", roomId, userId);
            throw new IllegalArgumentException("방 참가자만 구독할 수 있습니다.");
        }
    }

    private Long resolveUserId(StompHeaderAccessor accessor) {
        Principal principal = accessor.getUser();
        if (principal != null) {
            return Long.parseLong(principal.getName());
        }

        Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
        if (sessionAttributes != null) {
            Object userId = sessionAttributes.get(USER_ID_SESSION_KEY);
            if (userId instanceof Long value) {
                return value;
            }
            if (userId instanceof String value) {
                return Long.parseLong(value);
            }
        }

        log.warn("WebSocket principal not found on subscribe. sessionId={}", accessor.getSessionId());
        throw new IllegalArgumentException("웹소켓 사용자 정보를 찾을 수 없습니다.");
    }

    private String getFirstNativeHeader(StompHeaderAccessor accessor, String headerName) {
        List<String> values = accessor.getNativeHeader(headerName);
        if (values == null || values.isEmpty()) {
            return null;
        }
        return values.getFirst();
    }

    private void unregister(StompHeaderAccessor accessor) {
        String sessionId = accessor.getSessionId();
        if (sessionId != null) {
            webSocketSessionRegistry.unregisterSession(sessionId);
        }
    }

    private Long extractRoomId(String destination) {
        String roomIdText = destination.substring(ROOM_SUBSCRIBE_PREFIX.length());
        int nextSlashIndex = roomIdText.indexOf('/');
        if (nextSlashIndex >= 0) {
            roomIdText = roomIdText.substring(0, nextSlashIndex);
        }
        return Long.parseLong(roomIdText);
    }
}

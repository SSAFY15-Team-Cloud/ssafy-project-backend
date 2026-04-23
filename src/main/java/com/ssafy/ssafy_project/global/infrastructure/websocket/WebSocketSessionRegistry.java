package com.ssafy.ssafy_project.global.infrastructure.websocket;

import com.ssafy.ssafy_project.roomparticipant.application.port.out.CloseUserWebSocketSessionsPortOut;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class WebSocketSessionRegistry implements CloseUserWebSocketSessionsPortOut {

    private final ConcurrentHashMap<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<String, Long> sessionUsers = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<Long, Set<String>> userSessions = new ConcurrentHashMap<>();

    public void registerSession(WebSocketSession session){
        sessions.put(session.getId(), session);
        log.debug("WebSocket session registered. sessionId={}", session.getId());
    }

    public void bindUser(String sessionId, Long userId){
        sessionUsers.put(sessionId, userId);
        userSessions
                .computeIfAbsent(userId, key -> ConcurrentHashMap.newKeySet())
                .add(sessionId);

        log.debug("WebSocket session bound. sessionId={}, userId={}", sessionId, userId);
    }

    public void unregisterSession(String sessionId){
        sessions.remove(sessionId);

        Long userId = sessionUsers.remove(sessionId);
        if(userId != null) {
            Set<String> sessionIds = userSessions.get(userId);
            if (sessionIds != null) {
                sessionIds.remove(sessionId);

                if (sessionIds.isEmpty()) {
                    userSessions.remove(userId);
                }
            }
        }

        log.debug("WebSocket session unregistered. sessionId={}, userId={}", sessionId, userId);
    }

    @Override
    public void closeByUserId(Long userId) {
        Set<String> sessionIds = userSessions.get(userId);

        if (sessionIds == null || sessionIds.isEmpty()) {
            log.debug("No WebSocket sessions to close. userId={}", userId);
            return;
        }

        for (String sessionId : Set.copyOf(sessionIds)) {
            WebSocketSession session = sessions.get(sessionId);

            if (session == null || !session.isOpen()) {
                unregisterSession(sessionId);
                continue;
            }

            try {
                session.close(new CloseStatus(4000, "ROOM_LEFT"));
                log.info("WebSocket session closed by room leave. userId={}, sessionId={}", userId, sessionId);
            } catch (IOException e) {
                log.warn("Failed to close WebSocket session. userId={}, sessionId={}", userId, sessionId, e);
            } finally {
                unregisterSession(sessionId);
            }
        }
    }
}

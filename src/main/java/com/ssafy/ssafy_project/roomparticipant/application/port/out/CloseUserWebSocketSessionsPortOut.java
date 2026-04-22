package com.ssafy.ssafy_project.roomparticipant.application.port.out;

public interface CloseUserWebSocketSessionsPortOut {
    void closeByUserId(Long userId);
}

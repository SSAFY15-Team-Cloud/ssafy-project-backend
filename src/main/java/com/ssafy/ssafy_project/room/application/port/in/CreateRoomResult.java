package com.ssafy.ssafy_project.room.application.port.in;

import java.time.LocalDateTime;

public record CreateRoomResult(
        Long roomId,
        String title,
        Long hostId,
        String roomCode,
        LocalDateTime createdTime
) {
}

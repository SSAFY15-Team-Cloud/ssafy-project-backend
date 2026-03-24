package com.ssafy.ssafy_project.Room.application.port.in;

import java.time.LocalDateTime;

public record CreateRoomResult(
        Long roomId,
        String title,
        Long hostId,
        LocalDateTime createdAt
) {
}

package com.ssafy.ssafy_project.room.application.port.in;

import com.ssafy.ssafy_project.room.domain.RoomStatus;

import java.time.LocalDateTime;

public record GetRoomResult(
        Long roomId,
        String title,
        RoomStatus status,
        Long hostId,
        LocalDateTime createdTime

) {
}

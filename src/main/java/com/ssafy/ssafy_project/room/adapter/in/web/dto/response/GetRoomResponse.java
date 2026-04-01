package com.ssafy.ssafy_project.room.adapter.in.web.dto.response;

import com.ssafy.ssafy_project.room.domain.RoomStatus;

import java.time.LocalDateTime;

public record GetRoomResponse(
        Long roomId,
        String title,
        RoomStatus status,
        Long hostId,
        LocalDateTime createdAt
) {
}

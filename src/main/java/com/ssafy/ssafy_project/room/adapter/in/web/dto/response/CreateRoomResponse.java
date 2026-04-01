package com.ssafy.ssafy_project.room.adapter.in.web.dto.response;

import java.time.LocalDateTime;

public record CreateRoomResponse(
        Long roomId,
        String title,
        Long hostId,
        String roomCode,
        LocalDateTime createdTime
) {

}

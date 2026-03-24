package com.ssafy.ssafy_project.Room.adapter.in.web.dto.response;

import java.time.LocalDateTime;

public record UpdateRoomResponse(
        Long roomId,
        String title,
        Long hostId,
        LocalDateTime createdAt
) {

}

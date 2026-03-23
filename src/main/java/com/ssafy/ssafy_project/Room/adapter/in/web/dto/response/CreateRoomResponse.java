package com.ssafy.ssafy_project.Room.adapter.in.web.dto.response;

import java.time.LocalDateTime;

public record CreateRoomResponse(
        Long roomId,
        String title,
        Long hostId,
        LocalDateTime createAd
) {

}

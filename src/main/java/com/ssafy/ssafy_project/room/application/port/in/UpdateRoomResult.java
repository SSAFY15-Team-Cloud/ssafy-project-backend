package com.ssafy.ssafy_project.room.application.port.in;

public record UpdateRoomResult(
        Long roomId,
        String title
) {
}

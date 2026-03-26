package com.ssafy.ssafy_project.room.application.port.in;

public record UpdateRoomCommand(
        Long roomId,
        String title,
        Long userId
) {
}

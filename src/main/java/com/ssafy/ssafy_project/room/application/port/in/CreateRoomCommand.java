package com.ssafy.ssafy_project.room.application.port.in;

public record CreateRoomCommand(
        String title,
        Long hostId
) {
}

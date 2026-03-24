package com.ssafy.ssafy_project.Room.application.port.in;

public record CreateRoomCommand(
        String title,
        Long hostId
) {
}

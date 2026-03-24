package com.ssafy.ssafy_project.Room.application.port.in;

public record UpdateRoomCommand(
        Long roomId,
        String title,
        Long userId
) {
}

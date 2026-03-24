package com.ssafy.ssafy_project.Room.application.port.in;

public record DeleteRoomCommand(
        Long roomId,
        Long userId
) {
}

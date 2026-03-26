package com.ssafy.ssafy_project.room.application.port.in;

public record DeleteRoomCommand(
        Long roomId,
        Long userId
) {
}

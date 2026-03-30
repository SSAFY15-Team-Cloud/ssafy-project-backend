package com.ssafy.ssafy_project.roomparticipant.application.port.in;

public record LeaveRoomCommand(
        Long roomId,
        Long userId
) {
}

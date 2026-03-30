package com.ssafy.ssafy_project.roomparticipant.application.port.in;

public record JoinRoomCommand(
        String roomCode,
        Long userId
) {
}

package com.ssafy.ssafy_project.roomparticipant.application.port.in;

public record RoomParticipantCreateCommand(
        String roomCode,
        Long userId
) {
}

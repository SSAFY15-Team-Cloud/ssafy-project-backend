package com.ssafy.ssafy_project.roomparticipant.application.port.in;

public record SaveRoomParticipantCommand(
        String roomCode,
        Long userId
) {
}

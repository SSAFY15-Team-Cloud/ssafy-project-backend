package com.ssafy.ssafy_project.roomparticipant.application.port.in;

public record RoomParticipantCreateCommand(
        Long roomId,
        Long userId
) {
}

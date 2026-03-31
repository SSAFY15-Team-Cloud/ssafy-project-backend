package com.ssafy.ssafy_project.roomparticipant.application.port.in;

public record GetParticipantsCommand(
        Long roomId,
        Long userId
) {
}

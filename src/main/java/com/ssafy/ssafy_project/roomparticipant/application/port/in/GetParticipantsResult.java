package com.ssafy.ssafy_project.roomparticipant.application.port.in;

public record GetParticipantsResult(
        Long id,
        String email,
        String nickname,
        String name
) {
}

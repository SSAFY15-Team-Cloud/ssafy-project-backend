package com.ssafy.ssafy_project.roomparticipant.adapter.in.web.dto.response;

public record ParticipantResponse(
        Long userId,
        String email,
        String nickname,
        String name
) {
}

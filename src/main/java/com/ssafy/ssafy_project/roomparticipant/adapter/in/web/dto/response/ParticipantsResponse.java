package com.ssafy.ssafy_project.roomparticipant.adapter.in.web.dto.response;

import java.util.List;

public record ParticipantsResponse(
        List<ParticipantResponse> users
) {
}

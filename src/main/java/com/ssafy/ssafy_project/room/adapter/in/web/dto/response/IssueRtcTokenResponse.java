package com.ssafy.ssafy_project.room.adapter.in.web.dto.response;

public record IssueRtcTokenResponse(
        String serverUrl,
        String token,
        String roomName,
        String identity,
        String displayName
) {
}
